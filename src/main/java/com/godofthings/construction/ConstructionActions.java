package com.godofthings.construction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * 神之建筑的四种批量动作（建造 / 天使放置 / 销毁 / 替换），全部照 ConstructionSticks 的
 * ActionConstruction / ActionAngel / ActionDestruction / ActionReplace 精简而来。
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

    private ConstructionActions()
    {
    }

    // ==================================================================================
    // 建造模式 · 右键方块
    // ==================================================================================

    /**
     * 贴着被点面「平面扩展」铺同种方块。
     *
     * <p>每一格必须满足：</p>
     * <ol>
     *   <li>它的「背面」（{@code pos.relative(face.getOpposite())}）还是被点的那种方块
     *       ——也就是只铺在这片已有表面上，不会悬空长出去；</li>
     *   <li>它自己现在是空气或可替换方块（草 / 雪 / 水之类）；</li>
     *   <li>复制朝向属性后 {@code canSurvive} 仍然成立，且没有被保护插件禁止交互。</li>
     * </ol>
     * <p>任何一条不满足：这一格不铺，也不再从它往外扩散（照上游写法）。材料扣不到就整轮结束。</p>
     */
    static void build(Player player, ItemStack wand, Level level, BlockPos clickedPos, Direction face)
    {
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

        // 候选格的「背面」方向：指回被点击的方块本体
        Direction backToBlock = face.getOpposite();

        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        BlockPos start = origin.relative(face).immutable();
        queue.addLast(start);
        visited.add(start);

        int placed = 0;
        boolean soundPlayed = false;
        while (!queue.isEmpty() && placed < GodConstructionItem.MAX_BUILD_BLOCKS)
        {
            BlockPos pos = queue.removeFirst();

            // 1) 背后那一格必须还是被点的方块：只能贴着已有同种表面铺
            BlockState behind = level.getBlockState(pos.relative(backToBlock));
            if (behind.getBlock() != template.getBlock())
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

            // 材料用完就整轮停下（不是跳过这一格，免得铺出参差不齐的一片）
            if (!GodConstructionItem.takeMaterial(player, wand, wanted))
            {
                break;
            }

            level.setBlock(pos, newState, 2);
            placed++;
            if (!soundPlayed)
            {
                GodConstructionItem.playPlaceSound(level, pos);
                soundPlayed = true;
            }
            expandPlane(queue, visited, pos, face);
        }
    }

    // ==================================================================================
    // 建造模式 · 右键空气（天使放置）
    // ==================================================================================

    /**
     * 天使放置：用副手的方块物品，在「眼睛沿视线方向前进 {@link GodConstructionItem#ANGEL_DISTANCE} 格」
     * 取整得到的那一格放 1 个方块（不要求贴着任何东西）。副手不是方块物品就什么也不做。
     */
    static void angel(Player player, ItemStack wand, Level level)
    {
        if (!(player.getOffhandItem().getItem() instanceof BlockItem blockItem))
        {
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 target = eye.add(player.getLookAngle().scale(GodConstructionItem.ANGEL_DISTANCE));
        BlockPos pos = BlockPos.containing(target).immutable(); // 取整（向下取整）到方块坐标

        BlockState newState = blockItem.getBlock().defaultBlockState();
        if (!level.getBlockState(pos).canBeReplaced() || !newState.canSurvive(level, pos))
        {
            return;
        }
        if (!level.mayInteract(player, pos))
        {
            return;
        }
        if (!GodConstructionItem.takeMaterial(player, wand, new ItemStack(blockItem)))
        {
            return;
        }

        level.setBlock(pos, newState, 2);
        GodConstructionItem.playPlaceSound(level, pos);
    }

    // ==================================================================================
    // 销毁模式 · 右键方块
    // ==================================================================================

    /**
     * 拆掉被点方块的连通块（最多 {@link GodConstructionItem#MAX_DESTROY_BLOCKS} 格，<b>不掉落任何物品</b>）。
     *
     * <p>每一格的要求（照 ActionDestruction）：</p>
     * <ol>
     *   <li>朝玩家那一侧（{@code pos.relative(face)}）必须是可透过的 —— 原实现的
     *       {@code isBlockPermeable}，即那格是空气或没有碰撞体；所以脸只能贴着自己看得见的方块拆；</li>
     *   <li>方块类型与被点方块相同；</li>
     *   <li>没有方块实体（箱子 / 机器不碰，免得丢内容），且 {@code destroySpeed >= 0}（基岩这类跳过）；</li>
     *   <li>发 {@link BlockEvent.BreakEvent}，被领地方取消的这一格就不拆。</li>
     * </ol>
     */
    static void destroy(Player player, Level level, BlockPos clickedPos, Direction face)
    {
        BlockPos origin = clickedPos.immutable();
        BlockState targetState = level.getBlockState(origin);
        if (targetState.isAir())
        {
            return; // 点到空气（理论上不会）就什么也不做
        }
        Block targetBlock = targetState.getBlock();

        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.addLast(origin);
        visited.add(origin);

        int removed = 0;
        while (!queue.isEmpty() && removed < GodConstructionItem.MAX_DESTROY_BLOCKS)
        {
            BlockPos pos = queue.removeFirst();

            // 1) 面向玩家的那一侧必须能透过（照 StickUtil#isBlockPermeable）
            if (!isBlockPermeable(level, pos.relative(face)))
            {
                continue;
            }

            // 2) 只拆被点的那种方块
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != targetBlock)
            {
                continue;
            }

            // 3) 带方块实体的、不可破坏的、被保护插件禁止交互的都跳过
            if (level.getBlockEntity(pos) != null)
            {
                continue;
            }
            if (state.getDestroySpeed(level, pos) < 0.0F)
            {
                continue;
            }
            if (!level.mayInteract(player, pos))
            {
                continue;
            }

            // 4) 领地 / 保护插件有最后一次否决权
            BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, state, player);
            if (NeoForge.EVENT_BUS.post(breakEvent).isCanceled())
            {
                continue;
            }

            level.removeBlock(pos, false); // 不掉落
            removed++;

            for (Direction direction : Direction.values())
            {
                pushCandidate(queue, visited, pos.relative(direction));
            }
        }
    }

    // ==================================================================================
    // 替换模式 · 右键方块
    // ==================================================================================

    /**
     * 把被点方块的连通块换成副手拿的方块（最多 {@link GodConstructionItem#MAX_REPLACE_BLOCKS} 格）。
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
        BlockState newState = blockItem.getBlock().defaultBlockState();
        Block targetBlock = level.getBlockState(origin).getBlock();

        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.addLast(origin);
        visited.add(origin);

        int replaced = 0;
        while (!queue.isEmpty() && replaced < GodConstructionItem.MAX_REPLACE_BLOCKS)
        {
            BlockPos pos = queue.removeFirst();
            BlockState oldState = level.getBlockState(pos);

            // 跳过空气、没有碰撞体的方块、已经是目标方块的格子（照 ActionReplace）
            if (oldState.isAir() || !oldState.blocksMotion() || oldState.getBlock() == newState.getBlock())
            {
                continue;
            }
            // 只换被点的那种方块
            if (oldState.getBlock() != targetBlock)
            {
                continue;
            }
            if (!newState.canSurvive(level, pos))
            {
                continue;
            }
            if (level.getBlockEntity(pos) != null || !level.mayInteract(player, pos))
            {
                continue;
            }
            // 替换同样会移除旧方块，按保护插件惯例先问一声
            BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, pos, oldState, player);
            if (NeoForge.EVENT_BUS.post(breakEvent).isCanceled())
            {
                continue;
            }

            // 材料用完就整轮停下
            if (!GodConstructionItem.takeMaterial(player, wand, new ItemStack(blockItem)))
            {
                break;
            }

            // 旧方块掉成物品（照 ReplaceSnapshot：只用方块物品，不走战利品表）
            ItemStack oldItem = new ItemStack(oldState.getBlock().asItem());
            if (!oldItem.isEmpty())
            {
                Block.popResource(level, pos, oldItem);
            }

            level.setBlock(pos, newState, 2);
            replaced++;

            for (Direction direction : Direction.values())
            {
                pushCandidate(queue, visited, pos.relative(direction));
            }
        }
    }

    // ==================================================================================
    // 工具方法
    // ==================================================================================

    /** 列队一个候选格：批量操作里一律存不可变坐标，防止别处复用可变 pos 串格 */
    private static void pushCandidate(Deque<BlockPos> queue, Set<BlockPos> visited, BlockPos pos)
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
    private static void expandPlane(Deque<BlockPos> queue, Set<BlockPos> visited, BlockPos pos, Direction face)
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
    private static boolean isBlockPermeable(Level level, BlockPos pos)
    {
        return level.isEmptyBlock(pos) || level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /**
     * 以 {@code template}（被点方块的 state）为底，复制 {@code from}（背面方块）的朝向属性值。
     *
     * <p>半砖单独处理：{@code SlabType.DOUBLE} 不复制（否则会刷出双半砖），统一落成下半砖。</p>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copyRotation(BlockState template, BlockState from)
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
