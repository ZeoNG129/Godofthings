package com.godofthings.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 流体封存格：矿机挖掉水 / 岩浆之后留下的占位方块。
 *
 * <h2>为什么需要它（v7.20.1）</h2>
 * 挖掉一格水之后想让旁边的水不灌回来，只靠 {@code Level#setBlock} 的 flags 是<b>做不到</b>的：
 * 实测 flags = 0 / 2 / 3 三者在 11 tick 内都会让水位自己涨回来（而且直接变回水源）。
 * 源码里的判定是 {@code FlowingFluid#canHoldFluid}：
 * <pre>
 * return !state.is(NETHER_PORTAL) &amp;&amp; ... ? !state.blocksMotion() : false;
 * </pre>
 * 也就是说「能不能被流体占据」只看 {@code blocksMotion()}——空气与所有不挡移动的方块都返回 true，
 * 流体因此总能流进去。想真正挡住，就必须留一个 {@code blocksMotion() == true} 的方块。
 *
 * <h2>怎么做到「挡流体但不挡人」</h2>
 * <ul>
 *   <li>{@link BlockBehaviour.Properties#forceSolidOn()}：让 {@code blocksMotion()} 恒为 true
 *       （{@code legacySolid} 直接取这个开关），流体据此判定此格不可占据 —— 于是水 / 岩浆流不进来。</li>
 *   <li>{@link #getCollisionShape} 与 {@link #getShape} 都返回空：玩家、生物、物品照常穿过，
 *       也不会出现选中框。</li>
 *   <li>{@link #getRenderShape} 为 {@code INVISIBLE}：完全不渲染。</li>
 *   <li>{@link #isSuffocating} / {@link #isViewBlocking} 关掉：不会让人窒息、不会挡住视线。</li>
 * </ul>
 */
public class FluidSealBlock extends Block
{
    public FluidSealBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
    {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos)
    {
        return 0;
    }

    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context)
    {
        return false;
    }
}
