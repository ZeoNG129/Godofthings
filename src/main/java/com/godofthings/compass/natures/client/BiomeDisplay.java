package com.godofthings.compass.natures.client;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.text.WordUtils;

import com.godofthings.compass.natures.config.ConfigHandler;
import com.godofthings.compass.natures.util.BiomeUtils;

import net.minecraft.Util;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

/**
 * 把群系翻译成「人话」的客户端工具（移植自 Natures Compass 3.4.0 的 {@code BiomeUtils}，
 * 作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>上游把这些方法和注册表查询混在一个类里；这里把它们单独拆出来，因为只有它们需要
 * {@code net.minecraft.client.*} 与 {@code I18n}。服务端路径引用的是 {@link BiomeUtils}，
 * 所以专用服务端不会因为本类而加载客户端 API。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class BiomeDisplay
{
    private BiomeDisplay()
    {
    }

    /**
     * 群系的「标签 / 类别」（从 {@code is_*} 系列标签反推，如 {@code is_forest} → 森林）。
     *
     * <p>{@code is_overworld} 一律忽略：部分主世界群系有、部分没有，显示出来只会干扰。
     * 没找到任何类别时返回「无」（{@code godofthings.natures.none}）。</p>
     */
    public static String getBiomeTags(Level level, Biome biome)
    {
        List<String> tagPathsToIgnore = List.of("is_overworld");
        Set<String> biomeCategories = new TreeSet<>();
        Optional<? extends Registry<Biome>> optionalRegistry = BiomeUtils.getBiomeRegistry(level);
        if (optionalRegistry.isPresent())
        {
            Registry<Biome> biomeRegistry = optionalRegistry.get();
            Optional<? extends net.minecraft.resources.ResourceKey<Biome>> optionalKey = biomeRegistry.getResourceKey(biome);
            if (optionalKey.isPresent() && biomeRegistry.getHolder(optionalKey.get()).isPresent())
            {
                Holder<Biome> biomeHolder = biomeRegistry.getHolder(optionalKey.get()).get();
                // 从标签反推类别是上游的「hacky」做法，但确实是唯一通用手段
                List<TagKey<Biome>> categoryTags = biomeHolder.tags()
                        .filter(tag -> tag.location().getPath().startsWith("is_"))
                        .collect(Collectors.toList());
                for (TagKey<Biome> tag : categoryTags)
                {
                    if (tagPathsToIgnore.contains(tag.location().getPath()))
                    {
                        continue;
                    }
                    String fixedPath = tag.location().getPath().replaceFirst("is_", "");
                    if (fixedPath.contains("/"))
                    {
                        fixedPath = fixedPath.substring(0, fixedPath.indexOf("/"));
                    }
                    String biomeKey = Util.makeDescriptionId("biome",
                            ResourceLocation.fromNamespaceAndPath(tag.location().getNamespace(), fixedPath));
                    String translatedBiomeKey = I18n.get(biomeKey);
                    if (!biomeKey.equals(translatedBiomeKey))
                    {
                        return translatedBiomeKey;
                    }
                    String categoryKey = Util.makeDescriptionId("category",
                            ResourceLocation.fromNamespaceAndPath(tag.location().getNamespace(), fixedPath));
                    String translatedCategoryKey = I18n.get(categoryKey);
                    if (!categoryKey.equals(translatedCategoryKey))
                    {
                        return translatedCategoryKey;
                    }
                    biomeCategories.add(WordUtils.capitalize(fixedPath.replace('_', ' ')));
                }
            }
        }
        if (biomeCategories.isEmpty())
        {
            biomeCategories.add(I18n.get("godofthings.natures.none"));
        }
        return String.join(", ", biomeCategories);
    }

    /** 群系的显示名（配置项 {@code fixBiomeNames} 打开时给 CamelCase 补空格） */
    public static String getBiomeNameForDisplay(Level level, ResourceLocation biome)
    {
        Optional<Biome> optionalBiome = BiomeUtils.getBiomeForKey(level, biome);
        if (optionalBiome.isPresent())
        {
            return getBiomeNameForDisplay(level, optionalBiome.get());
        }
        return "";
    }

    public static String getBiomeNameForDisplay(Level level, Biome biome)
    {
        if (biome == null)
        {
            return "";
        }
        if (ConfigHandler.CLIENT.fixBiomeNames.get())
        {
            String original = getBiomeName(level, biome);
            StringBuilder fixed = new StringBuilder();
            char pre = ' ';
            for (int i = 0; i < original.length(); i++)
            {
                char c = original.charAt(i);
                if (Character.isUpperCase(c) && Character.isLowerCase(pre) && Character.isAlphabetic(pre))
                {
                    fixed.append(' ');
                }
                fixed.append(c);
                pre = c;
            }
            return fixed.toString();
        }
        return getBiomeName(level, biome);
    }

    /** 群系的本地化名字（{@code biome.minecraft.plains} 之类） */
    public static String getBiomeName(Level level, Biome biome)
    {
        Optional<ResourceLocation> optionalKey = BiomeUtils.getKeyForBiome(level, biome);
        return optionalKey.isPresent() ? I18n.get(Util.makeDescriptionId("biome", optionalKey.get())) : "";
    }

    public static String getBiomeName(Level level, ResourceLocation key)
    {
        Optional<Biome> optionalBiome = BiomeUtils.getBiomeForKey(level, key);
        if (optionalBiome.isPresent())
        {
            return getBiomeName(level, optionalBiome.get());
        }
        return "";
    }

    /** 群系来自哪个模组（{@code minecraft} 显示成 Minecraft，其它取模组显示名） */
    public static String getBiomeSource(Level level, Biome biome)
    {
        Optional<ResourceLocation> optionalKey = BiomeUtils.getKeyForBiome(level, biome);
        if (optionalKey.isEmpty())
        {
            return "";
        }
        String modid = optionalKey.get().getNamespace();
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

    private static String getDimensionName(ResourceLocation dimensionKey)
    {
        String name = I18n.get(Util.makeDescriptionId("dimension", dimensionKey));
        if (name.equals(Util.makeDescriptionId("dimension", dimensionKey)))
        {
            name = dimensionKey.toString();
            if (name.contains(":"))
            {
                name = name.substring(name.indexOf(':') + 1);
            }
            name = WordUtils.capitalize(name.replace('_', ' '));
        }
        return name;
    }

    /** 维度列表 → 「主世界, 下界」这样的人话（去重） */
    public static String dimensionKeysToString(List<ResourceLocation> dimensions)
    {
        Set<String> dimensionNames = new HashSet<>();
        dimensions.forEach(key -> dimensionNames.add(getDimensionName(key)));
        return String.join(", ", dimensionNames);
    }
}
