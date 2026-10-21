package com.godofthings.construction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 神之建筑 / 天神建筑的四种批量动作（建造 / 天使放置 / 销毁 / 替换），全部照 ConstructionSticks 的
 * ActionConstruction / ActionAngel / ActionDestruction / ActionReplace 精简而来。
 *
 * <p>三种批量动作（建造 / 销毁 / 替换）在这里只做<b>前置校验并登记作业</b>（{@link ConstructionJob}），
 * 真正的 BFS 与落方块在作业里按 tick 预算推进 —— 天神建筑的格子上限是 {@code Integer.MAX_VALUE}，
 * 一次点击命中几十万格时不能把服务端卡住一帧。神之建筑（≤ 1024 格）的作业会在同一 tick 内跑完，
 * 表现与改造前一致。</p>
 *
 * <p>方向约定（最容易写错的地方）：</p>
 * <ul>
 *   <li>{@code face} = {@code UseOnContext#getClickedFace()}，即<b>玩家点中的那个面</b>，
 *       也就是朝向玩家的那一面（点地面就是 {@code UP}）。</li>
 *   <li>{@code clickedPos.relative(face)} = 贴着被点面<b>外侧</b>的那一格，建造的第一个候选。</li>
 *   <li>{@code face.getOpposite()} = 从候选格<b>指回方块本体</b>的方向；候选格的「背面」
 *       （{@code pos.relative(face.getOpposite())}）就是它脚下的支撑方块。</li>
 *   <li>plane 扩散只在垂直于 {@code face} 的平面内进行（点地面 = 在 XZ 平面铺，点侧面 = 在竖直面铺）。</li>
 * </ul>
 *
 * <p>上游里那些依赖 {@code StickUtil} / {@code ContainerManager} / 配置类的部分都换成了最直白的等价写法：
 * 距离与坐标合法性用 {@code level.mayInteract} + BFS 上限代替 {@code isPositionModifiable}，
 * 物品扣取用「遍历背包 + AE 网络」，放置 / 破坏事件照旧发 NeoForge 事件。</p>
 */
public final class ConstructionActions
{
    /** 建造时要跟随被点方块复制的朝向 / 形状属性（存在才复制，照 PlaceSnapshot 的子集） */
    private static final Property<?>[] ROTATION_PROPERTIES =
            {
                    BlockStateProperties.HORIZONTAL_FACING,
                    BlockStateProperties.FACING,
                    BlockStateProperties.AXIS,
                    BlockStateProperties.HALF,
                    BlockStateProperties.ROTATION_16
            };

    /** 天使放置沿视线前进的步长（格） */
    public static final double ANGEL_STEP = 0.25;
    /**
     * 天使放置最多沿视线走多少格（<b>安全帽</b>）。
     *
     * <p>天神建筑的 {@link ConstructionTier#HEAVENLY#getAngelDistance()} 是 int 上限，
     * 绝不能写成 {@code eye.add(look.scale(Integer.MAX_VALUE))}（坐标会溢出成 NaN / 无限远）。
     * 512 格已经远超正常视野能看清的距离，同时保证步进循环最多跑 2048 次。</p>
     */
    public static final int ANGEL_STEP_LIMIT = 512;

    private ConstructionActions()
    {
    }

    // ==================================================================================
    // 建造模式 · 右键方块
    // ==================================================================================

    /**
     * 贴着被点面「平面扩展」铺同种方块。
     *
     * <p>要铺哪些格与 {@link #collectBuild} 是同一套 BFS（客户端预览用的是那个纯函数，两边结果一致），
     * 这里只负责校验前置条件 + 登记作业 {@link ConstructionJob#startBuild}（扣材料 / 落方块都在作业里）。</p>
     *
     * <p>任何一格不满足条件：这一格不铺，也不再从它往外扩散（照上游写法）。材料扣不到就整轮结束。</p>
     */
    static void build(Player player, ItemStack wand, Level level, BlockPos clickedPos, Direction face)
    {
        if (!(level instanceof net.minecraft.server.level.ServerLevel))
        {
            return; // 改方块只在服务端做
        }

        BlockPos origin = clickedPos.immutable();
        BlockState template = level.getBlockState(origin);

        // 没有对应物品的方块（水 / 火 / 农田…）没法扣材料，也没法当成「材料」使用
        ItemStack wanted = new ItemStack(template.getBlock().asItem());
        if (template.isAir() || wanted.isEmpty())
        {
            player.displayClientMessage(
                    Component.translatable("message.godofthings.god_construction.no_block"), true);
            return;
        }

        BlockPos start = origin.relative(face).immutable();
        // 允许当「支撑面」的方块集合只算一次，BFS 里就是 O(1) 的 contains
        Set<Block> accepted = similarBlocks(wand, template);
        ConstructionJob.startBuild(player, wand, template, accepted, start, face);
    }

    /**
     * 只算不做的建造：返回这一次右键<b>将要铺下去</b>的坐标（顺序 = BFS 出队顺序）。
     *
     * <p>每一格必须满足：</p>
     * <ol>
     *   <li>它的「背面」（{@code pos.relative(face.getOpposite())}）还是被点的那种方块
     *       —— 开了相似方块开关时放宽成「同种或相似」（见 {@link #similarBlocks}）；</li>
     *   <li>它自己现在是空气或可替换方块（草 / 雪 / 水之类）；</li>
     *   <li>复制朝向属性后 {@code canSurvive} 仍然成立，且没有被保护插件禁止交互。</li>
     * </ol>
     *
     * <p><b>不落方块、不扣材料</b>，所以客户端可以直接调它来画预览线框（预览只取前 {@code max} 个，
     * 不然天神建筑要画几十万条线）。</p>
     *
     * @param max 最多返回多少个坐标（预览传 256；要「全部」就传 {@code Integer.MAX_VALUE}）
     */
    public static List<BlockPos> collectBuild(Level level, Player player, ItemStack wand, BlockPos clickedPos,
            Direction face, int max)
    {
        BlockPos origin = clickedPos.immutable();
        BlockState template = level.getBlockState(origin);

        // 没有对应物品的方块不可能铺（和 build 的守卫条件保持一致）
        if (template.isAir() || new ItemStack(template.getBlock().asItem()).isEmpty() || max <= 0)
        {
            return List.of();
        }

        // 允许当「支撑面」的方块集合只算一次，BFS 里就是 O(1) 的 contains
        Set<Block> accepted = similarBlocks(wand, template);
        // 候选格的「背面」方向：指回被点击的方块本体
        Direction backToBlock = face.getOpposite();

        List<BlockPos> result = new ArrayList<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos start = origin.relative(face).immutable();
        queue.addLast(start);
        visited.add(start);

        while (!queue.isEmpty() && result.size() < max)
        {
            BlockPos pos = queue.removeFirst();

            // 1) 背后那一格必须还是被点的方块（开了相似开关则允许相似方块）
            BlockState behind = level.getBlockState(pos.relative(backToBlock));
            if (!accepted.contains(behind.getBlock()))
            {
                continue;
            }

            // 2) 目标格必须是空气或可替换方块
            if (!level.getBlockState(pos).canBeReplaced())
            {
                continue;
            }

            // 3) 以被点方块的 state 为模板，复制背面方块的朝向（朝向 / 半砖方向跟着走）
            BlockState newState = copyRotation(template, behind);
            if (!newState.canSurvive(level, pos))
            {
                continue;
            }
            if (!level.mayInteract(player, pos))
            {
                continue;
            }

            result.add(pos);
            expandPlane(queue, visited, pos, face);
        }
        return result;
    }

    /**
     * 建造时「可以当支撑面」的方块集合。
     *
     * <p>默认只有被点的那种方块；相似方块开关（数据组件 {@code godofthings:god_construction_similar}）
     * 打开时，再把<b>被点方块物品所有标签</b>里的方块物品加进来 —— 例如标签 {@code minecraft:planks}
     * 就把所有木板算成同族。以 {@code mineable} 开头的标签要跳过：那是「用什么工具挖」，
     * 不是「什么算同族」（吃了它木头会把石头也算成同族）。</p>
     */
    static Set<Block> similarBlocks(ItemStack wand, BlockState template)
    {
        Set<Block> accepted = new HashSet<>();
        accepted.add(template.getBlock());
        if (!GodConstructionItem.isSimilarEnabled(wand))
        {
            return accepted;
        }

        ItemStack sample = new ItemStack(template.getBlock().asItem());
        if (sample.isEmpty())
        {
            return accepted;
        }

        sample.getTags().forEach(tag ->
        {
            if (tag.location().getPath().startsWith("mineable"))
            {
                return; // 跳过 mineable* 标签
            }
            BuiltInRegistries.ITEM.getTag(tag).ifPresent(holders -> holders.forEach(holder ->
            {
                if (holder.value() instanceof BlockItem blockItem)
                {
                    accepted.add(blockItem.getBlock());
                }
            }));
        });
        return accepted;
    }

    // ==================================================================================
    // 建造模式 · 右键空气（天使放置）
    // ==================================================================================

    /**
     * 天使放置：用副手的方块物品，在<b>眼睛沿视线方向</b>前方放 1 个方块（不要求贴着任何东西）。
     * 副手不是方块物品就什么也不做。
     *
     * <p>两种品阶走两条路（数值见 {@link ConstructionTier}）：</p>
     * <ul>
     *   <li><b>神之建筑（{@code angelDistance = 2}）</b>：照旧 —— 眼睛沿视线前进正好 2 格、取整到方块，
     *       只有这一个候选点，与改造前逐格一致（点不到就什么都不放）。</li>
     *   <li><b>天神建筑（{@code angelDistance = int 上限}）</b>：<b>绝不能</b>算
     *       {@code eye + look * Integer.MAX_VALUE}（坐标直接溢出），改成以 {@value #ANGEL_STEP} 格步进
     *       前进，最多走 {@code min(angelDistance, }{@value #ANGEL_STEP_LIMIT}{@code ) = 512} 格，
     *       取第一个「可替换 + 可存活 + mayInteract 通过」的位置。与玩家自己身体相交的格子直接跳过，
     *       免得把方块放进自己脚下。</li>
     * </ul>
     */
    static void angel(Player player, ItemStack wand, Level level)
    {
        if (!(player.getOffhandItem().getItem() instanceof BlockItem blockItem))
        {
            return;
        }

        BlockState newState = blockItem.getBlock().defaultBlockState();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        int distance = GodConstructionItem.getTier(wand).getAngelDistance();

        BlockPos pos;
        if (distance <= ANGEL_STEP_LIMIT)
        {
            // 有限距离（神之建筑）：照旧的单点取整，行为完全不变
            pos = BlockPos.containing(eye.add(look.scale(distance))).immutable();
            if (!canAngelPlace(level, player, newState, pos))
            {
                return;
            }
        }
        else
        {
            // 无限距离（天神建筑）：0.25 格步进找第一个能放的位置，最多走 512 格
            pos = findAngelPos(level, player, newState, eye, look, ANGEL_STEP_LIMIT);
            if (pos == null)
            {
                return;
            }
        }

        if (!GodConstructionItem.takeMaterial(player, wand, new ItemStack(blockItem)))
        {
            return;
        }

        level.setBlock(pos, newState, 2);
        GodConstructionItem.playPlaceSound(level, pos);
    }

    /**
     * 沿视线步进找天使放置点（只给「int 上限」品阶用）。
     *
     * @param maxTravel 最多走多少格（调用方已经和 {@link #ANGEL_STEP_LIMIT} 取过 min）
     * @return 第一个能放的位置；一路走完都没有就返回 null
     */
    private static BlockPos findAngelPos(Level level, Player player, BlockState newState, Vec3 eye, Vec3 look,
            int maxTravel)
    {
        // 玩家自己的碰撞箱：整格落在里面的候选一律跳过（不然会把自己埋进方块里）
        AABB body = player.getBoundingBox();

        // 步进用累计位移而不是乘法，避免浮点累积误差把最后一格漏掉
        Vec3 step = look.scale(ANGEL_STEP);
        Vec3 cursor = eye;
        for (double traveled = ANGEL_STEP; traveled <= maxTravel + 1.0E-6D; traveled += ANGEL_STEP)
        {
            cursor = cursor.add(step);
            BlockPos candidate = BlockPos.containing(cursor);
            if (body.intersects(new AABB(candidate)))
            {
                continue; // 别把方块放进玩家自己身体里
            }
            if (canAngelPlace(level, player, newState, candidate))
            {
                return candidate.immutable();
            }
        }
        return null;
    }

    /** 天使放置的单格判定：可替换 + 能存活 + 允许交互 */
    private static boolean canAngelPlace(Level level, Player player, BlockState newState, BlockPos pos)
    {
        return level.getBlockState(pos).canBeReplaced()
                && newState.canSurvive(level, pos)
                && level.mayInteract(player, pos);
    }

    // ==================================================================================
    // 销毁模式 · 右键方块
    // ==================================================================================

    /**
     * 拆掉被点方块的连通块（神之建筑最多 {@link GodConstructionItem#MAX_DESTROY_BLOCKS} 格、天神建筑无限，
     * <b>不掉落任何物品</b>）。
     *
     * <p>每一格的要求（照 ActionDestruction，判定在 {@link ConstructionJob#stepDestroy} 里）：</p>
     * <ol>
     *   <li>朝玩家那一侧（{@code pos.relative(face)}）必须是可透过的 —— 原实现的
     *       {@code isBlockPermeable}，即那格是空气或没有碰撞体；所以脸只能贴着自己看得见的方块拆；</li>
     *   <li>方块类型与被点方块相同；</li>
     *   <li>没有方块实体（箱子 / 机器不碰，免得丢内容），且 {@code destroySpeed >= 0}（基岩这类跳过）；</li>
     *   <li>发 {@code BlockEvent.BreakEvent}，被领地方取消的这一格就不拆。</li>
     * </ol>
     */
    static void destroy(Player player, ItemStack wand, Level level, BlockPos clickedPos, Direction face)
    {
        
        BlockPos origin = clickedPos.immutable();
        BlockState targetState = level.getBlockState(origin);
        if (targetState.isAir())
        {
            return; // 点到空气（理论上不会）就什么也不做
        }
        ConstructionJob.startDestroy(player, wand, targetState, origin, face);
    }

    // ==================================================================================
    // 替换模式 · 右键方块
    // ==================================================================================

    /**
     * 把被点方块的连通块换成副手拿的方块（神之建筑最多 {@link GodConstructionItem#MAX_REPLACE_BLOCKS} 格、
     * 天神建筑无限）。
     *
     * <p>照 ActionReplace：副手必须是方块物品（否则提示 {@code need_offhand}）；扩散时跳过空气、
     * 没有碰撞体的方块（草 / 花 / 火把…）和已经是目标方块的格子；新方块 {@code canSurvive} 不通过就跳过这一格。
     * 每换一格消耗 1 个副手方块（背包不够去 AE 网络找），旧方块直接掉成物品（照 ReplaceSnapshot，
     * 不走战利品表）。</p>
     */
    static void replace(Player player, ItemStack wand, Level level, BlockPos clickedPos)
    {
        
        if (!(player.getOffhandItem().getItem() instanceof BlockItem blockItem))
        {
            // 副手不是方块（空手也算）：提示一下，别让玩家以为物品坏了
            player.displayClientMessage(
                    Component.translatable("message.godofthings.god_construction.need_offhand"), true);
            return;
        }

        BlockPos origin = clickedPos.immutable();
        Block targetBlock = level.getBlockState(origin).getBlock();
        ConstructionJob.startReplace(player, wand, targetBlock, blockItem.getBlock().defaultBlockState(),
                origin, new ItemStack(blockItem));
    }

    // ==================================================================================
    // 工具方法（包内共用：ConstructionJob 也要用）
    // ==================================================================================

    /** 列队一个候选格：批量操作里一律存不可变坐标，防止别处复用可变 pos 串格 */
    static void pushCandidate(Deque<BlockPos> queue, Set<BlockPos> visited, BlockPos pos)
    {
        BlockPos next = pos.immutable();
        if (visited.add(next))
        {
            queue.addLast(next);
        }
    }

    /**
     * 只在垂直于 {@code face} 的平面内扩散：4 个正交邻居 + 4 个斜角邻居
     * （沿 {@code face} 轴的分量恒为 0，也就是不会往上方 / 下方那两格跑）。
     */
    static void expandPlane(Deque<BlockPos> queue, Set<BlockPos> visited, BlockPos pos, Direction face)
    {
        for (Direction first : Direction.values())
        {
            if (first.getAxis() == face.getAxis())
            {
                continue; // 法线方向不参与（那是垂直于平面的轴）
            }
            pushCandidate(queue, visited, pos.relative(first));

            for (Direction second : Direction.values())
            {
                if (second.getAxis() == face.getAxis() || second.getAxis() == first.getAxis())
                {
                    continue; // 斜角要用平面里的另一条轴，同轴 / 法线轴都不行
                }
                pushCandidate(queue, visited, pos.relative(first).relative(second));
            }
        }
    }

    /**
     * 照 StickUtil#isBlockPermeable：该格是空气，或者碰撞体为空 = 视线能穿过去。
     * 用于销毁模式「只能拆自己看得见的那一面」。
     */
    static boolean isBlockPermeable(Level level, BlockPos pos)
    {
        return level.isEmptyBlock(pos) || level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /**
     * 以 {@code template}（被点方块的 state）为底，复制 {@code from}（背面方块）的朝向属性值。
     *
     * <p>半砖单独处理：{@code SlabType.DOUBLE} 不复制（否则会刷出双半砖），统一落成下半砖。</p>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static BlockState copyRotation(BlockState template, BlockState from)
    {
        BlockState result = template;
        for (Property property : ROTATION_PROPERTIES)
        {
            if (from.hasProperty(property) && result.hasProperty(property))
            {
                result = result.setValue(property, from.getValue(property));
            }
        }

        if (from.hasProperty(BlockStateProperties.SLAB_TYPE) && result.hasProperty(BlockStateProperties.SLAB_TYPE))
        {
            SlabType slabType = from.getValue(BlockStateProperties.SLAB_TYPE);
            result = result.setValue(BlockStateProperties.SLAB_TYPE,
                    slabType == SlabType.DOUBLE ? SlabType.BOTTOM : slabType);
        }
        return result;
    }
}
