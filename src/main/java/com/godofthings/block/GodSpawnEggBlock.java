package com.godofthings.block;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodSpawnEggBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class GodSpawnEggBlock extends BaseEntityBlock
{
    public static final MapCodec<GodSpawnEggBlock> CODEC = simpleCodec(GodSpawnEggBlock::new);

    public GodSpawnEggBlock(Properties properties)
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
        return new GodSpawnEggBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return level.isClientSide ? null
                : createTickerHelper(type, Godofthings.GOD_SPAWN_EGG_BE.get(), GodSpawnEggBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.openMenu(state.getMenuProvider(level, pos), buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos)
    {
        return level.getBlockEntity(pos) instanceof MenuProvider mp ? mp : null;
    }

    // 打掉时物品一并消失，不掉落
    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                               net.minecraft.world.level.block.state.BlockState state,
                               @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer,
                               net.minecraft.world.item.ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);
        // 神之共鸣（v5.15.4）：记录放置者为主人（共鸣判定：主人在线 + 穿齐全套 + 对应共鸣开关开）
        if (placer instanceof net.minecraft.server.level.ServerPlayer player
                && level.getBlockEntity(pos) instanceof GodSpawnEggBlockEntity be)
        {
            be.setOwner(player.getUUID());
        }
    }

}
