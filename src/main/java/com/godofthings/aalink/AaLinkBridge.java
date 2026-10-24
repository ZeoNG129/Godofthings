package com.godofthings.aalink;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;

/**
 * 全局桥接器（移植自 AALink 的 AutoLinkBridge）。
 *
 * <p>监听方块放置事件，把任意位置放置的 AE 设备接入「无线桥接中枢」所在网络。
 * 机制：每当一个承载 AE 网格节点的方块被放置时，取出该设备的网格节点，与
 * {@link AaLinkConnectorBlockEntity} 登记的中枢节点做一次
 * {@link GridHelper#createConnection}。AE2 的 GridConnection 是逻辑连接，
 * 不依赖节点间的物理位置，因此无论设备放在多远、甚至其它维度，都能直接并入
 * 中枢所在的 ME 网格——满足「无视距离无视维度」。</p>
 *
 * <p>时序：方块被放置时其 BlockEntity 的网格节点可能尚未创建（AE2 节点多在
 * 首个 tick 才建），因此这里先只记录放置位置，下一 tick 由
 * {@link #bridgePending()} 重试解析节点再建连。</p>
 */
public final class AaLinkBridge
{
    private static final int MAX_ATTEMPTS = 60;

    private static final Set<IGridNode> HUBS = Collections.synchronizedSet(new HashSet<>());
    private static final Set<PlacementRef> PENDING = Collections.synchronizedSet(new HashSet<>());
    /** 已知承载 AE 网格节点的方块实体类型缓存：同一类型方块只需查询一次 getNodeHost。 */
    private static final Set<BlockEntityType<?>> KNOWN_NODE_HOST_TYPES =
            Collections.synchronizedSet(new HashSet<>());
    /** 自动连接总开关：关闭时放置设备不会自动连接，但已建立的连接保持不断开。 */
    private static volatile boolean enabled = false;
    /** 是否已从存档恢复过开关状态（每个服务端会话一次）。 */
    private static boolean loaded = false;

    private AaLinkBridge()
    {
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    public static void setEnabled(boolean value)
    {
        enabled = value;
        saveToData();
    }

    public static void toggleEnabled()
    {
        enabled = !enabled;
        saveToData();
    }

    /** 修改开关后写入世界存档，保证重启/重进后状态保持。 */
    private static void saveToData()
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
        {
            return;
        }
        for (ServerLevel level : server.getAllLevels())
        {
            AaLinkSavedData.get(level).setEnabled(enabled);
        }
    }

    /** 世界加载后从存档恢复开关状态（server ticking 前调用）。 */
    static void loadFromData(ServerLevel level)
    {
        enabled = AaLinkSavedData.get(level).isEnabled();
    }

    static void registerHub(IGridNode node)
    {
        if (node != null)
        {
            HUBS.add(node);
        }
    }

    static void unregisterHub(IGridNode node)
    {
        if (node != null)
        {
            HUBS.remove(node);
        }
    }

    /**
     * 中枢节点（重新）创建后，把本维度所有「曾经成功桥接」的设备重新加入待桥接队列。
     * 解决：中枢区块卸载→重载后，其它仍处于加载状态区块的设备不会触发放置/区块加载事件，
     * 导致它们与中枢的逻辑连接丢失且不自动恢复。
     */
    static void requeueLinked(ServerLevel level)
    {
        for (BlockPos pos : AaLinkSavedData.get(level).getLinkedPositions())
        {
            PENDING.add(new PlacementRef(level, pos));
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event)
    {
        if (!enabled)
        {
            return; // 自动连接开关关闭：放置设备不自动连接
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel))
        {
            return; // 只在服务端处理
        }
        // 快速过滤：没有中枢时直接忽略，省去后续一切查询
        if (HUBS.isEmpty())
        {
            return;
        }
        BlockPos pos = event.getBlockSnapshot().getPos();
        BlockEntity be = serverLevel.getBlockEntity(pos);
        if (be == null || be instanceof AaLinkConnectorBlockEntity)
        {
            return; // 无实体或中枢本身
        }
        // 缓存已知承载 AE 节点的方块实体类型：
        //  - 首次遇到某类型 → 查一次 getNodeHost 判断，承载节点则缓存该类型
        //  - 之后同类型方块放置 → 直接命中缓存，跳过 getNodeHost（对普通方块零额外开销）
        if (KNOWN_NODE_HOST_TYPES.contains(be.getType()))
        {
            PENDING.add(new PlacementRef(serverLevel, pos));
            return;
        }
        if (GridHelper.getNodeHost(serverLevel, pos) != null)
        {
            KNOWN_NODE_HOST_TYPES.add(be.getType());
            PENDING.add(new PlacementRef(serverLevel, pos));
        }
    }

