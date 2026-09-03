package com.godofthings.block;

import com.godofthings.block.entity.CreatureAnnihilationBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CreatureAnnihilationBlockEntity be)
        {
            be.toggleEnabled();
            player.displayClientMessage(Component.translatable(
                    be.isEnabled() ? "message.godofthings.toggle_on" : "message.godofthings.toggle_off"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
