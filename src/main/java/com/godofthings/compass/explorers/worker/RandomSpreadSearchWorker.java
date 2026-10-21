package com.godofthings.compass.explorers.worker;

import java.util.List;
import java.util.UUID;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

/**
 * 随机散布搜索（村庄 / 神殿 / 前哨站等绝大多数结构走这条；移植自 Explorer's Compass 1.20.x，
 * 作者 ChaosTheDude，CC BY-NC-SA 4.0；算法一行未改）。
 *
 * <p>按 placement 的 {@code spacing} 一格格算候选区块（{@code getPotentialStructureChunk} 用种子算），
 * 从起点所在的那一格开始向外做方形扩张。</p>
 */
public class RandomSpreadSearchWorker extends StructureSearchWorker<RandomSpreadStructurePlacement>
{
    private final int spacing;
    private int length;
    private final int startSectionPosX;
    private final int startSectionPosZ;
    private int x;
    private int z;

    public RandomSpreadSearchWorker(ServerLevel level, Player player, ItemStack stack, BlockPos startPos,
            RandomSpreadStructurePlacement placement, List<Structure> structureSet, String managerId,
            List<Pair<UUID, ChunkPos>> foundChunks)
    {
        super(level, player, stack, startPos, placement, structureSet, managerId, foundChunks);

        spacing = placement.spacing();
        startSectionPosX = SectionPos.blockToSectionCoord(startPos.getX());
        startSectionPosZ = SectionPos.blockToSectionCoord(startPos.getZ());
        x = 0;
        z = 0;
        length = 0;

        finished = !level.getServer().getWorldData().worldGenOptions().generateStructures();
    }

    @Override
    public boolean hasWork()
    {
        return super.hasWork();
    }

    @Override
    public boolean doWork()
    {
        super.doWork();
        if (hasWork())
        {
            boolean shouldSampleX = x == -length || x == length;
            boolean shouldSampleZ = z == -length || z == length;

            if (shouldSampleX || shouldSampleZ)
            {
                int sampleX = startSectionPosX + (spacing * x);
                int sampleZ = startSectionPosZ + (spacing * z);

                ChunkPos chunkPos = placement.getPotentialStructureChunk(level.getSeed(), sampleX, sampleZ);
                currentPos = new BlockPos(SectionPos.sectionToBlockCoord(chunkPos.x, 8), 0,
                        SectionPos.sectionToBlockCoord(chunkPos.z, 8));

                Pair<BlockPos, Structure> pair = getStructureGeneratingAt(chunkPos);
                samples++;
                if (pair != null)
                {
                    succeed(pair.getFirst(), pair.getSecond());
                }
            }
            z++;
            if (z > length)
            {
                x++;
                if (x > length)
                {
                    length++;
                    x -= length;
                }
                z = -length;
            }
        }
        else
        {
            if (!finished)
            {
                fail();
            }
        }

        if (hasWork())
        {
            return true;
        }

        if (!finished)
        {
            fail();
        }

        return false;
    }

    @Override
    protected String getName()
    {
        return "RandomSpreadSearchWorker";
    }

    @Override
    public boolean shouldLogRadius()
    {
        return true;
    }
}
