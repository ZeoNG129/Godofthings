package com.godofthings.aeskill.divine;

import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.godofthings.aeskill.AeSkillEffects;
import com.godofthings.aeskill.AeSkills;
import com.godofthings.aeskill.TickBudget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 「神明之力」——有 AE 合成任务激活时，给整条产线上被供货的机器额外的原生 tick。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.divine.DivineGrace}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），<b>逐行对照编写</b>，常量与闸门语义一个不改。</p>
 *
 * <h2>为什么是「重复调用」而不是「改倍率字段」</h2>
 * 与装配超频一致：同一个服务器 tick 内多次执行机器自己的 {@code tickingRequest} /
 * {@code BlockEntityTicker.tick}。这样任何能被 AE2 样板驱动的机器（AE2 原生
 * {@link ICraftingMachine}、把机器暴露成外部库存的第三方方块）都不需要逐个适配，
 * 它们自己的进度、耗电、产出规则全被保留。
 *
 * <h2>目标发现</h2>
 * 样板供应器每次成功推样板时会被记下；这些供应器「配置朝向」相邻的机器就是加速目标。
 * <b>刻意不做「按网格枚举全部装配机」</b>：那会把技能从「这条产线」放大成「整个基地」。
 *
 * <h2>三重激活闸门</h2>
 * 有等级 → 网格上有未暂停的合成任务 → 该供应器属于活跃网格。任一不满足立即清空状态，
 * 因此任务结束/取消/暂停后不会残留「上一单的机器」继续被加速。
 */
public final class DivineGrace
{
    /** 供应器热度上限（tick = 60 秒），只用于清理已失效的供应器，不参与激活判定。 */
    private static final int PROVIDER_TTL_TICKS = 1200;

    /** 单台机器每 tick 的额外次数硬上限（倍率 1024 - 1）。 */
    private static final int MAX_EXTRA_PER_MACHINE = 1023;

    /** 同一 tick 内所有机器合计的额外次数硬上限。 */
    private static final int MAX_TOTAL_EXTRA = 8192;

    /** 本功能每次进入的单片时间预算：2.5ms。 */
    private static final long ALLOWANCE_NANOS = 2_500_000L;

    /** 最近成功推过样板的供应器 → 最后一次推送的 tick。 */
    private static final Map<BlockEntity, Long> HOT_PROVIDERS = new IdentityHashMap<>();

    /** 本 tick 解析出的目标集合（身份集合，去重不依赖 equals）。 */
    private static final Set<BlockEntity> TARGETS = Collections.newSetFromMap(new IdentityHashMap<>());

    /** 本 tick 实际执行过额外 tick 的机器；{@link #isAccelerated} 只认它。 */
    private static final Set<BlockEntity> ACCELERATED = Collections.newSetFromMap(new IdentityHashMap<>());

    private static final List<BlockEntity> ORDERED = new ArrayList<>();

    private static int cursor;

    private static long lastTick = Long.MIN_VALUE;

    /** 0 表示当前没有在加速。 */
    private static volatile int activeMultiplier;

    private DivineGrace() {}

