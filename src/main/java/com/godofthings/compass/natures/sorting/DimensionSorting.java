package com.godofthings.compass.natures.sorting;

import java.util.Optional;

import com.godofthings.compass.natures.NaturesCompassData;
import com.godofthings.compass.natures.client.BiomeDisplay;
import com.godofthings.compass.natures.util.BiomeUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 按「哪些维度会生成这个群系」排序（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>维度列表来自服务端推下来的 {@link NaturesCompassData#dimensionKeysForAllowedBiomeKeys}。</p>
 */
@OnlyIn(Dist.CLIENT)
public class DimensionSorting implements ISorting<String>
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
            Optional<ResourceLocation> optionalBiomeKey = BiomeUtils.getKeyForBiome(MC.level, biome);
            if (optionalBiomeKey.isPresent())
            {
                return BiomeDisplay.dimensionKeysToString(
                        NaturesCompassData.dimensionKeysForAllowedBiomeKeys.get(optionalBiomeKey.get()));
            }
        }
        return "";
    }

    @Override
    public ISorting<?> next()
    {
        return new RainfallSorting();
    }

    @Override
    public String getLocalizedName()
    {
        return I18n.get("godofthings.natures_compass.dimension");
    }
}
