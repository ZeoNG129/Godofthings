package com.godofthings.block;

import com.godofthings.block.entity.GodEnchantBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class GodEnchantBlock extends BaseEntityBlock
{
    public GodEnchantBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec()
    {
        return simpleCodec(GodEnchantBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new GodEnchantBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
    {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.openMenu(state.getMenuProvider(level, pos), buf ->
            {
                buf.writeBlockPos(pos);
                buf.writeBoolean(isHeavenly());
            });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 是否为升级版天神附魔（选择附魔默认最高等级） */
    public boolean isHeavenly()
    {
        return false;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos)
    {
        return level.getBlockEntity(pos) instanceof MenuProvider mp ? mp : null;
    }
}
