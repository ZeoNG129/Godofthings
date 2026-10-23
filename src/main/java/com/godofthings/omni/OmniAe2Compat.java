package com.godofthings.omni;

import java.util.ArrayList;
import java.util.List;

import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import org.jetbrains.annotations.Nullable;

/**
 * AE2 系适配器（原样移植自 Omni Upgrades 的 {@code AE2Compat}）。
 *
 * <p>AE2 Lightning Tech 的全部可升级机器（过载 IO 端口 / 样板供应器 / 接口 /
 * 闪电组装室等）都实现 AE2 的 {@code IUpgradeableObject} 或 {@code IPartHost}，
 * 因此这一个适配器同时覆盖 AE2 与 AE2LT。安装走 AE2 官方升级 API，
 * 严格按 {@code getMaxInstalled} 上限执行，绝不超装。</p>
 */
public class OmniAe2Compat implements UpgradeCompat {

    public static final boolean IS_LOADED = ModList.get().isLoaded("ae2");
    private static final Direction[] PART_SIDES = new Direction[] {
            null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    @Override
    public boolean isUpgradeItem(ItemStack stack) {
        return IS_LOADED && !Upgrades.getTooltipLinesForCard(stack.getItem()).isEmpty();
    }

    @Override
    public boolean passThroughOnNoTarget() {
        return true;
    }

    @Override
    public boolean isMachine(Level level, BlockPos pos) {
        if (!IS_LOADED) {
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IUpgradeableObject || be instanceof IPartHost;
    }

    @Override
    public boolean canMachineAcceptUpgrade(Level level, BlockPos pos, ItemStack upgrade) {
        return IS_LOADED && canAccept(level, pos, upgrade, null);
    }

    @Override
    public boolean canMachineAcceptUpgrade(Level level, BlockPos pos, ItemStack upgrade, @Nullable Vec3 clickPos) {
        return IS_LOADED && canAccept(level, pos, upgrade, clickPos);
    }

    @Override
    public boolean installUpgrade(Player player, InteractionHand hand, Level level, BlockPos pos, ItemStack upgrade) {
        return IS_LOADED && install(player, level, pos, null, upgrade);
    }

    @Override
    public boolean installUpgrade(Player player, InteractionHand hand, Level level, BlockPos pos,
            @Nullable Vec3 clickPos, ItemStack upgrade) {
        return IS_LOADED && install(player, level, pos, clickPos, upgrade);
    }

    @Override
    public int[] getColor(ItemStack stack) {
        return new int[] { 162, 102, 255 };
    }

    @Override
    public Component getDisplayName(ItemStack stack) {
        int[] rgb = getColor(stack);
        int color = rgb[0] << 16 | rgb[1] << 8 | rgb[2];
        return stack.getHoverName().copy().withColor(color);
    }

    private static boolean canAccept(Level level, BlockPos pos, ItemStack upgrade, @Nullable Vec3 clickPos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return false;
        }
        if (be instanceof IPartHost host && clickPos != null) {
            SelectedPart sp = host.selectPartWorld(clickPos);
            if (sp.part instanceof IUpgradeableObject part) {
                return accepts(part.getUpgrades(), upgrade);
            }
            return false;
        }
        if (be instanceof IUpgradeableObject upgradable) {
            return accepts(upgradable.getUpgrades(), upgrade);
        }
        if (be instanceof IPartHost host) {
            for (IUpgradeInventory inv : collectPartInventories(host)) {
                if (accepts(inv, upgrade)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean install(Player player, Level level, BlockPos pos, @Nullable Vec3 clickPos, ItemStack upgrade) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return false;
        }
        List<IUpgradeInventory> targets = new ArrayList<>(8);
        if (be instanceof IPartHost host && clickPos != null) {
            SelectedPart sp = host.selectPartWorld(clickPos);
            if (!(sp.part instanceof IUpgradeableObject part)) {
                return false;
            }
            targets.add(part.getUpgrades());
        } else if (be instanceof IUpgradeableObject upgradable) {
            targets.add(upgradable.getUpgrades());
        } else if (be instanceof IPartHost host) {
            targets.addAll(collectPartInventories(host));
        }
        for (IUpgradeInventory inv : targets) {
            if (feed(inv, player, upgrade)) {
                return true;
            }
        }
        return false;
    }

    private static List<IUpgradeInventory> collectPartInventories(IPartHost host) {
        List<IUpgradeInventory> result = new ArrayList<>(7);
        for (Direction side : PART_SIDES) {
            if (host.getPart(side) instanceof IUpgradeableObject upgradable) {
                result.add(upgradable.getUpgrades());
            }
        }
        return result;
    }

    private static boolean accepts(IUpgradeInventory inv, ItemStack upgrade) {
        Item card = upgrade.getItem();
        int maxInstalled = inv.getMaxInstalled(card);
        if (maxInstalled <= 0) {
            return false;
        }
        if (inv.getInstalledUpgrades(card) >= maxInstalled) {
            return false;
        }
        return inv.simulateAdd(upgrade.copyWithCount(1)).isEmpty();
    }

    private static boolean feed(IUpgradeInventory inv, Player player, ItemStack upgrade) {
        Item card = upgrade.getItem();
        int maxInstalled = inv.getMaxInstalled(card);
        if (maxInstalled <= 0) {
            return false;
        }
        int allowed = maxInstalled - inv.getInstalledUpgrades(card);
        if (allowed <= 0) {
            return false;
        }
        int toInsert = Math.min(upgrade.getCount(), allowed);
        ItemStack remainder = inv.simulateAdd(upgrade.copyWithCount(toInsert));
        int insertable = toInsert - remainder.getCount();
        if (insertable <= 0) {
            return false;
        }
        remainder = inv.addItems(upgrade.copyWithCount(insertable));
        int inserted = insertable - remainder.getCount();
        if (inserted <= 0) {
            return false;
        }
        if (!player.isCreative()) {
            upgrade.shrink(inserted);
        }
        return true;
    }
}
