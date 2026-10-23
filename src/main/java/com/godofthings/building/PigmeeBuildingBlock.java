package com.godofthings.building;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 猪咪建材方块（原样移植自 AE2LT）：贴图取自猪咪 fumo 的素色装饰方块。 */
public final class PigmeeBuildingBlock extends Block {
    public PigmeeBuildingBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5F, 6.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }
}
