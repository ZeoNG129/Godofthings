package com.godofthings.measurement;

// 基于 Measurements（作者 Mrbysco，MIT License）移植并改名「神之测量」：
// https://github.com/Mrbysco/measurements
// 测量线框的颜色选项（RANDOM = 每个测量框随机取一种染色）。

import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public enum MeasureLineColor
{
    RANDOM(null),
    WHITE(DyeColor.WHITE),
    ORANGE(DyeColor.ORANGE),
    MAGENTA(DyeColor.MAGENTA),
    LIGHT_BLUE(DyeColor.LIGHT_BLUE),
    YELLOW(DyeColor.YELLOW),
    LIME(DyeColor.LIME),
    PINK(DyeColor.PINK),
    GRAY(DyeColor.GRAY),
    LIGHT_GRAY(DyeColor.LIGHT_GRAY),
    CYAN(DyeColor.CYAN),
    PURPLE(DyeColor.PURPLE),
    BLUE(DyeColor.BLUE),
    BROWN(DyeColor.BROWN),
    GREEN(DyeColor.GREEN),
    RED(DyeColor.RED),
    BLACK(DyeColor.BLACK);

    private final DyeColor color;

    MeasureLineColor(@Nullable DyeColor color)
    {
        this.color = color;
    }

    public DyeColor getColor(Random random)
    {
        if (this.color == null)
        {
            int pick = random.nextInt(DyeColor.values().length);
            return DyeColor.values()[pick];
        }
        return color;
    }
}
