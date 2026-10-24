package com.godofthings.aalink;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import appeng.api.parts.IPartItem;
import appeng.api.upgrades.IUpgradeableItem;
import appeng.parts.PartPlacement;

/**
 * 无线连接器卡的自动连接（照抄上游 FrequencyCardAutoConnectHandler 的完整触发链）：
 * <ol>
 *   <li><b>放置完整方块</b>（BlockEvent.EntityPlace）：AE 线缆、机器等；</li>
 *   <li><b>手持 AE2 部件右键</b>（RightClickBlock + PartPlacement 求落点）：输出/输入总线、
 *       存储总线等放进电缆总线不触发方块放置事件，必须单独拦截——v7.11.2 只移植了路径 1，
 *       放部件永远不自动连接，这是「自动连接失效」的主因之一。</li>
 * </ol>
 * 两条链都进同一个「延迟重试」队列：设备节点在放置当 tick 往往还没就绪，
 * 上游靠 WirelessLinkRegistry 的队列延迟 2 tick；本地实现为最多 10 次、每次 2 tick
 * 的重试（共 ~1 秒），节点就绪或重试耗尽才结束，静默失败不刷屏。
 */
public final class ConnectorCardAutoConnectHandler
{
    /** 每次重试的间隔（照抄上游 queueAutoConnect 的 2 tick） */
    private static final int RETRY_INTERVAL_TICKS = 2;
    /** 最多重试次数（节点 / 中枢可能因区块加载稍晚就绪） */
    private static final int MAX_RETRIES = 10;

    /** 延迟重试队列：玩家放置设备后等节点就绪再接线 */
    private static final Deque<PendingLink> PENDING = new ArrayDeque<>();

    private record PendingLink(net.minecraft.world.entity.player.Player player, ItemStack card, ServerLevel level, BlockPos pos, int attempts)
    {
    }

    private ConnectorCardAutoConnectHandler()
    {
    }

    // ---------------------------------------------------------------- 路径 1：放置完整方块

    @SubscribeEvent
    public static void onBlockPlaced(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event)
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
        BlockPos pos = event.getPos().immutable();
        var card = ConnectorCardItem.findAutoConnectCard(player);
        if (card.isEmpty())
        {
            return;
        }
        // 只对可能是 AE 设备的位置排队（快速预检，普通方块直接忽略）
        if (level.getBlockEntity(pos) == null && AaLinkBridge.resolveDeviceNode(level, pos) == null)
        {
            return;
        }
        enqueue(player, card.get(), level, pos);
    }

    // ---------------------------------------------------------------- 路径 2：手持部件右键电缆

    /**
     * 照抄上游 onRightClickBlock：手持 AE2 部件（IPartItem）右键时用 PartPlacement
     * 计算部件实际落点，把「落点位置」排进自动连接队列。
     * 用 HIGHEST 优先级抢在部件交互被消费前读取上下文（不取消事件，放置本身照常发生）。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getItemStack().getItem() instanceof IPartItem<?>))
        {
            return;
        }
        var clickedFace = event.getFace();
        if (clickedFace == null)
        {
            return;
        }
        var card = ConnectorCardItem.findAutoConnectCard(player);
        if (card.isEmpty())
        {
            return;
        }
        // PartPlacement 求落点：会返回部件将放置到的坐标与面（客户端服务端都可用；
        // 这里在服务端事件里算，与上游一致）
        PartPlacement.Placement placement;
        try
        {
            placement = PartPlacement.getPartPlacement(
                    player,
                    player.level(),
                    event.getItemStack(),
                    event.getPos(),
                    clickedFace,
                    event.getHitVec().getLocation());
        }
        catch (Exception ignored)
        {
            return;
        }
        if (placement == null)
        {
            return;
        }
        if (!(player.level() instanceof ServerLevel level))
        {
            return;
        }
        enqueue(player, card.get(), level, placement.pos().immutable());
    }

    // ---------------------------------------------------------------- 延迟重试队列

    private static void enqueue(net.minecraft.world.entity.player.Player player, ItemStack card, ServerLevel level, BlockPos pos)
    {
        // 复制卡数据：队列跨 tick 存活，玩家可能已把卡移走
        PENDING.add(new PendingLink(player, card.copy(), level, pos, 0));
    }

    /** 临时探针入口：手动喂一条进自动连接队列（GameTest 的 setBlock 不触发 EntityPlace）。探针删除后可移除。 */
    public static void debugEnqueue(net.minecraft.world.entity.player.Player player, ItemStack card, ServerLevel level, BlockPos pos)
    {
        enqueue(player, card, level, pos);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        if (PENDING.isEmpty())
        {
            return;
        }
        int budget = PENDING.size();
        for (int i = 0; i < budget; i++)
        {
            var pending = PENDING.poll();
            if (pending.attempts() >= MAX_RETRIES)
            {
                continue; // 重试耗尽：静默放弃
            }
            if (!pending.level().isLoaded(pending.pos()) || pending.player().isRemoved())
            {
                continue; // 区块卸载 / 玩家离线
            }
            // 就绪即接；未就绪则排下一轮（+RETRY_INTERVAL_TICKS 由下方节拍控制）
            if (tryAutoConnect(pending))
            {
                continue;
            }
            PENDING.add(new PendingLink(pending.player(), pending.card(), pending.level(),
                    pending.pos(), pending.attempts() + 1));
        }
    }

    /**
     * 尝试把设备接入卡上绑定的连接器网络。
     * @return true 表示已处理（成功接入 / 明确不可接 / 无卡无绑定——不再重试）
     */
    private static boolean tryAutoConnect(PendingLink pending)
    {
        var player = pending.player();
        var card = pending.card();
        var level = pending.level();
        var pos = pending.pos();

        var data = ConnectorCardItem.getData(card);
        if (!data.isBound() || !data.autoConnect() || !data.canBeUsedBy(player.getUUID()))
        {
            return true; // 卡状态已变：不再重试
        }

        ResourceLocation dimId = ResourceLocation.tryParse(data.boundConnectorDimension().orElse(""));
        BlockPos hubPos = BlockPos.of(data.boundConnectorPos().orElse(0L));
        ServerLevel hubLevel = dimId == null
                ? null
                : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (hubLevel == null
                || !(hubLevel.getBlockEntity(hubPos) instanceof AaLinkConnectorBlockEntity hub))
        {
            return true; // 中枢不存在：绑定失效，重试无意义
        }
        var hubNode = hub.getHubNode();
        if (hubNode == null || hubNode.getGrid() == null)
        {
            return false; // 中枢节点未就绪（刚放置/区块刚加载）：值得重试
        }

        var deviceNode = AaLinkBridge.resolveDeviceNode(level, pos);
        if (deviceNode == null)
        {
            return pending.attempts() + 1 >= 3; // 前几 tick 等节点生成；3 次后放弃（不是 AE 设备）
        }
        if (deviceNode.getGrid() == hubNode.getGrid())
        {
            return true; // 已同网（含物理相邻直连）：完成
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
        return true;
    }
}
