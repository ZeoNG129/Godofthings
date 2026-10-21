package com.godofthings.compass.natures.sorting;

import com.godofthings.compass.natures.client.BiomeDisplay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.biome.Biome;

/**
 * 按群系标签（{@code is_*} 系列，如 is_forest / is_mountain）排序（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 */
public class TagsSorting implements ISorting<String>
{
    private static final Minecraft MC = Minecraft.getInstance();

    @Override
    public int compare(Biome biome1, Biome biome2)
    {
        return getValue(biome1).compareTo(getValue(biome2));
    }

    @Override
    public String getValue(Biome biome)
    {
        if (MC.level != null)
        {
            return BiomeDisplay.getBiomeTags(MC.level, biome);
        }
        return "";
    }

    @Override
    public ISorting<?> next()
    {
        return new DimensionSorting();
    }

    @Override
    public String getLocalizedName()
    {
        return I18n.get("godofthings.natures_compass.tags");
    }
}
