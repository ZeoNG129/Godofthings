package com.godofthings.compass.explorers.client;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.godofthings.compass.explorers.config.ConfigHandler;
import org.apache.commons.lang3.text.WordUtils;

import net.minecraft.Util;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

/**
 * 结构 / 维度显示名的美化（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>上游这几段写在 {@code StructureUtils} 里（那个类服务端搜索线程也会用）。本项目的约定是
 * 「import {@code net.minecraft.client.*} 的类不能被服务端路径引用」，所以这里把查语言文件的部分
 * 单独拆成一个纯客户端类，{@code util.StructureUtils} 只留服务端查询。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class StructureDisplay
{
    private StructureDisplay()
    {
    }

    /** 结构的显示名：先查语言文件，查不到就把 id 转成「首字母大写 + 下划线换空格」 */
    public static String getPrettyStructureName(ResourceLocation key)
    {
        if (key == null)
        {
            return "";
        }
        String name = key.toString();
        if (ConfigHandler.CLIENT.translateStructureNames.get())
        {
            name = I18n.get(Util.makeDescriptionId("structure", key));
        }
        if (name.equals(Util.makeDescriptionId("structure", key))
                || !ConfigHandler.CLIENT.translateStructureNames.get())
        {
            name = key.toString();
            if (name.contains(":"))
            {
                name = name.substring(name.indexOf(":") + 1);
            }
            name = WordUtils.capitalize(name.replace('_', ' '));
        }
        return name;
    }

    /** 结构来源（模组显示名；minecraft 固定写 Minecraft） */
    public static String getPrettyStructureSource(ResourceLocation key)
    {
        if (key == null)
        {
            return "";
        }
        String registryEntry = key.toString();
        String modid = registryEntry.substring(0, registryEntry.indexOf(":"));
        if (modid.equals("minecraft"))
        {
            return "Minecraft";
        }
        Optional<? extends ModContainer> sourceContainer = ModList.get().getModContainerById(modid);
        if (sourceContainer.isPresent())
        {
            return sourceContainer.get().getModInfo().getDisplayName();
        }
        return modid;
    }

    /** 维度键列表 → 人类可读的「主世界, 下界」 */
    public static String dimensionKeysToString(List<ResourceLocation> dimensions)
    {
        Set<String> dimensionNames = new HashSet<>();
        dimensions.forEach(key -> dimensionNames.add(getDimensionName(key)));
        return String.join(", ", dimensionNames);
    }

    private static String getDimensionName(ResourceLocation dimensionKey)
    {
        String name = I18n.get(Util.makeDescriptionId("dimension", dimensionKey));
        if (name.equals(Util.makeDescriptionId("dimension", dimensionKey)))
        {
            name = dimensionKey.toString();
            if (name.contains(":"))
            {
                name = name.substring(name.indexOf(":") + 1);
            }
            name = WordUtils.capitalize(name.replace('_', ' '));
        }
        return name;
    }
}
