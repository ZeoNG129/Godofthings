package com.godofthings.item;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 神之工具的「时间加速」（移植自万象担架的荒辰移晷之杖 WondrousStaffAcceleration）。
 *
 * <h3>机制</h3>
 * 对一个方块连续驱动 N 次它的「一 tick 该做的事」：
 * <ul>
 *   <li><b>避雷针</b>：直接生成真实落雷（不依赖天气，等价 {@code /summon lightning_bolt}），有数量上限</li>
 *   <li><b>AE 机器</b>：走 AE 自己的网格刻 —— {@code IGridNode.getService(IGridTickable.class)}
 *       然后 {@code tickingRequest(node, 1)}；返回 {@code SLEEP} 的端点当次就摘掉，避免空转</li>
 *   <li><b>有 BlockEntity 的方块</b>：取 {@code BlockState.getTicker(...)} 反复调用</li>
 *   <li><b>随机刻方块</b>（作物/树苗等）：反复 {@code randomTick}。<b>一次虚拟刻 = 一次 randomTick</b>，
 *       不要再乘原版 1/1365 的区块抽样概率（上游注释特别强调过：重复抽样会让作物慢 1365 倍）</li>
 * </ul>
 *
 * <h3>与上游的差异（刻意的）</h3>
 * 上游为每个被加速目标生成一个自定义实体 {@code WondrousStaffAccelerationEntity} 来托管计时；
 * 这里改为<b>纯服务端的坐标标记表</b>（{@code Map<维度, Map<坐标, 标记>>}），
 * 不需要自定义实体、不需要渲染器、也不需要额外的网络包，语义等价而实现更轻。
 *
 * <h3>时间预算</h3>
 * 每个服务器 tick 的加速总耗时上限 {@link #BUDGET_NANOS}（默认 3ms），
 * 超预算立即停止本轮（倍率越高越明显），保证 <b>1024 倍也不会把服务器拖崩</b>。
 */
public final class WandAcceleration
{
    /** 新建标记时的默认倍率 */
    public static final int DEFAULT_SPEED = 2;
    /** 倍率上限（超过则回到默认） */
    public static final int MAX_SPEED = 1024;
    /** 标记的默认持续时间（tick）= 30 秒 */
    public static final int DEFAULT_DURATION_TICKS = 600;
    /** 避雷针单次最多生成多少道落雷（防 1024 倍刷爆实体） */
    public static final int MAX_BOLTS = 8;
    /** 每个服务器 tick 的加速时间预算（纳秒） */
    public static final long BUDGET_NANOS = 3_000_000L;
    /** 看向天空时的日间推进量（tick） */
    public static final int TIME_SKIP = 100;

    private static final Direction[] DIRECTIONS = Direction.values();

    /** 标记：速度 + 剩余 tick（负数 = 永久） */
    private static final class Mark
    {
        int speed;
        int remaining;

        Mark(int speed, int remaining)
        {
            this.speed = speed;
            this.remaining = remaining;
        }
    }

    private static final Map<ResourceKey<Level>, Map<BlockPos, Mark>> MARKS = new HashMap<>();

    private WandAcceleration() {}

    /** 在坐标上落一个加速标记；已存在则倍率翻倍（超过上限回到默认）并刷新时长 */
    public static int mark(ServerLevel level, BlockPos pos, boolean permanent)
    {
        Map<BlockPos, Mark> map = MARKS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Mark mark = map.get(pos);
        int speed;
        if (mark == null)
        {
            speed = DEFAULT_SPEED;
            map.put(pos.immutable(), new Mark(speed, permanent ? -1 : DEFAULT_DURATION_TICKS));
        }
        else
        {
            speed = mark.speed * 2;
            if (speed > MAX_SPEED)
            {
                speed = DEFAULT_SPEED;
            }
            mark.speed = speed;
            mark.remaining = permanent ? -1 : DEFAULT_DURATION_TICKS;
        }
        return speed;
    }

    /** 移除某坐标的标记 */
    public static boolean clear(ServerLevel level, BlockPos pos)
    {
        Map<BlockPos, Mark> map = MARKS.get(level.dimension());
        return map != null && map.remove(pos) != null;
    }

    /** 该坐标当前是否有加速 */
    public static boolean isMarked(ServerLevel level, BlockPos pos)
    {
        Map<BlockPos, Mark> map = MARKS.get(level.dimension());
        return map != null && map.containsKey(pos);
    }

    /** 当前标记总数（提示用） */
    public static int count(ServerLevel level)
    {
        Map<BlockPos, Mark> map = MARKS.get(level.dimension());
        return map == null ? 0 : map.size();
    }

    /**
     * 每服务器 tick 驱动全部标记。由 {@code WandAccelerationHandler} 调用。
     *
     * @return 本 tick 实际执行的虚拟刻数
     */
    public static int tickAll(ServerLevel level)
    {
        Map<BlockPos, Mark> map = MARKS.get(level.dimension());
        if (map == null || map.isEmpty())
        {
            return 0;
        }
        long deadline = System.nanoTime() + BUDGET_NANOS;
        int executed = 0;
        Iterator<Map.Entry<BlockPos, Mark>> it = map.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<BlockPos, Mark> entry = it.next();
            Mark mark = entry.getValue();
            if (mark.remaining == 0)
            {
                it.remove();
                continue;
            }
            if (System.nanoTime() >= deadline)
            {
                break; // 超预算：剩下的下个 tick 继续
            }
            BlockPos pos = entry.getKey();
            if (!level.isLoaded(pos))
            {
                continue;
            }
            executed += tickTarget(level, pos, mark.speed, deadline);
            if (mark.remaining > 0)
            {
                mark.remaining--;
            }
        }
        if (map.isEmpty())
        {
            MARKS.remove(level.dimension());
        }
        return executed;
    }

    /** 清空某维度的全部标记（换维度/卸载时用） */
    public static void clearAll(ServerLevel level)
    {
        MARKS.remove(level.dimension());
    }

    /** 加速单个方块（一次调用 = speed 个虚拟刻，受 deadline 约束） */
    public static int tickTarget(ServerLevel level, BlockPos pos, int speed, long deadline)
    {
        if (speed <= 0 || System.nanoTime() >= deadline)
        {
            return 0;
        }
        BlockState state = level.getBlockState(pos);
        // ① 避雷针：真实落雷
        if (state.getBlock() instanceof LightningRodBlock)
        {
            int bolts = Math.min(speed, MAX_BOLTS);
            for (int i = 0; i < bolts; i++)
            {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt == null)
                {
                    continue;
                }
                bolt.setVisualOnly(false);
                bolt.moveTo(Vec3.atBottomCenterOf(pos.above()));
                level.addFreshEntity(bolt);
            }
            return bolts;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        // ② 无方块实体：随机刻方块（作物 / 树苗等）—— 一次虚拟刻 = 一次 randomTick
        if (blockEntity == null)
        {
            if (!state.isRandomlyTicking())
            {
                return speed;
            }
            int executed = 0;
            for (; executed < speed && System.nanoTime() < deadline; executed++)
            {
                BlockState current = level.getBlockState(pos);
                if (!current.isRandomlyTicking())
                {
                    return executed;
                }
                current.randomTick(level, pos, level.getRandom());
            }
            return executed;
        }

        // ③ AE 机器：优先走 AE 网格刻
        int ae = tickAeNodes(level, pos, speed, deadline);
        if (ae >= 0)
        {
            return ae;
        }

        // ④ 其它有方块实体的方块：反复调用它自己的 ticker
        @SuppressWarnings("rawtypes")
        BlockEntityTicker ticker = state.getTicker(level, blockEntity.getType());
        if (ticker == null)
        {
            return speed;
        }
        int executed = 0;
        for (; executed < speed && System.nanoTime() < deadline; executed++)
        {
            if (blockEntity.isRemoved() || level.getBlockState(pos) != state)
            {
                return executed;
            }
            //noinspection unchecked
            ticker.tick(level, pos, state, blockEntity);
        }
        return executed;
    }

    /**
     * 驱动该方块暴露的全部 AE 端点（多方块主机可能多个面返回同一节点，需要按身份去重）。
     *
     * @return 消耗的虚拟刻数；<b>-1 表示这个方块根本不是 AE 节点</b>（交给调用方走普通 ticker）
     */
    private static int tickAeNodes(ServerLevel level, BlockPos pos, int speed, long deadline)
    {
        IInWorldGridNodeHost host = GridHelper.getNodeHost(level, pos);
        if (host == null)
        {
            return -1;
        }
        Set<IGridNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<IGridNode> active = new ArrayList<>();
        List<IGridTickable> tickables = new ArrayList<>();
        for (Direction direction : DIRECTIONS)
        {
            IGridNode node = host.getGridNode(direction);
            if (node == null || !seen.add(node))
            {
                continue;
            }
            IGridTickable tickable = node.getService(IGridTickable.class);
            if (tickable != null)
            {
                active.add(node);
                tickables.add(tickable);
            }
        }
        if (active.isEmpty())
        {
            return -1;
        }
        int executed = 0;
        for (; executed < speed && !active.isEmpty() && System.nanoTime() < deadline; executed++)
        {
            for (int i = active.size() - 1; i >= 0; i--)
            {
                try
                {
                    TickRateModulation modulation = tickables.get(i).tickingRequest(active.get(i), 1);
                    if (modulation == TickRateModulation.SLEEP)
                    {
                        active.remove(i);
                        tickables.remove(i);
                    }
                }
                catch (RuntimeException ignored)
                {
                    // 设备可能拒绝越界刻（未加载/未激活）：只摘掉这个端点，其它继续
                    active.remove(i);
                    tickables.remove(i);
                }
            }
        }
        return active.isEmpty() ? speed : executed;
    }
}