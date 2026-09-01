package com.godofthings.block;

import com.godofthings.block.entity.CreatureAnnihilationBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生物覆灭：放下后以自身为中心 512 格内不再有生物自然生成。
 */
public class CreatureAnnihilationBlock extends BaseEntityBlock
{
    public static final MapCodec<CreatureAnnihilationBlock> CODEC = simpleCodec(CreatureAnnihilationBlock::new);

    public CreatureAnnihilationBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec()
    {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new CreatureAnnihilationBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }
}