    /** 服务端 tick 时尝试解析尚未建连的设备节点并桥接。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        if (!loaded)
        {
            loaded = true;
            MinecraftServer server = event.getServer();
            for (ServerLevel level : server.getAllLevels())
            {
                loadFromData(level);
            }
        }
        if (HUBS.isEmpty() || PENDING.isEmpty())
        {
            return;
        }
        bridgePending();
    }

    /**
     * 服务器停止（含单机退出世界）时清空跨存档的静态状态。
     * 单机「退出世界→回标题→进另一个存档」时，若不清空，HUBS/PENDING 会残留
     * 上一个存档已销毁的 IGridNode/ServerLevel 引用，导致节点指向无效网格。
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event)
    {
        HUBS.clear();
        PENDING.clear();
        KNOWN_NODE_HOST_TYPES.clear();
        enabled = false;
        loaded = false;
    }

    /**
     * 区块加载时扫描其中的 AE 设备并重新桥接。
     * 服务器重启后，已放置的设备不会触发放置事件，但随区块重新加载，
     * 通过这里把设备节点重新接入中枢，保证网络不丢。
     * 不依赖中枢是否已就绪：设备位置先记录，中枢就绪后 bridgePending 会自动建连。
     *
     * <p>只恢复「曾经成功桥接过」的设备（{@link AaLinkSavedData#isLinked}）。
     * 开关关闭期间放置的设备从未被桥接，重进存档后保持断连，不会被这里强制接入。</p>
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event)
    {
        if (!(event.getLevel() instanceof ServerLevel serverLevel))
        {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk levelChunk))
        {
            return;
        }
        AaLinkSavedData data = AaLinkSavedData.get(serverLevel);
        for (var entry : levelChunk.getBlockEntities().entrySet())
        {
            BlockEntity be = entry.getValue();
            if (be == null || be instanceof AaLinkConnectorBlockEntity)
            {
                continue;
            }
            if (!data.isLinked(entry.getKey()))
            {
                continue; // 从未桥接过的设备（如开关关闭时放置的）不重连
            }
            PENDING.add(new PlacementRef(serverLevel, entry.getKey()));
        }
    }

    private static void bridgePending()
    {
        synchronized (PENDING)
        {
            if (HUBS.isEmpty())
            {
                return; // 无中枢时保留所有待处理项，等中枢就绪后再建连
            }
            var it = PENDING.iterator();
            while (it.hasNext())
            {
                PlacementRef ref = it.next();
                if (ref.attempts >= MAX_ATTEMPTS)
                {
                    it.remove(); // 重试次数用尽（设备可能已被拆除），丢弃
                    continue;
                }
                IGridNode deviceNode = resolveDeviceNode(ref.level, ref.pos);
                if (deviceNode == null)
                {
                    ref.attempts++;
                    continue; // 节点还没建好，下一 tick 再试
                }
                IGridNode hubNode = pickHubNode(ref.level);
                if (hubNode == null)
                {
                    // 有设备节点但没找到中枢：保留等待（不计数，避免误删）
                    continue;
                }
                it.remove();
                try
                {
                    // 已在同一网格 / 已建连则跳过，否则逻辑建连并入中枢网络。
                    if (deviceNode == hubNode || deviceNode.getGrid() == hubNode.getGrid())
                    {
                        continue;
                    }
                    GridHelper.createConnection(deviceNode, hubNode);
                    // 记录已桥接设备：重进存档/区块重载时只恢复这些设备
                    AaLinkSavedData.get(ref.level).link(ref.pos);
                }
                catch (Exception ignored)
                {
                    // 重复建连等场景安全忽略
                }
            }
        }
    }

    /**
     * 解析方块实体的 AE 网格节点（部件优先，回退宿主节点）；供桥接与无线连接工具复用。
     */
    public static IGridNode resolveDeviceNode(ServerLevel level, BlockPos pos)
    {
        // 优先处理 AE2 部件（Part）：输出总线/输入总线/存储总线等挂在电缆总线里，
        // 它们不是 IInWorldGridNodeHost 方块，必须通过 IPartHost.getPart(side) 取部件自己的节点。
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IPartHost partHost)
        {
            for (Direction side : Direction.values())
            {
                IPart part = partHost.getPart(side);
                if (part != null)
                {
                    IGridNode node = part.getGridNode();
                    if (node != null)
                    {
                        return node;
                    }
                }
            }
            // 电缆总线里没有任何部件但自身也有节点（如纯线缆），回退到宿主节点
        }
        // 完整方块（ME 线缆/机器/存储方块）：走 IInWorldGridNodeHost
        IInWorldGridNodeHost host = GridHelper.getNodeHost(level, pos);
        if (host == null)
        {
            return null;
        }
        for (Direction side : Direction.values())
        {
            IGridNode node = host.getGridNode(side);
            if (node != null)
            {
                return node;
            }
        }
        return null;
    }

    /**
     * 断开设备节点上所有由本模组建立的逻辑连接（无线连接器卡「再点一次断开」用）。
     * 只断「连接到中枢节点」的连接；设备与相邻线缆的物理连接不受影响。
     */
    public static void disconnect(IGridNode deviceNode)
    {
        var connections = new java.util.ArrayList<>(deviceNode.getConnections());
        for (var connection : connections)
        {
            var other = connection.a() == deviceNode ? connection.b() : connection.a();
            if (HUBS.contains(other))
            {
                connection.destroy();
            }
        }
    }

    private static IGridNode pickHubNode(ServerLevel level)
    {
        synchronized (HUBS)
        {
            // 优先选择与设备同维度的中枢，否则退回任意一个可用中枢。
            for (IGridNode node : HUBS)
            {
                if (node.getGrid() != null && node.getLevel() == level)
                {
                    return node;
                }
            }
            for (IGridNode node : HUBS)
            {
                if (node.getGrid() != null)
                {
                    return node;
                }
            }
        }
        return null;
    }

    private static final class PlacementRef
    {
        private final ServerLevel level;
        private final BlockPos pos;
        private int attempts = 0;

        private PlacementRef(ServerLevel level, BlockPos pos)
        {
            this.level = level;
            this.pos = pos;
        }
    }
}
