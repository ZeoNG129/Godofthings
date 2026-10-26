package com.godofthings.elevator;

import com.godofthings.elevator.block.ElevatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

/**
 * 本模组实际使用的电梯方块（NeoForge 侧实现）。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.neoforge.ElevatorBlock}（MIT），逐行对照。</p>
 *
 * <p>[本地改动] 上游在此类里重写的 {@code getFriction(state,level,pos,entity)}、
 * {@code getSoundType(state,level,pos,entity)}、{@code getAppearance}、{@code hidesNeighborFace}、
 * {@code supportsExternalFaceHiding}、{@code hasDynamicLightEmission}、{@code getLightEmission}、
 * {@code canConnectRedstone}、{@code shouldCheckWeakPower}、{@code collisionExtendsVertically}
 * 在本项目的 NeoForge 21.1.249 中<b>都不存在</b>，故未移植（详见 {@link ElevatorBlockBase} 类注释）。</p>
 */
public class ElevatorBlock extends ElevatorBlockBase
{
    private final com.mojang.serialization.MapCodec<ElevatorBlock> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance
                    .group(DyeColor.CODEC.fieldOf("color").forGetter(ElevatorBlockBase::getColor))
                    .apply(instance, ElevatorBlock::new));

    public ElevatorBlock(DyeColor color)
    {
        super(color);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.HorizontalDirectionalBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected void openMenu(Player player, ElevatorBlockEntity tile, BlockPos pos)
    {
        player.openMenu(tile, pos);
    }
}
