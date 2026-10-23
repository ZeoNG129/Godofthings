package com.godofthings.omni;

import com.mojang.logging.LogUtils;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * 批量安装核心（原样移植自 Omni Upgrades 的 {@code BatchInstallInteractionHandler}）：
 * 普通右键 = 单台安装；按住 Shift 右键 = 从点击处 BFS 洪泛，把插件批量装进
 * 相连的全部同类兼容机器（曼哈顿距离 ≤16、上限 1000 台），顺序填满或平均分配。
 */
@EventBusSubscriber(modid = GodofthingsOmni.MODID)
public class OmniBatchInstallHandler {

    private static final int MAX_RADIUS = 16;
    private static final int MAX_TARGETS = 1000;

    @SubscribeEvent
    public static void onRightClickBlock(RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        ItemStack heldItem = player.getItemInHand(event.getHand());
        UpgradeCompat compat = OmniCompatManager.getCompatFor(heldItem);
        if (compat == null) {
            return;
        }
        Level level = event.getLevel();
        BlockPos startPos = compat.resolveMainPos(level, event.getPos());
        boolean masterOff = level.isClientSide ? !OmniConfig.isEnabled : !OmniNetwork.isEnabled(player);
        if (masterOff) {
            return;
        }
        if (OmniBlacklist.isBlacklisted(level.isClientSide, player, heldItem)) {
            return;
        }
        if (!compat.isMachine(level, startPos)) {
            return;
        }

        boolean isClient = level.isClientSide;
        Component itemName = compat.getDisplayName(heldItem);
        boolean isBatch = player.isShiftKeyDown();
        List<BlockPos> targets = new ArrayList<>();
        Vec3 clickVec = isBatch ? null : event.getHitVec().getLocation();
        if (isBatch) {
            targets = findConnectedMachines(level, startPos, heldItem, compat);
        } else if (compat.canMachineAcceptUpgrade(level, startPos, heldItem, clickVec)) {
            targets.add(startPos);
        }

        if (targets.isEmpty()) {
            if (!compat.passThroughOnNoTarget()) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                if (!isClient) {
                    level.playSound(null, startPos, SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS, 1.0F, 1.5F);
                    player.displayClientMessage(
                            Component.translatable("message.godofthings.omni.batch_install.fail"), true);
                }
            }
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        InstallSession session = new InstallSession(player, event.getHand(), level, compat,
                heldItem.copy(), clickVec, isBatch);
        if (OmniConfig.isSequentialFillEnabled) {
            installSequential(session, targets);
        } else {
            installRoundRobin(session, targets);
        }

        int successMachines = session.successfulMachines.size();
        if (!isClient) {
            for (BlockPos pos : session.successfulMachines) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    be.setChanged();
                    level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 3);
                }
            }
            if (successMachines > 0) {
                level.playSound(null, startPos, SoundEvents.ARMOR_EQUIP_GOLD.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
                Component numMachines = Component.literal(String.valueOf(successMachines)).withStyle(ChatFormatting.AQUA);
                Component numConsumed = Component.literal(String.valueOf(session.totalConsumed)).withStyle(ChatFormatting.AQUA);
                player.displayClientMessage(Component.translatable("message.godofthings.omni.batch_install.success",
                        numMachines, itemName, numConsumed), true);
            } else {
                level.playSound(null, startPos, SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.BLOCKS, 1.0F, 1.5F);
                player.displayClientMessage(
                        Component.translatable("message.godofthings.omni.batch_install.fail"), true);
            }
        }
    }

    private static void installSequential(InstallSession session, List<BlockPos> targets) {
        for (BlockPos targetPos : targets) {
            while (!session.outOfItems && session.installAt(targetPos)) {
                session.successfulMachines.add(targetPos);
            }
            if (session.outOfItems) {
                break;
            }
        }
    }

    private static void installRoundRobin(InstallSession session, List<BlockPos> targets) {
        Set<BlockPos> fullMachines = new HashSet<>();
        int totalTargets = targets.size();
        while (fullMachines.size() < totalTargets && !session.outOfItems) {
            boolean anyInstalledInThisRound = false;
            Iterator<BlockPos> iterator = targets.iterator();
            while (iterator.hasNext()) {
                BlockPos targetPos = iterator.next();
                if (fullMachines.contains(targetPos)) {
                    continue;
                }
                if (!session.outOfItems) {
                    if (session.installAt(targetPos)) {
                        anyInstalledInThisRound = true;
                        session.successfulMachines.add(targetPos);
                    } else if (!session.outOfItems) {
                        fullMachines.add(targetPos);
                    }
                }
            }
            if (!anyInstalledInThisRound) {
                return;
            }
        }
    }

    private static int findMatchingSlot(Player player, ItemStack template, int startIndex) {
        for (int i = startIndex; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                return i;
            }
        }
        return -1;
    }

    private static List<BlockPos> findConnectedMachines(Level level, BlockPos startPos, ItemStack heldItem,
            UpgradeCompat compat) {
        List<BlockPos> result = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>(128);
        LongOpenHashSet visitedParts = new LongOpenHashSet();
        Set<BlockPos> validBases = new LinkedHashSet<>();
        queue.add(startPos);
        visitedParts.add(startPos.asLong());
        List<BlockPos> offsets = NeighborOffsets.get(OmniConfig.isDiagonalDetectionEnabled);
        while (!queue.isEmpty() && validBases.size() < MAX_TARGETS) {
            BlockPos currentPart = queue.poll();
            BlockPos currentBase = compat.resolveMainPos(level, currentPart);
            if (compat.isMachine(level, currentBase)) {
                if (compat.canMachineAcceptUpgrade(level, currentBase, heldItem)) {
                    validBases.add(currentBase);
                }
                for (BlockPos offset : offsets) {
                    BlockPos neighbor = currentPart.offset(offset.getX(), offset.getY(), offset.getZ());
                    if (startPos.distManhattan(neighbor) <= MAX_RADIUS && visitedParts.add(neighbor.asLong())) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        result.addAll(validBases);
        return result;
    }

    private static class InstallSession {
        final Player player;
        final InteractionHand hand;
        final Level level;
        final UpgradeCompat compat;
        final ItemStack template;
        final Vec3 clickVec;
        final boolean isBatch;
        int searchStartIndex = 0;
        int totalConsumed = 0;
        boolean outOfItems = false;
        final Set<BlockPos> successfulMachines = new LinkedHashSet<>();

        InstallSession(Player player, InteractionHand hand, Level level, UpgradeCompat compat,
                ItemStack template, Vec3 clickVec, boolean isBatch) {
            this.player = player;
            this.hand = hand;
            this.level = level;
            this.compat = compat;
            this.template = template;
            this.clickVec = clickVec;
            this.isBatch = isBatch;
        }

        boolean installAt(BlockPos targetPos) {
            ItemStack currentHandItem = player.getItemInHand(hand);
            if (currentHandItem.isEmpty()) {
                if (!isBatch) {
                    outOfItems = true;
                    return false;
                }
                currentHandItem = restock();
                if (currentHandItem == null) {
                    return false;
                }
            }
            int originalCount = currentHandItem.getCount();
            ItemStack operand = currentHandItem.copy();
            operand.setCount(1);
            boolean success = compat.installUpgrade(player, hand, level, targetPos, clickVec, operand);
            if (success) {
                int consumed = 1 - operand.getCount() + (originalCount - player.getItemInHand(hand).getCount());
                currentHandItem.shrink(1 - operand.getCount());
                if (currentHandItem.isEmpty()) {
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
                if (!player.isCreative()) {
                    totalConsumed += consumed;
                }
            }
            return success;
        }

        private ItemStack restock() {
            if (player.isCreative()) {
                ItemStack creativeStack = template.copyWithCount(template.getMaxStackSize());
                player.setItemInHand(hand, creativeStack);
                return creativeStack;
            }
            int slot = findMatchingSlot(player, template, searchStartIndex);
            if (slot == -1) {
                outOfItems = true;
                return null;
            }
            ItemStack stack = player.getInventory().getItem(slot);
            player.setItemInHand(hand, stack);
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            searchStartIndex = slot + 1;
            return stack;
        }
    }
}
