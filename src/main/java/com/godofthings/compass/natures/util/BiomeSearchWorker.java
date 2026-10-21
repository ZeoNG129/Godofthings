package com.godofthings.compass.natures.util;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;

import com.godofthings.compass.natures.NaturesCompassData;
import com.godofthings.compass.natures.config.ConfigHandler;
import com.godofthings.compass.natures.items.NaturesCompassItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.WorldWorkerManager;

/**
 * 群系搜索算法（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0；算法一行未改）。
 *
 * <p>走的是 NeoForge 的 {@link WorldWorkerManager}：每 tick 在服务端主线程做一小步，不会卡服。
 * 搜索路径是从起点开始向外扩张的方形螺旋（北 → 东 → 南 → 西，每绕一圈边长 +1 个采样格），
 * 每个采样点沿 Y 轴取 {@code yValues} 里的若干个高度，取生物群系源里的群系 id 比较。</p>
 *
 * <p>「搜索下一个」时还要额外判断新找到的群系是否与之前找到的<b>连通</b>（{@link #checkConnections()}）：
 * 沿直线在两点之间采样，若出现一段连续的非目标群系（阈值是总采样数的 25%，最多 10），就认为不连通，
 * 跳过这个候选点继续搜。</p>
 */
public class BiomeSearchWorker implements WorldWorkerManager.IWorker
{
    private final String id = RandomStringUtils.random(8, "0123456789abcdef");

    private final int sampleSpace;
    private final int maxSamples;
    private final int maxRadius;
    private final ServerLevel level;
    private final ResourceLocation biomeKey;
    private final BlockPos startPos;
    private int samples;
    private int nextLength;
    private Direction direction;
    private final ItemStack stack;
    private final Player player;
    private int x;
    private int z;
    private final int[] yValues;
    private int length;
    private boolean finished;
    private int lastRadiusThreshold;
    private final List<BlockPos> prevPos;
    private boolean checkingConnections;
    private BlockPos candidatePos;
    private int prevPosIndex;
    private int connectionsCheckSampleIndex;
    private int connectionsCheckSamples;
    private int consecutiveNonMatchingSamples;

    public BiomeSearchWorker(ServerLevel level, Player player, ItemStack stack, ResourceLocation biomeKey,
            BlockPos startPos, List<BlockPos> prevPos)
    {
        this.level = level;
        this.player = player;
        this.stack = stack;
        this.biomeKey = biomeKey;
        this.startPos = startPos;
        this.prevPos = prevPos;
        this.x = startPos.getX();
        this.z = startPos.getZ();
        this.yValues = Mth.outFromOrigin(startPos.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight(), 64)
                .toArray();
        this.sampleSpace = ConfigHandler.GENERAL.sampleSpaceModifier.get() * BiomeUtils.getBiomeSize(level);
        this.maxSamples = ConfigHandler.GENERAL.maxSamples.get();
        this.maxRadius = ConfigHandler.GENERAL.radiusModifier.get() * BiomeUtils.getBiomeSize(level);
        this.nextLength = sampleSpace;
        this.length = 0;
        this.samples = 0;
        this.direction = Direction.UP;
        this.finished = false;
        this.lastRadiusThreshold = 0;
    }

