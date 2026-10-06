package com.godofthings.measurement;

// 基于 Measurements（作者 Mrbysco，MIT License）移植并改名「神之测量」：
// https://github.com/Mrbysco/measurements
// 三轴长度数字的颜色选项：RANDOM 随机；XYZRGB 按轴配色（X 红 / Y 绿 / Z 蓝）。

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public enum MeasureTextColor
{
    RANDOM(null),
    XYZRGB(null),
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

    MeasureTextColor(@Nullable DyeColor color)
    {
        this.color = color;
    }

    /**
     * Gets the color
     *
     * @param random the RandomSource used to generate a random color if RANDOM is selected
     * @param axis   the axis used when XYZRGB is used
     * @return the color
     */
    public DyeColor getColor(Random random, @Nullable Direction.Axis axis)
    {
        if (this == RANDOM)
        {
            int pick = random.nextInt(DyeColor.values().length);
            return DyeColor.values()[pick];
        }
        else if (this == XYZRGB)
        {
            if (axis == null)
            {
                return DyeColor.YELLOW;
            }
            switch (axis)
            {
                default -> {
                    return DyeColor.RED;
                }
                case Y -> {
                    return DyeColor.GREEN;
                }
                case Z -> {
                    return DyeColor.BLUE;
                }
            }
        }
        else
        {
            return color;
        }
    }
}
