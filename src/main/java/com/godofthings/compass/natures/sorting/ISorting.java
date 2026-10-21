package com.godofthings.compass.natures.sorting;

import java.util.Comparator;

import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 群系列表的一种排序方式（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>{@link #next()} 串成环：名称 → 来源 → 标签 → 维度 → 降水量 → 温度 → 名称 ……</p>
 */
@OnlyIn(Dist.CLIENT)
public interface ISorting<T> extends Comparator<Biome>
{
    @Override
    int compare(Biome biome1, Biome biome2);

    /** 这个群系在该排序维度上的值（列表里显示的就是它） */
    T getValue(Biome biome);

    /** 下一种排序方式（环形） */
    ISorting<?> next();

    /** 排序方式的名字（语言键 {@code godofthings.natures_compass.*}） */
    String getLocalizedName();
}
