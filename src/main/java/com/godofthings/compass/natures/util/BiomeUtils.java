package com.godofthings.compass.natures.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.godofthings.compass.natures.config.ConfigHandler;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * 群系查询（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p><b>本类只放服务端也能跑的东西</b>：注册表查询、黑名单过滤、经验消耗、维度归属、距离计算。
 * 所有需要 {@code net.minecraft.client.*} 的「把群系名字 / 标签 / 来源翻译成人话」的逻辑
 * 都拆到了 {@code client.BiomeDisplay} —— 这样服务端路径（物品 / 网络包 / 搜索 worker）引用本类时
 * 不会把客户端类拖进类加载。</p>
 */
public final class BiomeUtils
{
    private BiomeUtils()
    {
    }

    public static Optional<? extends Registry<Biome>> getBiomeRegistry(Level level)
    {
        return level.registryAccess().registry(Registries.BIOME);
    }

    public static Optional<ResourceLocation> getKeyForBiome(Level level, Biome biome)
    {
        Optional<? extends Registry<Biome>> registry = getBiomeRegistry(level);
        return registry.isPresent() ? Optional.of(registry.get().getKey(biome)) : Optional.empty();
    }

    public static Optional<Biome> getBiomeForKey(Level level, ResourceLocation key)
    {
        Optional<? extends Registry<Biome>> registry = getBiomeRegistry(level);
        return registry.isPresent() ? registry.get().getOptional(key) : Optional.empty();
    }

    /** 允许搜索的群系（去掉黑名单里的、去掉带 {@code c:hidden_from_locator_selection} 标签的） */
    public static List<ResourceLocation> getAllowedBiomeKeys(Level level)
    {
        List<ResourceLocation> biomeKeys = new ArrayList<>();
        Optional<? extends Registry<Biome>> registry = getBiomeRegistry(level);
        if (registry.isPresent())
        {
            for (Map.Entry<ResourceKey<Biome>, Biome> entry : registry.get().entrySet())
            {
                Biome biome = entry.getValue();
                if (biome != null)
                {
                    Optional<ResourceLocation> optionalBiomeKey = getKeyForBiome(level, biome);
                    if (optionalBiomeKey.isPresent()
                            && !biomeKeyIsBlacklisted(optionalBiomeKey.get())
                            && !biomeKeyIsHidden(level, optionalBiomeKey.get()))
                    {
                        biomeKeys.add(optionalBiomeKey.get());
                    }
                }
            }
        }
        return biomeKeys;
    }

    /** 搜索这个群系要扣多少经验等级（配置项 perBiomeXpLevels 优先，否则 defaultXpLevels） */
    public static int getXpLevelsForBiome(ResourceLocation biomeKey)
    {
        int xpLevels = ConfigHandler.GENERAL.defaultXpLevels.get();
        Map<String, Integer> xpLevelOverrides = parseXpLevelOverridesConfig();
        for (String biomeRegex : xpLevelOverrides.keySet())
        {
            if (biomeKey.toString().matches(convertToRegex(biomeRegex)))
            {
                xpLevels = xpLevelOverrides.get(biomeRegex);
                if (xpLevels > 3)
                {
                    xpLevels = 3;
                }
                break;
            }
        }
        return xpLevels;
    }

    /** 把配置里 {@code "biome_id,num_levels"} 形式的字符串解析成表（写错的条目直接跳过） */
    public static Map<String, Integer> parseXpLevelOverridesConfig()
    {
        List<String> xpLevelOverrides = ConfigHandler.GENERAL.perBiomeXpLevels.get();
        Map<String, Integer> parsedOverrides = new HashMap<>();
        for (String override : xpLevelOverrides)
        {
            String[] split = override.split(",");
            if (split.length != 2)
            {
                continue;
            }
            String biomeRegex = split[0];
            String xpLevelsStr = split[1];
            try
            {
                parsedOverrides.put(biomeRegex, Integer.valueOf(xpLevelsStr));
            }
            catch (NumberFormatException e)
            {
                // 写错的经验值，跳过
            }
        }
        return parsedOverrides;
    }

    public static Map<ResourceLocation, Integer> getXpLevelsForAllowedBiomes(List<ResourceLocation> allowedBiomes)
    {
        Map<ResourceLocation, Integer> xpLevels = new HashMap<>();
        for (ResourceLocation biomeKey : allowedBiomes)
        {
            xpLevels.put(biomeKey, getXpLevelsForBiome(biomeKey));
        }
        return xpLevels;
    }

