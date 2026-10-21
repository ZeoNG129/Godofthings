package com.godofthings.compass.explorers.worker;

import java.util.List;
import java.util.UUID;

import com.godofthings.compass.explorers.ExplorersCompassData;
import com.godofthings.compass.explorers.config.ConfigHandler;
import com.godofthings.compass.explorers.items.ExplorersCompassItem;
import com.godofthings.compass.explorers.util.StructureUtils;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.neoforged.neoforge.common.WorldWorkerManager;

/**
 * 结构搜索的基类（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0；
 * 搜索算法一行未改）。
 *
 * <p>走的是 NeoForge 的 {@link WorldWorkerManager}：每 tick 在服务端主线程做一小步，不会卡服。
 * 子类只负责「下一个候选区块在哪」，命中判定 / 半径刷新 / 缓存都在这层。</p>
 *
 * <p><b>已删除</b>：上游 {@link #succeed} 里「搜到后往小地图模组里加路径点」的那段联动
 * （原本是查 {@code ModList} 里有没有装那个小地图模组，装了就往客户端发一条建路径点的包）——
 * 用户明确要求去掉。现在搜到结构只写物品状态，不再碰任何外部地图模组。</p>
 */
public abstract class StructureSearchWorker<T extends StructurePlacement> implements WorldWorkerManager.IWorker
{
    protected String managerId;
    protected ServerLevel level;
    protected Player player;
    protected ItemStack stack;
    protected BlockPos startPos;
    protected BlockPos currentPos;
    protected T placement;
    protected List<Structure> structureSet;
    protected int samples;
    protected boolean finished;
    protected int lastRadiusThreshold;
    protected List<Pair<UUID, ChunkPos>> foundChunks;

    public StructureSearchWorker(ServerLevel level, Player player, ItemStack stack, BlockPos startPos, T placement,
            List<Structure> structureSet, String managerId, List<Pair<UUID, ChunkPos>> foundChunks)
    {
        this.level = level;
        this.player = player;
        this.stack = stack;
        this.startPos = startPos;
        this.structureSet = structureSet;
        this.placement = placement;
        this.managerId = managerId;
        this.foundChunks = foundChunks;

        currentPos = startPos;
        samples = 0;

        finished = !level.getServer().getWorldData().worldGenOptions().generateStructures();
    }

    public void start()
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            if (ConfigHandler.GENERAL.maxRadius.get() > 0)
            {
                ExplorersCompassData.LOGGER.info("SearchWorkerManager {}: {} starting with {}{} max samples", managerId,
                        getName(), shouldLogRadius() ? ConfigHandler.GENERAL.maxRadius.get() + " max radius, " : "",
                        ConfigHandler.GENERAL.maxSamples.get());
                WorldWorkerManager.addWorker(this);
            }
            else
            {
                fail();
            }
        }
    }

    @Override
    public boolean hasWork()
    {
        return !finished && getRadius() < ConfigHandler.GENERAL.maxRadius.get()
                && samples < ConfigHandler.GENERAL.maxSamples.get();
    }

    @Override
    public boolean doWork()
    {
        int radius = getRadius();
        if (radius > 250 && radius / 250 > lastRadiusThreshold)
        {
            if (ExplorersCompassData.isCompass(stack))
            {
                ((ExplorersCompassItem) stack.getItem()).setSearchRadius(stack, roundRadius(radius), player);
            }
            lastRadiusThreshold = radius / 250;
        }
        return false;
    }

    /** 这个区块里有没有目标结构（已经在缓存里就跳过） */
    protected Pair<BlockPos, Structure> getStructureGeneratingAt(ChunkPos chunkPos)
    {
        if (foundChunks.contains(Pair.of(player.getUUID(), chunkPos)))
        {
            return null;
        }

        for (Structure structure : structureSet)
        {
            StructureCheckResult result = level.structureManager().checkStructurePresence(chunkPos, structure,
                    placement, false);
            if (result != StructureCheckResult.START_NOT_PRESENT)
            {
                if (result == StructureCheckResult.START_PRESENT)
                {
                    return Pair.of(placement.getLocatePos(chunkPos), structure);
                }

                ChunkAccess chunkAccess = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
                StructureStart structureStart = level.structureManager()
                        .getStartForStructure(SectionPos.bottomOf(chunkAccess), structure, chunkAccess);
                if (structureStart != null && structureStart.isValid())
                {
                    return Pair.of(placement.getLocatePos(structureStart.getChunkPos()), structure);
                }
            }
        }

        return null;
    }

    protected void succeed(BlockPos pos, Structure structure)
    {
        ExplorersCompassData.LOGGER.info("SearchWorkerManager {}: {} succeeded with {}{} samples", managerId,
                getName(), shouldLogRadius() ? getRadius() + " radius, " : "", samples);
        if (stack.getItem() instanceof ExplorersCompassItem compass)
        {
            compass.succeed(stack, StructureUtils.getKeyForStructure(level, structure), pos.getX(), pos.getZ(),
                    samples, ConfigHandler.GENERAL.displayCoordinates.get());
        }
        else
        {
            ExplorersCompassData.LOGGER.error(
                    "SearchWorkerManager {}: {} found invalid compass after successful search", managerId, getName());
        }
        ChunkPos chunkPos = new ChunkPos(pos);
        foundChunks.add(Pair.of(player.getUUID(), chunkPos));
        finished = true;
    }

    protected void fail()
    {
        ExplorersCompassData.LOGGER.info("SearchWorkerManager {}: {} failed with {}{} samples", managerId, getName(),
                shouldLogRadius() ? getRadius() + " radius, " : "", samples);
        if (ExplorersCompassData.isCompass(stack))
        {
            ((ExplorersCompassItem) stack.getItem()).fail(stack, roundRadius(getRadius()), samples);
        }
        else
        {
            ExplorersCompassData.LOGGER.error(
                    "SearchWorkerManager {}: {} found invalid compass after failed search", managerId, getName());
        }
        if (ConfigHandler.GENERAL.cleanCache.get())
        {
            foundChunks.removeIf(pair -> pair.getFirst().equals(player.getUUID()));
        }

        finished = true;
    }

    public void stop()
    {
        ExplorersCompassData.LOGGER.info("SearchWorkerManager {}: {} stopped with {}{} samples", managerId, getName(),
                shouldLogRadius() ? getRadius() + " radius, " : "", samples);
        finished = true;
    }

    protected int getRadius()
    {
        return StructureUtils.getHorizontalDistanceToLocation(startPos, currentPos.getX(), currentPos.getZ());
    }

    protected int roundRadius(int radius)
    {
        return (radius / 250) * 250;
    }

    protected abstract String getName();

    protected abstract boolean shouldLogRadius();
}
