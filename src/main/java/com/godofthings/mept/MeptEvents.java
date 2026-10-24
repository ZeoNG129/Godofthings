package com.godofthings.mept;

import com.godofthings.mept.network.ClearCableToolPointsPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * ME 放置工具的游戏总线事件（照抄上游 MEPlacementToolMod.CommonForgeEvents）：
 * 左键清空线缆选点、切手重置选点、玩家退出 / 服务器关闭时释放撤销历史。
 */
public final class MeptEvents
{
    private MeptEvents()
    {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        if (handleCableToolLeftClick(event.getEntity(), event.getLevel().isClientSide))
        {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event)
    {
        // 该事件只在客户端触发，需要发包通知服务端
        var player = event.getEntity();
        var stack = player.getMainHandItem();

        if (stack.getItem() != MeptItems.ME_CABLE_PLACEMENT_TOOL.get())
        {
            return;
        }

        // 检查是否设置了选点（客户端侧检查）
        var p1 = ItemMECablePlacementTool.getPoint1(stack);
        var p2 = ItemMECablePlacementTool.getPoint2(stack);
        var p3 = ItemMECablePlacementTool.getPoint3(stack);

        if (p1 != null || p2 != null || p3 != null)
        {
            // 先在客户端清掉选点获得即时视觉反馈
            ItemMECablePlacementTool.clearAllPoints(stack);

            // 发包让服务端也清掉
            int slot = player.getInventory().selected;
            PacketDistributor.sendToServer(
                    new ClearCableToolPointsPayload(slot)
            );
        }
    }

    /**
     * 处理线缆放置工具的左键——清空已选的点。
     * @return true 表示已清空且应取消事件
     */
    private static boolean handleCableToolLeftClick(Player player, boolean isClientSide)
    {
        var stack = player.getMainHandItem();

        // 只处理线缆放置工具
        if (stack.getItem() != MeptItems.ME_CABLE_PLACEMENT_TOOL.get())
        {
            return false;
        }

        // 检查是否设置了选点
        var p1 = ItemMECablePlacementTool.getPoint1(stack);
        var p2 = ItemMECablePlacementTool.getPoint2(stack);
        var p3 = ItemMECablePlacementTool.getPoint3(stack);

        if (p1 != null || p2 != null || p3 != null)
        {
            // 清空全部选点
            ItemMECablePlacementTool.clearAllPoints(stack);

            if (!isClientSide)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.mept.points_cleared"), true);
            }

            return true;
        }
        return false;
    }

    /**
     * 切换物品时——从线缆工具切走就重置它的选点。
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event)
    {
        if (!(event.getEntity() instanceof Player player))
        {
            return;
        }

        // 只关心主手槽
        if (event.getSlot() != EquipmentSlot.MAINHAND)
        {
            return;
        }

        var oldItem = event.getFrom();
        var newItem = event.getTo();

        // 从线缆工具切到别的东西时
        if (oldItem.getItem() == MeptItems.ME_CABLE_PLACEMENT_TOOL.get() &&
                newItem.getItem() != MeptItems.ME_CABLE_PLACEMENT_TOOL.get())
        {
            // 重置旧工具上的全部选点
            ItemMECablePlacementTool.clearAllPoints(oldItem);
        }
    }

    /**
     * 玩家退出：释放撤销历史，避免扣住断线玩家的 Level 引用。
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (!event.getEntity().level().isClientSide)
        {
            MeptRegistry.undoHistory().removePlayer(event.getEntity());
        }
    }

    /**
     * 服务器关闭：释放撤销历史持有的全部 Level 引用。
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        MeptRegistry.undoHistory().clear();
    }
}
