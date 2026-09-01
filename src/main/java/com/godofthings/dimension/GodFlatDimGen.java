package com.godofthings.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 神之平坦维度生成器：一片由「平滑石头（边框）/ 磨制安山岩（填充）/ 海晶灯（中心）」组成的平台。
 * 移植自 useless_mod 的 UselessDimGen2（1.20.1）。
 */
public class GodFlatDimGen extends ChunkGenerator
{
    // 平台构成方块（按用户要求固定）
    private static final Block BORDER_BLOCK = Blocks.SMOOTH_STONE;
    private static final Block FILL_BLOCK = Blocks.POLISHED_ANDESITE;
    private static final Block CENTER_BLOCK = Blocks.SEA_LANTERN;

    // 平台参数
    private static final int PLATFORM_START_Y = -64;
    private static final int PLATFORM_LAYERS = 69;
    private static final boolean GENERATE_BEDROCK = true;

    // 与 dimension_type/superflat.json 保持一致：min_y=-64, height=384
    private static final int GEN_DEPTH = 384;

    // 1.21.1：ChunkGenerator.codec() 返回 MapCodec（原 Codec），
    // 用 RecordCodecBuilder.mapCodec + instance.stable（参照原版 FlatLevelSource.CODEC 写法）
    public static final MapCodec<GodFlatDimGen> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
            ).apply(instance, instance.stable(GodFlatDimGen::new))
    );

    public GodFlatDimGen(BiomeSource biomeSource)
    {
        super(biomeSource);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region)
    {
        // 平台世界不生成原始怪物
    }

    @Override
    public int getSeaLevel()
    {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState)
    {
        BlockState[] states = new BlockState[level.getHeight()];
        int maxY = PLATFORM_START_Y + PLATFORM_LAYERS;

        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++)
        {
            int index = y - level.getMinBuildHeight();

            if (y == PLATFORM_START_Y && GENERATE_BEDROCK)
            {
                states[index] = Blocks.BEDROCK.defaultBlockState();
            }
            else if (y > PLATFORM_START_Y && y <= maxY)
            {
                states[index] = FILL_BLOCK.defaultBlockState();
            }
            else
            {
                states[index] = Blocks.AIR.defaultBlockState();
            }
        }

        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec()
    {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk)
    {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int maxY = PLATFORM_START_Y + PLATFORM_LAYERS;
        int airStartY = maxY + 1;
        int maxBuildHeight = chunk.getMaxBuildHeight();

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                // 基岩底层
                if (GENERATE_BEDROCK)
                {
                    chunk.setBlockState(pos.set(x, PLATFORM_START_Y, z), Blocks.BEDROCK.defaultBlockState(), false);
                }

                // 平台（Y=startY+1 到 maxY）
                for (int y = PLATFORM_START_Y + 1; y <= maxY; y++)
                {
                    BlockState blockState;

                    // 区块坐标 (8,8) 的中心方块
                    if (x == 8 && z == 8)
                    {
                        blockState = CENTER_BLOCK.defaultBlockState();
                    }
                    // 最北边一行 (z=0) 或最西边一列 (x=0) 为边框
                    else if (x == 0 || z == 0)
                    {
                        blockState = BORDER_BLOCK.defaultBlockState();
                    }
                    else
                    {
                        blockState = FILL_BLOCK.defaultBlockState();
                    }

                    chunk.setBlockState(pos.set(x, y, z), blockState, false);
                }

                // 空气层
                for (int y = airStartY; y < maxBuildHeight; y++)
                {
                    chunk.setBlockState(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), false);
                }
            }
        }
    }

    // 1.21.1：fillFromNoise 移除了 Executor 参数
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk)
    {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor level, RandomState randomState)
    {
        return PLATFORM_START_Y + PLATFORM_LAYERS + 1;
    }

    @Override
    public int getMinY()
    {
        return PLATFORM_START_Y;
    }

    @Override
    public int getGenDepth()
    {
        return GEN_DEPTH;
    }

    @Override
    public void applyCarvers(WorldGenRegion chunkRegion, long seed, RandomState randomState, BiomeManager biomeAccess, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving generationStep)
    {
        // 不需要洞穴雕刻
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos pos)
    {
        list.add("God of Things - Flat Platform");
        list.add("边框方块: " + BORDER_BLOCK.getName().getString());
        list.add("填充方块: " + FILL_BLOCK.getName().getString());
        list.add("中心方块: " + CENTER_BLOCK.getName().getString());
    }
}
