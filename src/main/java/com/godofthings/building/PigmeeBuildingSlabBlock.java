package com.godofthings.building;

import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 台阶（原样移植自 AE2LT）：原版台阶的摆放 / 含水 / 合并逻辑，装饰由模型提供。 */
public final class PigmeeBuildingSlabBlock extends SlabBlock {
    public PigmeeBuildingSlabBlock(MapColor color) {
        super(Properties.of().mapColor(color).strength(1.5F, 6.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }
}
