package com.godofthings.compass.explorers.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.godofthings.compass.explorers.ExplorersCompassData;
import com.godofthings.compass.explorers.config.ConfigHandler;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * 结构注册表 / 结构集 / 维度查询工具（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0；
 * 查询逻辑一行未改）。
 *
 * <p>本类<b>只放服务端也会用的查询</b>，不 import 任何 {@code net.minecraft.client.*} ——
 * 搜索线程（{@code worker} 包）在专用服务端上也会调到这里。显示名的美化（要查语言文件）拆到了
 * {@code client.StructureDisplay}，只有客户端界面 / HUD 会引用它。</p>
 *
 * <p>1.20.x Forge → NeoForge 1.21.1 的差异只有一处：{@code new ResourceLocation(...)} 改成
 * {@code ResourceLocation.fromNamespaceAndPath(...)}（1.21 起构造器私有）。</p>
 */
public final class StructureUtils
{
    private StructureUtils()
    {
    }

    public static ListMultimap<ResourceLocation, ResourceLocation> getTypeKeysToStructureKeys(ServerLevel level)
    {
        ListMultimap<ResourceLocation, ResourceLocation> typeKeysToStructureKeys = ArrayListMultimap.create();
        for (Structure structure : getStructureRegistry(level))
        {
            typeKeysToStructureKeys.put(getTypeForStructure(level, structure), getKeyForStructure(level, structure));
        }
        return typeKeysToStructureKeys;
    }

    public static Map<ResourceLocation, ResourceLocation> getStructureKeysToTypeKeys(ServerLevel level)
    {
        Map<ResourceLocation, ResourceLocation> structureKeysToStructureKeys = new HashMap<>();
        for (Structure structure : getStructureRegistry(level))
        {
            structureKeysToStructureKeys.put(getKeyForStructure(level, structure),
                    getTypeForStructure(level, structure));
        }
        return structureKeysToStructureKeys;
    }

    /** 这个结构属于哪个结构集（界面上叫「组」）；不属于任何结构集就返回 {@code godofthings:none} */
    public static ResourceLocation getTypeForStructure(ServerLevel level, Structure structure)
    {
        Registry<StructureSet> registry = getStructureSetRegistry(level);
        for (StructureSet set : registry)
        {
            for (StructureSelectionEntry entry : set.structures())
            {
                if (entry.structure().value().equals(structure))
                {
                    return registry.getKey(set);
                }
            }
        }
        return ExplorersCompassData.NONE;
    }

    public static ResourceLocation getKeyForStructure(ServerLevel level, Structure structure)
    {
        return getStructureRegistry(level).getKey(structure);
    }

    public static Structure getStructureForKey(ServerLevel level, ResourceLocation key)
    {
        return getStructureRegistry(level).get(key);
    }

    public static Holder<Structure> getHolderForStructure(ServerLevel level, Structure structure)
    {
        Optional<ResourceKey<Structure>> optional = getStructureRegistry(level).getResourceKey(structure);
        if (optional.isPresent())
        {
            return getStructureRegistry(level).getHolderOrThrow(optional.get());
        }
        return null;
    }

    /** 界面上可搜的全部结构（已按黑名单过滤） */
    public static List<ResourceLocation> getAllowedStructureKeys(ServerLevel level)
    {
        final List<ResourceLocation> structures = new ArrayList<>();
        for (Structure structure : getStructureRegistry(level))
        {
            if (structure != null && getKeyForStructure(level, structure) != null
                    && !structureIsBlacklisted(level, structure))
            {
                structures.add(getKeyForStructure(level, structure));
            }
        }
        return structures;
    }

    public static boolean structureIsBlacklisted(ServerLevel level, Structure structure)
    {
        final List<String> structureBlacklist = ConfigHandler.GENERAL.structureBlacklist.get();
        for (String structureKey : structureBlacklist)
        {
            if (getKeyForStructure(level, structure).toString().matches(convertToRegex(structureKey)))
            {
                return true;
            }
        }
        return false;
    }

    /** 这个结构能在哪些维度生成（按生物群系集是否有交集判断） */
    public static List<ResourceLocation> getGeneratingDimensionKeys(ServerLevel serverLevel, Structure structure)
    {
        final List<ResourceLocation> dimensions = new ArrayList<>();
        for (ServerLevel level : serverLevel.getServer().getAllLevels())
        {
            ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
            java.util.Set<Holder<Biome>> biomeSet = chunkGenerator.getBiomeSource().possibleBiomes();
            if (!structure.biomes().stream().noneMatch(biomeSet::contains))
            {
                dimensions.add(level.dimension().location());
            }
        }
        // 要塞的生物群系集是空的，会导致维度列表为空 —— 上游的兜底：直接记成主世界
        if (structure.type() == StructureType.STRONGHOLD && dimensions.isEmpty())
        {
            dimensions.add(ResourceLocation.withDefaultNamespace("overworld"));
        }
        return dimensions;
    }

    public static ListMultimap<ResourceLocation, ResourceLocation> getGeneratingDimensionsForAllowedStructures(
            ServerLevel serverLevel)
    {
        ListMultimap<ResourceLocation, ResourceLocation> dimensionsForAllowedStructures = ArrayListMultimap.create();
        for (ResourceLocation structureKey : getAllowedStructureKeys(serverLevel))
        {
            Structure structure = getStructureForKey(serverLevel, structureKey);
            dimensionsForAllowedStructures.putAll(structureKey, getGeneratingDimensionKeys(serverLevel, structure));
        }
        return dimensionsForAllowedStructures;
    }

    public static int getHorizontalDistanceToLocation(Player player, int x, int z)
    {
        return getHorizontalDistanceToLocation(player.blockPosition(), x, z);
    }

    public static int getHorizontalDistanceToLocation(BlockPos startPos, int x, int z)
    {
        return (int) Mth.sqrt((float) startPos.distSqr(new BlockPos(x, startPos.getY(), z)));
    }

    private static Registry<Structure> getStructureRegistry(ServerLevel level)
    {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE);
    }

    private static Registry<StructureSet> getStructureSetRegistry(ServerLevel level)
    {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
    }

    /** 把配置里的通配符（{@code *} / {@code ?}）转成正则 */
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
