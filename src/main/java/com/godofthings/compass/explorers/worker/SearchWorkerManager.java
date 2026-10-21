package com.godofthings.compass.explorers.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.godofthings.compass.explorers.util.StructureUtils;
import com.mojang.datafixers.util.Pair;
import org.apache.commons.lang3.RandomStringUtils;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/**
 * 搜索任务队列（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0；逻辑一行未改）。
 *
 * <p>一个结构集可能对应多个 placement，每个 placement 各派一个 worker；按顺序一个个跑，
 * 前面失败就 {@link #pop()} 掉换下一个。</p>
 *
 * <p>{@code foundChunks} 是「这个玩家已经搜到过的区块」缓存（静态，跨物品实例共享），
 * 搜到就记进去、失败时按配置 {@code cleanCache} 决定要不要清掉。</p>
 */
public class SearchWorkerManager
{
    private final String id = RandomStringUtils.random(8, "0123456789abcdef");

    private final List<StructureSearchWorker<?>> workers;
    public static List<Pair<UUID, ChunkPos>> foundChunks = new ArrayList<>();

    public SearchWorkerManager()
    {
        workers = new ArrayList<>();
    }

    public void createWorkers(ServerLevel level, Player player, ItemStack stack, List<Structure> structures,
            BlockPos startPos)
    {
        workers.clear();

        Map<StructurePlacement, List<Structure>> placementToStructuresMap = new Object2ObjectArrayMap<>();

        for (Structure structure : structures)
        {
            for (StructurePlacement structureplacement : level.getChunkSource().getGeneratorState()
                    .getPlacementsForStructure(
                            Objects.requireNonNull(StructureUtils.getHolderForStructure(level, structure))))
            {
                placementToStructuresMap.computeIfAbsent(structureplacement, holderSet -> new ObjectArrayList<>())
                        .add(structure);
            }
        }

        for (Map.Entry<StructurePlacement, List<Structure>> entry : placementToStructuresMap.entrySet())
        {
            StructurePlacement placement = entry.getKey();
            if (placement instanceof ConcentricRingsStructurePlacement concentricRings)
            {
                workers.add(new ConcentricRingsSearchWorker(level, player, stack, startPos, concentricRings,
                        entry.getValue(), id, foundChunks));
            }
            else if (placement instanceof RandomSpreadStructurePlacement randomSpread)
            {
                workers.add(new RandomSpreadSearchWorker(level, player, stack, startPos, randomSpread,
                        entry.getValue(), id, foundChunks));
            }
            else
            {
                workers.add(new GenericSearchWorker(level, player, stack, startPos, placement, entry.getValue(), id,
                        foundChunks));
            }
        }
    }

    /** 起第一个 worker；队列为空返回 false */
    public boolean start()
    {
        if (!workers.isEmpty())
        {
            workers.get(0).start();
            return true;
        }
        return false;
    }

    public void pop()
    {
        if (!workers.isEmpty())
        {
            workers.remove(0);
        }
    }

    public void stop()
    {
        for (StructureSearchWorker<?> worker : workers)
        {
            worker.stop();
        }
    }

    public void clear()
    {
        workers.clear();
    }
}
