package com.godofthings.block;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodResourceBlockEntity;
import com.godofthings.block.entity.GodResourceVariant;
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

/**
 * 神之资源系列方块（v5.13.0：矿物 / 作物 / 方块三台共用本类，构造时传入 {@link GodResourceVariant}）。
 * 三台共用同一个方块实体类型 {@link Godofthings#GOD_RESOURCE_BE}（与神之附魔/天神附魔共用 BE 类型同一做法）。
 */
public class GodResourceBlock extends BaseEntityBlock
{
    /**
     * 三台共用一个 codec：本方块无状态属性，codec 仅用于方块状态序列化框架；
     * 反序列化得到的实例来自注册表里的三个注册对象之一，其变体字段由注册时的工厂闭包决定，与 codec 无关。
     */
    public static final MapCodec<GodResourceBlock> CODEC =
            simpleCodec(props -> new GodResourceBlock(GodResourceVariant.ORE, props));

    private final GodResourceVariant variant;

    public GodResourceBlock(GodResourceVariant variant, net.minecraft.world.level.block.state.BlockBehaviour.Properties properties)
    {
        super(properties);
        this.variant = variant;
    }

    public GodResourceVariant getVariant()
    {
        return variant;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec()
    {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new GodResourceBlockEntity(variant, pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return level.isClientSide ? null
                : createTickerHelper(type, Godofthings.GOD_RESOURCE_BE.get(), GodResourceBlockEntity::tick);
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
}
