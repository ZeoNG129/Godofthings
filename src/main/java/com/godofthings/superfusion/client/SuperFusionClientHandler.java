package com.godofthings.superfusion.client;

import com.godofthings.client.WandKeyBindings;
import com.godofthings.superfusion.SuperFusionItem;
import com.godofthings.superfusion.network.BatchSelectionPayload;
import com.godofthings.superfusion.network.FusionActionPayload;
import com.godofthings.superfusion.network.MiddleClickPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * 超级融合卡的客户端输入处理（移植自 Mekanism-Card 的 {@code ModKeyHandler}）。
 * <p>
 * 处理四类原版没有入口的操作：
 * <ul>
 *   <li>潜行 + 左键空气：轮换融合模式；</li>
 *   <li>潜行 + 右键空气：清空卡内数据；</li>
 *   <li>左键空气（不潜行）：打开设置界面；</li>
 *   <li>中键机器：装满升级；</li>
 *   <li>按住「批量选择」键 + 左键点两个角点：框选批量执行。</li>
 * </ul>
 * <p>
 * 注意：本类不挂 {@code @EventBusSubscriber}（客户端专用类只由 {@code ClientModEvents}
 * 在 Mekanism 存在时手动注册，避免未装 Mekanism 时类加载失败）。
 */
public final class SuperFusionClientHandler
{
    /** 框选第一个角点（仅客户端保存，用于渲染与第二次点击） */
    @Nullable
    private static BlockPos firstSelectionCorner;
    /** 左键是否已经按下（避免长按重复触发） */
    private static boolean attackKeyWasDown;

    private SuperFusionClientHandler()
    {
    }

    @Nullable
    public static BlockPos getFirstSelectionCorner()
    {
        return firstSelectionCorner;
    }

    public static void clearSelectionCorner()
    {
        firstSelectionCorner = null;
    }

    /** 手上是不是融合卡 */
    public static boolean holdingCard()
    {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        return player != null && player.getMainHandItem().getItem() instanceof SuperFusionItem;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTick(ClientTickEvent.Pre event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null)
        {
            firstSelectionCorner = null;
            attackKeyWasDown = false;
            return;
        }
        handleAttackInput(minecraft, player);
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event)
    {
        if (event.isAttack())
        {
            if (shouldCaptureAttack())
            {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
            return;
        }
        if (!event.isUseItem())
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null)
        {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean targetingAir = minecraft.hitResult == null
                || minecraft.hitResult.getType() == HitResult.Type.MISS;
        if (stack.getItem() instanceof SuperFusionItem && player.isShiftKeyDown() && targetingAir)
        {
            PacketDistributor.sendToServer(FusionActionPayload.clearMemory());
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event)
    {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE || event.getAction() != GLFW.GLFW_PRESS)
        {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.screen != null)
        {
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof SuperFusionItem))
        {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit)
                || minecraft.hitResult.getType() != HitResult.Type.BLOCK)
        {
            return;
        }
        PacketDistributor.sendToServer(new MiddleClickPayload(hit.getBlockPos(), hit.getDirection()));
        event.setCanceled(true);
    }

    /** 按住批量选择键左键点方块时，别让原版把方块挖了 */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        if (!event.getLevel().isClientSide() || event.getEntity().isShiftKeyDown())
        {
            return;
        }
        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (WandKeyBindings.SUPER_FUSION_BATCH_KEY.get().isDown()
                && SuperFusionItem.isBatchSelectionTool(stack))
        {
            event.setCanceled(true);
        }
    }

    private static boolean shouldCaptureAttack()
    {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (minecraft.screen != null || player == null
                || !SuperFusionItem.isBatchSelectionTool(player.getMainHandItem()))
        {
            return false;
        }
        boolean targetingBlock = minecraft.hitResult != null
                && minecraft.hitResult.getType() == HitResult.Type.BLOCK;
        boolean targetingAir = minecraft.hitResult == null
                || minecraft.hitResult.getType() == HitResult.Type.MISS;
        if (player.isShiftKeyDown())
        {
            // 潜行 + 左键空气 = 轮换模式，要吃掉这次攻击
            return targetingAir;
        }
        return targetingBlock
                ? WandKeyBindings.SUPER_FUSION_BATCH_KEY.get().isDown()
                : targetingAir;
    }

    private static void handleAttackInput(Minecraft minecraft, Player player)
    {
        boolean attackDown = minecraft.options.keyAttack.isDown();
        if (!attackDown)
        {
            attackKeyWasDown = false;
            return;
        }
        if (attackKeyWasDown)
        {
            return;
        }
        attackKeyWasDown = true;
        if (minecraft.screen != null || minecraft.level == null)
        {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!SuperFusionItem.isBatchSelectionTool(stack))
        {
            firstSelectionCorner = null;
            return;
        }

        HitResult hitResult = minecraft.hitResult;
        boolean batchKeyDown = WandKeyBindings.SUPER_FUSION_BATCH_KEY.get().isDown();

        if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == HitResult.Type.BLOCK)
        {
            if (player.isShiftKeyDown() || !batchKeyDown)
            {
                return;
            }
            if (!minecraft.level.getWorldBorder().isWithinBounds(blockHit.getBlockPos()))
            {
                return;
            }
            handleSelectionClick(player, stack, blockHit.getBlockPos().immutable());
            return;
        }
        if (hitResult != null && hitResult.getType() != HitResult.Type.MISS)
        {
            return;
        }

        // 点空气
        if (batchKeyDown)
        {
            if (firstSelectionCorner != null)
            {
                firstSelectionCorner = null;
                player.displayClientMessage(
                        Component.translatable("message.godofthings.super_fusion.selection_cleared"), false);
            }
            return;
        }
        if (player.isShiftKeyDown())
        {
            // 潜行 + 左键空气 = 轮换模式
            PacketDistributor.sendToServer(FusionActionPayload.cycleMode());
            return;
        }
        // 普通左键空气 = 打开设置界面
        firstSelectionCorner = null;
        minecraft.setScreen(new SuperFusionScreen());
    }

    private static void handleSelectionClick(Player player, ItemStack stack, BlockPos clickedPos)
    {
        if (firstSelectionCorner == null)
        {
            firstSelectionCorner = clickedPos;
            player.displayClientMessage(Component.translatable(
                    "message.godofthings.super_fusion.selection_first", clickedPos.toShortString()), false);
        }
        else
        {
            BlockPos firstPos = firstSelectionCorner;
            firstSelectionCorner = null;
            PacketDistributor.sendToServer(new BatchSelectionPayload(firstPos, clickedPos));
        }
    }
}