    public void start()
    {
        if (!stack.isEmpty() && NaturesCompassData.isCompass(stack))
        {
            if (maxRadius > 0 && sampleSpace > 0)
            {
                NaturesCompassData.LOGGER.info("BiomeSearchWorker " + id + ": Starting search: " + sampleSpace
                        + " sample space, " + maxSamples + " max samples, " + maxRadius + " max radius, "
                        + prevPos.size() + " previous locations");
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
        return biomeKey != null && !finished && getRadius() <= maxRadius && samples <= maxSamples;
    }

    @Override
    public boolean doWork()
    {
        if (checkingConnections)
        {
            checkConnections();
            return !finished;
        }

        if (hasWork())
        {
            if (direction == Direction.NORTH)
            {
                z -= sampleSpace;
            }
            else if (direction == Direction.EAST)
            {
                x += sampleSpace;
            }
            else if (direction == Direction.SOUTH)
            {
                z += sampleSpace;
            }
            else if (direction == Direction.WEST)
            {
                x -= sampleSpace;
            }

            int sampleX = QuartPos.fromBlock(x);
            int sampleZ = QuartPos.fromBlock(z);

            for (int y : yValues)
            {
                int sampleY = QuartPos.fromBlock(y);
                Biome biomeAtPos = level.getChunkSource().getGenerator().getBiomeSource()
                        .getNoiseBiome(sampleX, sampleY, sampleZ, level.getChunkSource().randomState().sampler()).value();
                Optional<ResourceLocation> optionalBiomeAtPosKey = BiomeUtils.getKeyForBiome(level, biomeAtPos);
                if (optionalBiomeAtPosKey.isPresent() && optionalBiomeAtPosKey.get().equals(biomeKey))
                {
                    if (prevPos.isEmpty())
                    {
                        prevPos.add(new BlockPos(x, y, z));
                        succeed();
                        return false;
                    }

                    checkingConnections = true;
                    candidatePos = new BlockPos(x, y, z);
                    prevPosIndex = 0;
                    connectionsCheckSampleIndex = 0;
                    return true;
                }
            }

            finishSample();
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

    /**
     * 检查待定候选点是否与之前找到的某个位置连通。不连通（中间隔着一大段非目标群系）就接受它并成功返回；
     * 连通（说明是同一片群系的延伸）就跳过候选点继续搜。
     */
    private void checkConnections()
    {
        if (prevPosIndex >= prevPos.size())
        {
            // 走完所有历史位置都没找到连通的 → 接受候选点
            prevPos.add(candidatePos);
            checkingConnections = false;
            succeed();
            return;
        }

        BlockPos currentPrevPos = prevPos.get(prevPosIndex);
        int deltaX = candidatePos.getX() - currentPrevPos.getX();
        int deltaZ = candidatePos.getZ() - currentPrevPos.getZ();

        if (connectionsCheckSampleIndex == 0)
        {
            // 候选点离历史位置不到 2 个采样格 → 直接当连通，跳过
            double distance = Math.sqrt((double) (deltaX * deltaX) + (double) (deltaZ * deltaZ));
            if (distance <= sampleSpace * 2)
            {
                checkingConnections = false;
                finishSample();
                return;
            }

            connectionsCheckSamples = (int) (distance / sampleSpace);
            consecutiveNonMatchingSamples = 0;
            connectionsCheckSampleIndex = 1;
            return;
        }

        if (connectionsCheckSampleIndex >= connectionsCheckSamples)
        {
            // 整条线都采样完没找到足够大的空隙 → 当连通，跳过
            checkingConnections = false;
            finishSample();
            return;
        }

        // 沿历史位置 → 候选点的直线采样一个点
        double t = (double) connectionsCheckSampleIndex / connectionsCheckSamples;
        int checkSampleX = QuartPos.fromBlock((int) (currentPrevPos.getX() + deltaX * t));
        int checkSampleZ = QuartPos.fromBlock((int) (currentPrevPos.getZ() + deltaZ * t));

        boolean foundMatchingBiome = false;
        for (int y : yValues)
        {
            int checkSampleY = QuartPos.fromBlock(y);
            Biome biomeAtCheck = level.getChunkSource().getGenerator().getBiomeSource()
                    .getNoiseBiome(checkSampleX, checkSampleY, checkSampleZ, level.getChunkSource().randomState().sampler())
                    .value();
            Optional<ResourceLocation> checkBiomeKey = BiomeUtils.getKeyForBiome(level, biomeAtCheck);
            if (checkBiomeKey.isPresent() && checkBiomeKey.get().equals(biomeKey))
            {
                foundMatchingBiome = true;
                break;
            }
        }

        if (foundMatchingBiome)
        {
            consecutiveNonMatchingSamples = 0;
        }
        else
        {
            consecutiveNonMatchingSamples++;
            // 连续不匹配达到总采样数的 25%（最多 10 个）→ 认为两点不连通，换下一个历史位置
            int consecutiveNonMatchingThreshold = Math.clamp(connectionsCheckSamples / 4, 1, 10);
            if (consecutiveNonMatchingSamples >= consecutiveNonMatchingThreshold)
            {
                prevPosIndex++;
                connectionsCheckSampleIndex = 0;
                consecutiveNonMatchingSamples = 0;
                return;
            }
        }

        connectionsCheckSampleIndex++;
    }

    /**
     * 记一次采样、转向、刷新罗盘上的搜索半径。没匹配上（或跳过候选点）时调用。
     */
    private void finishSample()
    {
        samples++;
        length += sampleSpace;
        if (length >= nextLength)
        {
            if (direction == Direction.UP)
            {
                direction = Direction.NORTH;
            }
            else
            {
                nextLength += sampleSpace;
                direction = direction.getClockWise();
            }
            length = 0;
        }

        int radius = getRadius();
        if (radius > 500 && radius / 500 > lastRadiusThreshold)
        {
            if (!stack.isEmpty() && NaturesCompassData.isCompass(stack))
            {
                ((NaturesCompassItem) stack.getItem()).setSearchRadius(stack, roundRadius(radius, 500), player);
            }
            lastRadiusThreshold = radius / 500;
        }
    }

    private void succeed()
    {
        NaturesCompassData.LOGGER.info("BiomeSearchWorker " + id + ": Search succeeded: " + getRadius()
                + " radius, " + samples + " samples");
        if (!stack.isEmpty() && NaturesCompassData.isCompass(stack))
        {
            ((NaturesCompassItem) stack.getItem()).succeed(stack, player, x, z, prevPos, samples,
                    ConfigHandler.GENERAL.displayCoordinates.get());
        }
        else
        {
            NaturesCompassData.LOGGER.error("BiomeSearchWorker " + id + ": Invalid compass after search");
        }
        finished = true;
    }

    private void fail()
    {
        NaturesCompassData.LOGGER.info("BiomeSearchWorker " + id + ": Search failed: " + getRadius()
                + " radius, " + samples + " samples");
        if (!stack.isEmpty() && NaturesCompassData.isCompass(stack))
        {
            ((NaturesCompassItem) stack.getItem()).fail(stack, player, roundRadius(getRadius(), 500), samples);
        }
        else
        {
            NaturesCompassData.LOGGER.error("BiomeSearchWorker " + id + ": Invalid compass after search");
        }
        finished = true;
    }

    public void stop()
    {
        NaturesCompassData.LOGGER.info("BiomeSearchWorker " + id + ": Search stopped: " + getRadius()
                + " radius, " + samples + " samples");
        finished = true;
    }

    private int getRadius()
    {
        return BiomeUtils.getDistanceToBiome(startPos, x, z);
    }

    private int roundRadius(int radius, int roundTo)
    {
        return (radius / roundTo) * roundTo;
    }
}
