package com.godofthings.building;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** 素板 / 框板（原样移植自 AE2LT）：纯装饰方块，连接贴图完全由烘焙模型处理。 */
public final class PigmeeBuildingPanelBlock extends Block {
    private final DyeColor color;
    private final boolean framed;

    public PigmeeBuildingPanelBlock(DyeColor color, boolean framed) {
        super(Properties.of().mapColor(color.getMapColor()).strength(1.5F, 6.0F).sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
        this.color = color;
        this.framed = framed;
    }

    public DyeColor color() {
        return color;
    }

    public boolean framed() {
        return framed;
    }
}
