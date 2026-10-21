package com.godofthings.compass.explorers.sorting;

import java.util.Comparator;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 列表排序维度（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>{@link #next()} 串成一个环：名称 → 来源 → 维度 → 组 → 名称，点「排序方式」按钮就转一格。</p>
 */
@OnlyIn(Dist.CLIENT)
public interface ISorting extends Comparator<ResourceLocation>
{
    @Override
    int compare(ResourceLocation key1, ResourceLocation key2);

    Object getValue(ResourceLocation key);

    ISorting next();

    String getLocalizedName();
}
