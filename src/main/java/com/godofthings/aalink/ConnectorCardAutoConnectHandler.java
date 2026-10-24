package com.godofthings.aalink;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 无线连接器卡的自动连接（照抄上游 FrequencyCardAutoConnectHandler 的 onBlockPlaced 路径）：
 * 玩家放置方块后，若身上有一张「已绑定 + 自动连接开」的卡，
 * 就把新方块接入卡上绑定的无线连接器网络。
 * <p>
 * [本地改动] 上游还有部件放置（AE2 part 右键放置）路径，依赖 WirelessLinkRegistry 的
 * 延迟队列；本地复用 {@link AaLinkBridge} 已有的 tick 桥接队列：直接在这里同步接线，
 * 失败（目标不是 AE 设备 / 中枢未就绪）就静默跳过——与自动桥接的容错一致。
 */
public final class ConnectorCardAutoConnectHandler
{
    /** 放置后延迟的 tick 数（照抄上游 queueAutoConnect 的 2 tick） */
    private static final int DELAY_TICKS = 2;

    private ConnectorCardAutoConnectHandler()
    {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event)
    {
        if (event.isCanceled())
        {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }

        // 服务端延迟 2 tick 再接线（方块实体 / 网格节点刚放置时可能还没就绪）
        BlockPos placedPos = event.getPos().immutable();
        var card = ConnectorCardItem.findAutoConnectCard(player);
        if (card.isEmpty())
        {
            return;
        }

        // 快速预检：这个位置是 AE 设备才值得延迟接线
        if (!isPotentialDevice(level, placedPos))
        {
            return;
        }

        var server = level.getServer();
        ItemStack cardCopy = card.get().copy();
        server.execute(() ->
        {
            // execute 在当前 tick 尾部执行；再排 DELAY_TICKS 次 getServer().execute 模拟延迟
            schedule(level, () -> autoConnect(player, cardCopy, level, placedPos), DELAY_TICKS);
        });
    }

    private static void schedule(ServerLevel level, Runnable task, int ticks)
    {
        if (ticks <= 0)
        {
            task.run();
            return;
        }
        level.getServer().execute(() -> schedule(level, task, ticks - 1));
    }

    private static boolean isPotentialDevice(ServerLevel level, BlockPos pos)
    {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null || AaLinkBridge.resolveDeviceNode(level, pos) != null;
    }

    /**
     * 把新放置的设备接入卡上绑定的连接器网络（静默失败——自动连接不该刷屏报错）。
     */
    private static void autoConnect(ServerPlayer player, ItemStack card, ServerLevel level, BlockPos pos)
    {
        if (!level.isLoaded(pos) || player.isRemoved())
        {
            return;
        }
        var data = ConnectorCardItem.getData(card);
        if (!data.isBound() || !data.autoConnect() || !data.canBeUsedBy(player.getUUID()))
        {
            return;
        }

        ResourceLocation dimId = ResourceLocation.tryParse(data.boundConnectorDimension().orElse(""));
        BlockPos hubPos = BlockPos.of(data.boundConnectorPos().orElse(0L));
        ServerLevel hubLevel = dimId == null
                ? null
                : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (hubLevel == null
                || !(hubLevel.getBlockEntity(hubPos) instanceof AaLinkConnectorBlockEntity hub))
        {
            return;
        }
        var hubNode = hub.getHubNode();
        if (hubNode == null || hubNode.getGrid() == null)
        {
            return;
        }

        var deviceNode = AaLinkBridge.resolveDeviceNode(level, pos);
        if (deviceNode == null || deviceNode.getGrid() == hubNode.getGrid())
        {
            return;
        }
        try
        {
            appeng.api.networking.GridHelper.createConnection(deviceNode, hubNode);
            AaLinkSavedData.get(level).link(pos);
        }
        catch (Exception ignored)
        {
            // 自动连接静默失败
        }
    }
}
