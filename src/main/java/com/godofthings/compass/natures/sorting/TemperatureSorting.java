package com.godofthings.compass.natures.sorting;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 按温度排序（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 */
@OnlyIn(Dist.CLIENT)
public class TemperatureSorting implements ISorting<Float>
{
    @Override
    public int compare(Biome biome1, Biome biome2)
    {
        return getValue(biome1).compareTo(getValue(biome2));
    }

    @Override
    public Float getValue(Biome biome)
    {
        return biome.getBaseTemperature();
    }

    @Override
    public ISorting<?> next()
    {
        return new NameSorting();
    }

    @Override
    public String getLocalizedName()
    {
        return I18n.get("godofthings.natures_compass.temperature");
    }
}
