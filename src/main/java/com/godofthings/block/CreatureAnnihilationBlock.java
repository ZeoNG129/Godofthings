package com.godofthings.block;

import com.godofthings.block.entity.CreatureAnnihilationBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 生物覆灭：放下后以自身为中心 512 格内不再有生物自然生成。
 */
public class CreatureAnnihilationBlock extends BaseEntityBlock
{
    public static final MapCodec<CreatureAnnihilationBlock> CODEC = simpleCodec(CreatureAnnihilationBlock::new);
    /** 是否抑制（true=绿色材质，false=红色材质）。 */
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");

    public CreatureAnnihilationBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ENABLED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(ENABLED);
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
            level.setBlock(pos, state.setValue(ENABLED, be.isEnabled()), 3);
            player.displayClientMessage(Component.translatable(
                    be.isEnabled() ? "message.godofthings.toggle_on" : "message.godofthings.toggle_off"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
