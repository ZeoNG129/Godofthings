package com.godofthings.compass.explorers.sorting;

import com.godofthings.compass.explorers.ExplorersCompassData;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 按「组」（结构集）排序（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 */
@OnlyIn(Dist.CLIENT)
public class GroupSorting implements ISorting
{
    @Override
    public int compare(ResourceLocation key1, ResourceLocation key2)
    {
        return ExplorersCompassData.structureKeysToTypeKeys.get(key1)
                .compareTo(ExplorersCompassData.structureKeysToTypeKeys.get(key2));
    }

    @Override
    public Object getValue(ResourceLocation key)
    {
        return ExplorersCompassData.structureKeysToTypeKeys.get(key);
    }

    @Override
    public ISorting next()
    {
        return new NameSorting();
    }

    @Override
    public String getLocalizedName()
    {
        return I18n.get("godofthings.explorers_compass.group");
    }
}
