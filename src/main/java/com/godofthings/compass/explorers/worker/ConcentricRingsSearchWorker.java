package com.godofthings.compass.explorers.worker;

import java.util.List;
import java.util.UUID;

import com.godofthings.compass.explorers.config.ConfigHandler;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

/**
 * 同心环搜索（要塞专用；移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0；
 * 算法一行未改）。
 *
 * <p>要塞的候选区块是种子直接算出来的（{@code getRingPositionsFor}），顺序并不按距离排，
 * 所以这里不看半径，只逐个试完候选环、记下最近的那个。</p>
 */
public class ConcentricRingsSearchWorker extends StructureSearchWorker<ConcentricRingsStructurePlacement>
{
    private final List<ChunkPos> potentialChunks;
    private int chunkIndex;
    private double minDistance;
    private Pair<BlockPos, Structure> closest;

    public ConcentricRingsSearchWorker(ServerLevel level, Player player, ItemStack stack, BlockPos startPos,
            ConcentricRingsStructurePlacement placement, List<Structure> structureSet, String managerId,
            List<Pair<UUID, ChunkPos>> foundChunks)
    {
        super(level, player, stack, startPos, placement, structureSet, managerId, foundChunks);

        minDistance = Double.MAX_VALUE;
        chunkIndex = 0;
        potentialChunks = level.getChunkSource().getGeneratorState().getRingPositionsFor(placement);

        finished = !level.getServer().getWorldData().worldGenOptions().generateStructures()
                || potentialChunks == null || potentialChunks.isEmpty();
    }

    @Override
    public boolean hasWork()
    {
        // 候选环的顺序不代表距离，所以这里不判半径，只看采样数与候选数量
        return !finished && samples < ConfigHandler.GENERAL.maxSamples.get() && chunkIndex < potentialChunks.size();
    }

    @Override
    public boolean doWork()
    {
        super.doWork();
        if (hasWork())
        {
            ChunkPos chunkPos = potentialChunks.get(chunkIndex);
            currentPos = new BlockPos(SectionPos.sectionToBlockCoord(chunkPos.x, 8), 0,
                    SectionPos.sectionToBlockCoord(chunkPos.z, 8));
            double distance = startPos.distSqr(currentPos);

            if (closest == null || distance < minDistance)
            {
                Pair<BlockPos, Structure> pair = getStructureGeneratingAt(chunkPos);
                if (pair != null)
                {
                    minDistance = distance;
                    closest = pair;
                }
            }

            samples++;
            chunkIndex++;
        }

        if (hasWork())
        {
            return true;
        }

        if (closest != null)
        {
            succeed(closest.getFirst(), closest.getSecond());
        }
        else if (!finished)
        {
            fail();
        }

        return false;
    }

    @Override
    protected String getName()
    {
        return "ConcentricRingsSearchWorker";
    }

    @Override
    public boolean shouldLogRadius()
    {
        return false;
    }
}