    /** 供应器成功把样板推给机器时调用。 */
    public static void onPush(BlockEntity provider)
    {
        if (AeSkillEffects.getDivineLevel() <= 0)
        {
            return;
        }
        Level level = provider.getLevel();
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel))
        {
            return;
        }
        HOT_PROVIDERS.put(provider, (long) serverLevel.getServer().getTickCount());
    }

    /**
     * 该方块实体本 tick 是否已被神明之力实际加速过（读「本 tick 真的跑过」的集合，无相位差）。
     */
    public static boolean isAccelerated(BlockEntity blockEntity)
    {
        return blockEntity != null && !ACCELERATED.isEmpty() && ACCELERATED.contains(blockEntity);
    }

    public static int activeMultiplier()
    {
        return activeMultiplier;
    }

    public static void reset()
    {
        stop();
        lastTick = Long.MIN_VALUE;
    }

    /** 只清运行期状态，不动 tick 计数（供闸门关闭时使用）。 */
    private static void stop()
    {
        HOT_PROVIDERS.clear();
        TARGETS.clear();
        ACCELERATED.clear();
        ORDERED.clear();
        cursor = 0;
        activeMultiplier = 0;
    }

    /** 服务器 tick 开头执行：决定激活、重建目标、发放额外 tick。 */
    public static void tick(MinecraftServer server)
    {
        long now = server.getTickCount();
        // 同一 tick 的重复入口直接返回，且绝不能在这里清 ACCELERATED：
        // 本 tick 的「装配超频让路」判定依赖它，清空了会让同一台机器吃两份额外 tick。
        if (now == lastTick)
        {
            return;
        }
        lastTick = now;
        ACCELERATED.clear();
        int level = AeSkillEffects.getDivineLevel();
        if (level <= 0)
        {
            if (activeMultiplier != 0 || !TARGETS.isEmpty() || !HOT_PROVIDERS.isEmpty())
            {
                stop();
            }
            return;
        }
        HOT_PROVIDERS.entrySet().removeIf(entry -> now - entry.getValue() > PROVIDER_TTL_TICKS
                || entry.getKey().isRemoved() || entry.getKey().getLevel() == null);
        // 闸门 1：只有「网格上还有未暂停任务」的供应器才算产线仍然活着。
        Set<IGrid> live = activeGrids();
        if (live.isEmpty())
        {
            if (activeMultiplier != 0 || !TARGETS.isEmpty() || !HOT_PROVIDERS.isEmpty())
            {
                stop();
            }
            return;
        }
        // 闸门 2：产线机器只取活跃网格里的供应器，绝不跨网格顺手加速。
        resolveTargets(live);
        if (ORDERED.isEmpty())
        {
            activeMultiplier = 0;
            return;
        }
        resolveMultiplier(level);
        accelerate(activeMultiplier);
    }

    private static void resolveMultiplier(int level)
    {
        int next = AeSkills.divineMultiplier(level);
        if (next != activeMultiplier)
        {
            activeMultiplier = next;
        }
    }

    /** 收集「网格上还有未暂停任务」的网络；同一网络的多个供应器只查一次 CPU 列表。 */
    private static Set<IGrid> activeGrids()
    {
        Set<IGrid> live = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<IGrid> checked = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockEntity provider : HOT_PROVIDERS.keySet())
        {
            if (!(provider instanceof PatternProviderLogicHost host))
            {
                continue;
            }
            IGrid grid;
            try
            {
                grid = host.getGrid();
            }
            catch (RuntimeException ignored)
            {
                continue;
            }
            if (grid == null || !checked.add(grid))
            {
                continue;
            }
            if (hasActiveJob(grid))
            {
                live.add(grid);
            }
        }
        return live;
    }

    private static boolean hasActiveJob(IGrid grid)
    {
        ICraftingService service = grid.getCraftingService();
        if (service == null)
        {
            return false;
        }
        for (ICraftingCPU cpu : service.getCpus())
        {
            if (cpu instanceof CraftingCPUCluster cluster)
            {
                var logic = cluster.craftingLogic;
                if (logic != null && logic.hasJob() && !logic.isJobSuspended())
                {
                    return true;
                }
                continue;
            }
            // 第三方自有 CPU 实现拿不到 craftingLogic，但公开了 isBusy()。
            if (cpu != null && cpu.isBusy())
            {
                return true;
            }
        }
        return false;
    }

    /** 目标 = 活跃网格上、推过样板的供应器在其配置朝向上紧邻的机器。 */
    private static void resolveTargets(Set<IGrid> live)
    {
        TARGETS.clear();
        ORDERED.clear();
        for (BlockEntity provider : HOT_PROVIDERS.keySet())
        {
            if (!(provider instanceof PatternProviderLogicHost host))
            {
                continue;
            }
            Level level = provider.getLevel();
            if (level == null || provider.isRemoved())
            {
                continue;
            }
            if (!belongsTo(host, live))
            {
                continue;
            }
            for (Direction side : host.getTargets())
            {
                BlockPos pos = provider.getBlockPos().relative(side);
                if (!level.isLoaded(pos))
                {
                    continue;
                }
                BlockEntity target = level.getBlockEntity(pos);
                if (target == null || target == provider || target.isRemoved())
                {
                    continue;
                }
                if (!isMachineTarget(target, level))
                {
                    continue;
                }
                if (TARGETS.add(target))
                {
                    ORDERED.add(target);
                }
            }
        }
    }

    private static boolean belongsTo(PatternProviderLogicHost host, Set<IGrid> live)
    {
        try
        {
            IGrid grid = host.getGrid();
            return grid != null && live.contains(grid);
        }
        catch (RuntimeException ignored)
        {
            return false;
        }
    }

    private static boolean isMachineTarget(BlockEntity target, Level level)
    {
        // 供应器/接口彼此相邻属于常见布局，它们不是产线机器。
        if (target instanceof PatternProviderLogicHost)
        {
            return false;
        }
        // 注意：装配机一旦拿到样板 acceptsPlans() 就会变 false，不能用它做过滤，
        // 否则「正在干活的机器」反而被排除，加速完全不生效。
        if (target instanceof ICraftingMachine)
        {
            return true;
        }
        // 通用路径：把物品送进普通方块库存的机器，只要有方块 ticker 就能被额外 tick。
        return tickerOf(level, target) != null;
    }

    private static void accelerate(int multiplier)
    {
        int extra = Math.min(MAX_EXTRA_PER_MACHINE, multiplier - 1);
        if (extra <= 0)
        {
            return;
        }
        long began = System.nanoTime();
        long deadline = TickBudget.deadline(began, ALLOWANCE_NANOS);
        if (deadline <= began)
        {
            return;
        }
        int size = ORDERED.size();
        int start = Math.floorMod(cursor, size);
        // served = 已「完整服务完」的台数。提前撞到时间/次数上限时不推进 cursor，
        // 否则 ORDERED 头部那几台会永远吃掉全部配额，队尾机器被系统性饿死。
        int served = 0;
        int total = 0;
        try
        {
            outer:
            for (int n = 0; n < size; n++)
            {
                BlockEntity target = ORDERED.get((start + n) % size);
                Level level = target.getLevel();
                if (level == null || target.isRemoved())
                {
                    served = n + 1;
                    continue;
                }
                boolean complete = true;
                for (int i = 0; i < extra; i++)
                {
                    if (total >= MAX_TOTAL_EXTRA)
                    {
                        complete = false;
                        break outer;
                    }
                    if (System.nanoTime() >= deadline)
                    {
                        complete = false;
                        break outer;
                    }
                    if (!runOnce(level, target))
                    {
                        break;
                    }
                    total++;
                    ACCELERATED.add(target);
                }
                if (complete)
                {
                    served = n + 1;
                }
            }
            cursor = (start + served) % size;
        }
        finally
        {
            TickBudget.charge(began);
        }
    }

    /** 优先走网格 tickable，其次走方块 ticker；返回 false 表示机器已休眠。 */
    private static boolean runOnce(Level level, BlockEntity target)
    {
        IGridNode node = nodeOf(target);
        if (node != null && node.isActive())
        {
            IGridTickable tickable = node.getService(IGridTickable.class);
            if (tickable != null)
            {
                TickRateModulation modulation = tickable.tickingRequest(node, 1);
                return modulation != TickRateModulation.SLEEP && modulation != TickRateModulation.IDLE;
            }
        }
        BlockEntityTicker<BlockEntity> ticker = tickerOf(level, target);
        if (ticker == null)
        {
            return false;
        }
        ticker.tick(level, target.getBlockPos(), target.getBlockState(), target);
        return true;
    }

    private static IGridNode nodeOf(BlockEntity target)
    {
        if (!(target instanceof IInWorldGridNodeHost host))
        {
            return null;
        }
        IGridNode node = host.getGridNode(null);
        if (node != null)
        {
            return node;
        }
        for (Direction side : Direction.values())
        {
            node = host.getGridNode(side);
            if (node != null)
            {
                return node;
            }
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static BlockEntityTicker<BlockEntity> tickerOf(Level level, BlockEntity target)
    {
        BlockState state = target.getBlockState();
        return state.getTicker(level, (BlockEntityType) target.getType());
    }
}
