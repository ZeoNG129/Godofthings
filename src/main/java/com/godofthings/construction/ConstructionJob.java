package com.godofthings.construction;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 一次批量建造 / 销毁 / 替换的<b>跨 tick 作业</b>。
 *
 * <p>改造前 {@link ConstructionActions} 是「一次点击内 BFS 到底、顺手全部执行」，神之建筑（1024 / 81 / 1024）
 * 那点量无所谓；天神建筑的格子上限是 {@code Integer.MAX_VALUE}（见 {@link ConstructionTier#HEAVENLY}），
 * 一次点击可能命中几十万格，一个 tick 里做完必然把服务端卡死。所以这里把
 * <b>BFS 与执行交错</b>成一台状态机：每次 tick 从队列取一个坐标 → 做原有判定 → 执行 → 成功则把邻居入队，
 * 一个 tick 最多处理 {@link #PER_TICK_BUDGET} 个格子，剩下的顺延到下一个 tick。</p>
 *
 * <p>状态机要点：</p>
 * <ul>
 *   <li><b>谁能取消</b>：只有三个条件 —— 玩家下线、换了维度、手上已经没有本族物品（主副手都没有）；
 *       另外同一玩家再点一次会先把旧作业收摊（一个玩家同时只有一个作业）。服务器换了实例
 *       （单人换存档）也会丢弃。</li>
 *   <li><b>撤销何时落库</b>：作业<b>结束</b>时（队列空 / 已达格子上限 / 材料用完 / 被取消）把这次积累的
 *       {@link ConstructionUndo.BlockChange} 一次性 {@link ConstructionUndo#push} 成一条记录 ——
 *       与改造前「一次操作压一条」完全一致，撤销永远按整轮回滚，不会只回滚半轮。</li>
 *   <li><b>材料何时扣</b>：逐格扣（和改造前一样），每成功放 / 换一格扣 1 个；扣不到就整轮停下
 *       （不是跳过这一格），已扣的不会退。</li>
 *   <li><b>音效</b>：第一格成功时播一次（{@code soundPlayed}），跨 tick 也不会重复播。</li>
 * </ul>
 *
 * <p>神之建筑（{@link ConstructionTier#GOD}）的作业量最多 1024 格 &lt; 每 tick 预算 8192，
 * 所以登记后当场就在<b>同一 tick</b> 内跑完（{@link #register} 里直接推进一次），表现与改造前一致；
 * 只有超预算的作业才会顺延。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class ConstructionJob
{
    /**
     * 每个 tick 最多处理多少个格子（BFS 出队次数）。
     *
     * <p>8192 = 神之建筑 1024 格上限的 8 倍，保证 GOD 品阶一定在发起的那一 tick 内跑完；
     * 1 tick = 50ms，8192 次「读 1~2 个方块状态 + 可能 1 次 setBlock」在服务端是可接受的开销。</p>
     */
    public static final int PER_TICK_BUDGET = 8192;

    /** 玩家 UUID → 正在跑的作业（同一玩家同时只允许一个） */
    private static final Map<UUID, ConstructionJob> SESSIONS = new HashMap<>();

    /** 作业种类 */
    public enum Kind
    {
        /** 建造：贴着被点面平面扩展铺方块 */
        BUILD,
        /** 销毁：拆连通块（不掉落） */
        DESTROY,
        /** 替换：连通块换成副手方块 */
        REPLACE
    }

    private final UUID playerId;
    /** 发起作业的服务器实例：换了实例（单人换存档）就丢弃，免得拿旧坐标在新存档里接着跑 */
    private final MinecraftServer server;
    /** 发起作业时玩家所在的维度：换维度就取消 */
    private final ResourceKey<Level> dimension;
    private final Kind kind;
    /** 待处理队列（队首 = 下一个要判定的格子） */
    private final Deque<BlockPos> queue = new ArrayDeque<>();
    /** 已入过队的格子，防止 BFS 反复入同一个格 */
    private final Set<BlockPos> visited = new HashSet<>();
    /**
     * 建造：被点方块的 state（复制朝向的模板）；
     * 替换：要换成的那个新方块的 state；销毁：被点方块的 state（只为排查方便，不参与判定）。
     */
    private final BlockState template;
    /** 销毁 / 替换要处理的方块类型（建造不用它：建造靠 {@link #accepted} + {@link #template} 判定） */
    private final Block targetBlock;
    /** 建造：能当「支撑面」的方块集合（相似方块开关已经在这里摊平为 O(1) 查询）；销毁 / 替换为空集 */
    private final Set<Block> accepted;
    /** 被点击的面（建造用来定扩散平面与「背面」，销毁用来判「朝玩家那侧可透过」；替换不用，传 null） */
    private final Direction face;
    /** 本次作业的格子上限（按物品品阶取，见 {@link ConstructionTier}） */
    private final int limit;
    /** 这次作业已经动过的格子（结束 / 取消时一次性压成一条撤销记录） */
    private final List<ConstructionUndo.BlockChange> changes = new ArrayList<>();
    /** 每格要扣的材料（建造 = 模板方块的物品，副手优先时就是副手那个方块；替换 = 副手方块；销毁为空栈） */
    private final ItemStack wanted;

    /** 已成功处理的格子数（到 {@link #limit} 就收工） */
    private int done;
    /** 一次操作只播一次放置音效 */
    private boolean soundPlayed;
    /** 材料用完：整轮停下、不再出队（<b>不是</b>「已收摊」，撤销记录照常落库） */
    private boolean stopped;
    /** 已收摊（撤销已落库 + 已从会话表摘掉）；单独一个标记是为了防止重复 push */
    private boolean closed;

    private ConstructionJob(Player player, Kind kind, BlockPos start, Direction face, int limit,
            BlockState template, Block targetBlock, Set<Block> accepted, ItemStack wanted)
    {
        this.playerId = player.getUUID();
        this.server = player.getServer();
        this.dimension = ((ServerLevel) player.level()).dimension();
        this.kind = kind;
        this.face = face;
        this.limit = limit;
        this.template = template;
        this.targetBlock = targetBlock;
        this.accepted = accepted;
        this.wanted = wanted;

        BlockPos first = start.immutable();
        this.queue.addLast(first);
        this.visited.add(first);
    }

    // ==================================================================================
    // 三个入口（ConstructionActions 调；登记后立刻推进一次）
    // ==================================================================================

    /**
     * 登记一次建造作业：从 {@code start}（被点面外侧的第一格）起平面扩展。
     *
     * @param template 放下去的模板 state（副手拿着方块物品时是<b>副手方块</b>的 defaultBlockState，
     *                 见 {@link ConstructionActions#buildTemplate}）
     * @param accepted 能当支撑面的方块集合（永远按<b>被点方块</b>算，见 {@link ConstructionActions#similarBlocks}）
     * @param wanted   每格要扣的材料（与 {@code template} 同一个方块物品，扣取顺序见
     *                 {@link GodConstructionItem#takeMaterial}：副手 → 主背包 → AE）
     */
    static void startBuild(Player player, ItemStack wand, BlockState template, Set<Block> accepted,
            BlockPos start, Direction face, ItemStack wanted)
    {
        ConstructionJob job = new ConstructionJob(player, Kind.BUILD, start, face,
                GodConstructionItem.getTier(wand).getMaxBuild(), template, template.getBlock(), accepted, wanted);
        register(player, job);
    }

    /** 登记一次销毁作业：从被点方块（{@code target}）起拆连通块 */
    static void startDestroy(Player player, ItemStack wand, BlockState target, BlockPos origin, Direction face)
    {
        ConstructionJob job = new ConstructionJob(player, Kind.DESTROY, origin, face,
                GodConstructionItem.getTier(wand).getMaxDestroy(), target, target.getBlock(), Set.of(),
                ItemStack.EMPTY);
        register(player, job);
    }

    /** 登记一次替换作业：从被点方块（{@code target}）起把连通块换成 {@code newState} */
    static void startReplace(Player player, ItemStack wand, Block target, BlockState newState,
            BlockPos origin, ItemStack material)
    {
        ConstructionJob job = new ConstructionJob(player, Kind.REPLACE, origin, null,
                GodConstructionItem.getTier(wand).getMaxReplace(), newState, target, Set.of(), material);
        register(player, job);
    }

    /**
     * 登记并<b>立刻推进一次</b>：GOD 品阶（≤ 1024 格）在 {@link #PER_TICK_BUDGET} 内当场跑完，
     * 所以神之建筑的表现与「一次点击全部做完」完全一致；超预算的才留到 {@link #onServerTick} 继续。
     */
    private static void register(Player player, ConstructionJob job)
    {
        // 同一玩家手上又点了一次：先把上一个作业收摊（已做的改动保留，撤销照常落库）
        ConstructionJob previous = SESSIONS.remove(player.getUUID());
        if (previous != null)
        {
            previous.finish(player);
        }
        SESSIONS.put(player.getUUID(), job);
        job.run(player);
    }

    // ==================================================================================
    // 每 tick 推进
    // ==================================================================================

    /** 对所有在跑的作业推进一格预算；顺手把该取消的作业取消掉，并清掉过期的「Ctrl 强制施工」标记 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        MinecraftServer server = event.getServer();
        // 过期标记跟作业跑不跑没关系，每 tick 都要清（见 ConstructionForce）
        ConstructionForce.cleanup(server);
        if (SESSIONS.isEmpty())
        {
            return;
        }
        // 快照一份：推进 / 取消过程中会往 SESSIONS 里删
        for (ConstructionJob job : List.copyOf(SESSIONS.values()))
        {
            Player player = server.getPlayerList().getPlayer(job.playerId);
            if (player == null || job.server != server)
            {
                // 玩家不在了（旧服务器实例同理）：撤销记录没有地方落，只保留已完成的方块改动
                job.drop();
                continue;
            }
            if (!job.dimension.equals(((ServerLevel) player.level()).dimension())
                    || !GodConstructionItem.isWand(GodConstructionItem.findWand(player)))
            {
                // 换维度 / 手上没棒子了：取消，已完成的部分保留且撤销照常落库
                job.finish(player);
                continue;
            }
            job.run(player);
        }
    }

    /** 玩家下线：先把撤销记录落到这个玩家身上，再丢弃作业（他下次进游戏还能撤回来） */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (!(event.getEntity() instanceof Player player))
        {
            return;
        }
        // 「Ctrl 强制施工」的一次性标记跟着作废（和有没有作业无关）
        ConstructionForce.forget(player);
        if (SESSIONS.isEmpty())
        {
            return;
        }
        ConstructionJob job = SESSIONS.get(player.getUUID());
        if (job != null)
        {
            job.finish(player);
        }
    }

    /**
     * 推进这个作业：本 tick 最多处理 {@link #PER_TICK_BUDGET} 个格子，队列空 / 到上限 / 材料用完就收摊。
     */
    private void run(Player player)
    {
        ItemStack wand = GodConstructionItem.findWand(player);
        if (!GodConstructionItem.isWand(wand))
        {
            finish(player);
            return;
        }

        int budget = PER_TICK_BUDGET;
        while (budget-- > 0 && !isDone())
        {
            step(player, wand);
        }
        if (isDone())
        {
            finish(player);
        }
    }

    /** 队列空 / 已达格子上限 / 材料用完 / 已收摊，都算结束 */
    private boolean isDone()
    {
        return this.stopped || this.closed || this.done >= this.limit || this.queue.isEmpty();
    }

    /**
     * 收摊：撤销记录一次性落库 + 从会话表摘掉。
     *
     * <p>{@link #closed} 保证只 push 一次（材料用完 <b>不会</b>跳过 push —— 已经放下去的方块也要能撤回）。</p>
     */
    private void finish(Player player)
    {
        if (!this.closed)
        {
            this.closed = true;
            ConstructionUndo.push(player, this.changes);
        }
        SESSIONS.remove(this.playerId, this);
    }

    /** 拿不到玩家（下线 / 旧服务器实例）时直接丢弃：改动保留，撤销记录只能丢 */
    private void drop()
    {
        this.closed = true;
        SESSIONS.remove(this.playerId, this);
    }

    /** 处理队首那一格（判定 + 执行 + 扩散） */
    private void step(Player player, ItemStack wand)
    {
        BlockPos pos = this.queue.removeFirst();
        switch (this.kind)
        {
            case BUILD -> stepBuild(player, wand, pos);
            case DESTROY -> stepDestroy(player, pos);
            case REPLACE -> stepReplace(player, wand, pos);
        }
    }

    // ==================================================================================
    // 三种作业的单格处理（判定顺序与改造前逐条一致）
    // ==================================================================================

    /** 建造：背后是支撑面 → 可替换 → 能存活 → 可交互 → 扣材料 → 落方块 → 平面扩散 */
    private void stepBuild(Player player, ItemStack wand, BlockPos pos)
    {
        ServerLevel level = ((ServerLevel) player.level());
        // 1) 背后那一格必须还是被点的方块（开了相似开关则允许相似方块）
        BlockState behind = level.getBlockState(pos.relative(this.face.getOpposite()));
        if (!this.accepted.contains(behind.getBlock()))
        {
            return;
        }
        // 2) 目标格必须是空气或可替换方块
        if (!level.getBlockState(pos).canBeReplaced())
        {
            return;
        }
        // 3) 以被点方块的 state 为模板，复制背面方块的朝向；放下去必须能存活、且没被保护插件拦住
        BlockState newState = ConstructionActions.copyRotation(this.template, behind);
        if (!newState.canSurvive(level, pos) || !level.mayInteract(player, pos))
        {
            return;
        }
        // 4) 材料用完就整轮停下（不是跳过这一格，免得铺出参差不齐的一片）
        if (!GodConstructionItem.takeMaterial(player, wand, this.wanted))
        {
            this.stopped = true;
            return;
        }

        // 记下放置前的状态（空气 / 被替换掉的方块），撤销时原样放回去
        BlockState oldState = level.getBlockState(pos);
        level.setBlock(pos, newState, 2);
        this.changes.add(new ConstructionUndo.BlockChange(pos, oldState));
        this.done++;
        playSoundOnce(level, pos);
        ConstructionActions.expandPlane(this.queue, this.visited, pos, this.face);
    }

    /** 销毁：朝玩家那侧可透过 → 同种方块 → 无方块实体 / 可破坏 / 可交互 → 没被保护插件取消 → 拆掉 */
    private void stepDestroy(Player player, BlockPos pos)
    {
        ServerLevel level = ((ServerLevel) player.level());
        // 1) 面向玩家的那一侧必须能透过（照 StickUtil#isBlockPermeable）
        if (!ConstructionActions.isBlockPermeable(level, pos.relative(this.face)))
        {
            return;
        }
        // 2) 只拆被点的那种方块
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != this.targetBlock)
        {
            return;
        }
        // 3) 带方块实体的、不可破坏的、被保护插件禁止交互的都跳过
        if (level.getBlockEntity(pos) != null || state.getDestroySpeed(level, pos) < 0.0F
                || !level.mayInteract(player, pos))
        {
            return;
        }
        // 4) 领地 / 保护插件有最后一次否决权
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
        if (NeoForge.EVENT_BUS.post(breakEvent).isCanceled())
        {
            return;
        }

        level.removeBlock(pos, false); // 不掉落
        this.changes.add(new ConstructionUndo.BlockChange(pos, state));
        this.done++;
        pushNeighbors(pos);
    }

    /** 替换：非空气 / 有碰撞体 / 不是目标方块本身 → 同种方块 → 能存活 / 可交互 / 无方块实体 → 扣材料 → 换掉 */
    private void stepReplace(Player player, ItemStack wand, BlockPos pos)
    {
        ServerLevel level = ((ServerLevel) player.level());
        BlockState oldState = level.getBlockState(pos);

        // 跳过空气、没有碰撞体的方块、已经是目标方块的格子（照 ActionReplace）
        if (oldState.isAir() || !oldState.blocksMotion() || oldState.getBlock() == this.template.getBlock())
        {
            return;
        }
        // 只换被点的那种方块
        if (oldState.getBlock() != this.targetBlock)
        {
            return;
        }
        if (!this.template.canSurvive(level, pos))
        {
            return;
        }
        if (level.getBlockEntity(pos) != null || !level.mayInteract(player, pos))
        {
            return;
        }
        // 替换同样会移除旧方块，按保护插件惯例先问一声
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, oldState, player);
        if (NeoForge.EVENT_BUS.post(breakEvent).isCanceled())
        {
            return;
        }
        // 材料用完就整轮停下
        if (!GodConstructionItem.takeMaterial(player, wand, this.wanted))
        {
            this.stopped = true;
            return;
        }

        // 旧方块掉成物品（照 ReplaceSnapshot：只用方块物品，不走战利品表）
        ItemStack oldItem = new ItemStack(oldState.getBlock().asItem());
        if (!oldItem.isEmpty())
        {
            Block.popResource(level, pos, oldItem);
        }

        level.setBlock(pos, this.template, 2);
        this.changes.add(new ConstructionUndo.BlockChange(pos, oldState));
        this.done++;
        pushNeighbors(pos);
    }

    // ==================================================================================
    // 工具方法
    // ==================================================================================

    /** 六个正交邻居入队（销毁 / 替换是三维连通块） */
    private void pushNeighbors(BlockPos pos)
    {
        for (Direction direction : Direction.values())
        {
            ConstructionActions.pushCandidate(this.queue, this.visited, pos.relative(direction));
        }
    }

    /** 一次操作只在第一格播放一次放置音效（跨 tick 也只播一次） */
    private void playSoundOnce(ServerLevel level, BlockPos pos)
    {
        if (!this.soundPlayed)
        {
            GodConstructionItem.playPlaceSound(level, pos);
            this.soundPlayed = true;
        }
    }
}
