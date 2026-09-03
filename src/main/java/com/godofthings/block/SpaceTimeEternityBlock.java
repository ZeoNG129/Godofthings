package com.godofthings.block;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.SpaceTimeEternityBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 时空永恒：放下后世界时间与天气永久锁定在放置时刻的状态。
 */
public class SpaceTimeEternityBlock extends BaseEntityBlock
{
    public static final MapCodec<SpaceTimeEternityBlock> CODEC = simpleCodec(SpaceTimeEternityBlock::new);
    /** 是否锁定（true=绿色材质，false=红色材质）。 */
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");

    public SpaceTimeEternityBlock(Properties properties)
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
        return new SpaceTimeEternityBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return level.isClientSide ? null
                : createTickerHelper(type, Godofthings.SPACE_TIME_ETERNITY_BE.get(), SpaceTimeEternityBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SpaceTimeEternityBlockEntity be)
        {
            be.toggleEnabled();
            level.setBlock(pos, state.setValue(ENABLED, be.isEnabled()), 3);
            player.displayClientMessage(Component.translatable(
                    be.isEnabled() ? "message.godofthings.toggle_on" : "message.godofthings.toggle_off"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
