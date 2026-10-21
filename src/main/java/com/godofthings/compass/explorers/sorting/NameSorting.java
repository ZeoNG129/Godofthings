package com.godofthings.compass.explorers.sorting;

import com.godofthings.compass.explorers.client.StructureDisplay;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 按结构名排序（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 */
@OnlyIn(Dist.CLIENT)
public class NameSorting implements ISorting
{
    @Override
    public int compare(ResourceLocation key1, ResourceLocation key2)
    {
        return StructureDisplay.getPrettyStructureName(key1).compareTo(StructureDisplay.getPrettyStructureName(key2));
    }

    @Override
    public Object getValue(ResourceLocation key)
    {
        return StructureDisplay.getPrettyStructureName(key);
    }

    @Override
    public ISorting next()
    {
        return new SourceSorting();
    }

    @Override
    public String getLocalizedName()
    {
        return I18n.get("godofthings.explorers_compass.name");
    }
}
