package com.godofthings.aalink;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/** 无线连接器方块（移植自 AALink 的 WirelessConnectorBlock）。 */
public class AaLinkConnectorBlock extends Block implements EntityBlock
{
    /** 1.21 起 Block 子类必须提供 codec（方块状态序列化用；本方块无额外状态，simpleCodec 即可）。 */
    public static final MapCodec<AaLinkConnectorBlock> CODEC = simpleCodec(AaLinkConnectorBlock::new);

    public AaLinkConnectorBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec()
    {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new AaLinkConnectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type)
    {
        if (type == AaLinkRegistry.WIRELESS_CONNECTOR_BE.get())
        {
            return (lvl, pos, st, be) -> AaLinkConnectorBlockEntity.tick(lvl, pos, st,
                    (AaLinkConnectorBlockEntity) be);
        }
        return null;
    }
}