    /** 这个群系会被哪些维度生成 */
    public static List<ResourceLocation> getGeneratingDimensionKeys(ServerLevel serverLevel, Biome biome)
    {
        List<ResourceLocation> dimensions = new ArrayList<>();
        Optional<? extends Registry<Biome>> optionalRegistry = getBiomeRegistry(serverLevel);
        if (optionalRegistry.isEmpty())
        {
            return dimensions;
        }
        Registry<Biome> biomeRegistry = optionalRegistry.get();
        Optional<ResourceKey<Biome>> optionalKey = biomeRegistry.getResourceKey(biome);
        if (optionalKey.isEmpty())
        {
            return dimensions;
        }
        Optional<Holder.Reference<Biome>> optionalHolder = biomeRegistry.getHolder(optionalKey.get());
        if (optionalHolder.isEmpty())
        {
            return dimensions;
        }
        Holder<Biome> biomeHolder = optionalHolder.get();
        for (ServerLevel level : serverLevel.getServer().getAllLevels())
        {
            if (level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().contains(biomeHolder))
            {
                dimensions.add(level.dimension().location());
            }
        }
        return dimensions;
    }

    /** 所有允许搜索的群系 → 生成它的维度列表（发给客户端做「维度」排序用） */
    public static ListMultimap<ResourceLocation, ResourceLocation> getGeneratingDimensionsForAllowedBiomes(ServerLevel serverLevel)
    {
        ListMultimap<ResourceLocation, ResourceLocation> dimensionsForAllowedBiomes = ArrayListMultimap.create();
        for (ResourceLocation biomeKey : getAllowedBiomeKeys(serverLevel))
        {
            Optional<Biome> optionalBiome = getBiomeForKey(serverLevel, biomeKey);
            optionalBiome.ifPresent(biome -> dimensionsForAllowedBiomes.putAll(
                    biomeKey, getGeneratingDimensionKeys(serverLevel, biome)));
        }
        return dimensionsForAllowedBiomes;
    }

    /** 群系尺寸系数（上游同样写死 4，见其 TODO） */
    public static int getBiomeSize(Level level)
    {
        return 4;
    }

    public static int getDistanceToBiome(Player player, int biomeX, int biomeZ)
    {
        return getDistanceToBiome(player.blockPosition(), biomeX, biomeZ);
    }

    public static int getDistanceToBiome(BlockPos startPos, int biomeX, int biomeZ)
    {
        return (int) Mth.sqrt((float) startPos.distSqr(new BlockPos(biomeX, startPos.getY(), biomeZ)));
    }

    /** 配置项 biomeBlacklist 命中（支持 * 与 ? 通配） */
    public static boolean biomeKeyIsBlacklisted(ResourceLocation biomeKey)
    {
        for (String key : ConfigHandler.GENERAL.biomeBlacklist.get())
        {
            if (biomeKey.toString().matches(convertToRegex(key)))
            {
                return true;
            }
        }
        return false;
    }

    /** 群系带 {@code c:hidden_from_locator_selection} 标签就不许罗盘搜（别的定位类模组的通用约定） */
    public static boolean biomeKeyIsHidden(Level level, ResourceLocation biomeKey)
    {
        Optional<? extends Registry<Biome>> optionalRegistry = getBiomeRegistry(level);
        if (optionalRegistry.isEmpty())
        {
            return false;
        }
        Registry<Biome> biomeRegistry = optionalRegistry.get();
        Optional<Biome> optionalBiome = getBiomeForKey(level, biomeKey);
        if (optionalBiome.isEmpty())
        {
            return false;
        }
        Optional<ResourceKey<Biome>> optionalKey = biomeRegistry.getResourceKey(optionalBiome.get());
        if (optionalKey.isEmpty())
        {
            return false;
        }
        Optional<Holder.Reference<Biome>> optionalHolder = biomeRegistry.getHolder(optionalKey.get());
        if (optionalHolder.isEmpty())
        {
            return false;
        }
        return optionalHolder.get().tags().anyMatch(tag -> tag.location().toString().equals("c:hidden_from_locator_selection"));
    }

    /** 配置里的 glob（* / ?）转成正则 */
    private static String convertToRegex(String glob)
    {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++)
        {
            char c = glob.charAt(i);
            if (c == '*')
            {
                regex.append(".*");
            }
            else if (c == '?')
            {
                regex.append('.');
            }
            else if (c == '.')
            {
                regex.append("\\.");
            }
            else
            {
                regex.append(c);
            }
        }
        regex.append('$');
        return regex.toString();
    }
}
