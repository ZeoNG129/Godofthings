package com.godofthings.superfusion.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 框选范围收集（移植自 Mekanism-Card 的 {@code BatchSelectionHelper}）。
 * <p>
 * 把两个角点之间的长方体展开成坐标列表，并做体积/距离上限保护，
 * 避免玩家一次框到几千个方块把服务端卡死。
 */
public final class BatchSelectionHelper
{
    /** 单边最大格数 */
    private static final int MAX_SIDE = 64;
    /** 最大总格数 */
    private static final int MAX_VOLUME = 32_768;
    /** 玩家到角点的最大距离（格） */
    private static final int MAX_DISTANCE = 128;

    private BatchSelectionHelper()
    {
    }

    public static List<BlockPos> collectPositions(Level level, BlockPos first, BlockPos second, Player player)
    {
        int minX = Math.min(first.getX(), second.getX());
        int maxX = Math.max(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int maxY = Math.max(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxZ = Math.max(first.getZ(), second.getZ());

        long sizeX = (long) maxX - minX + 1;
        long sizeY = (long) maxY - minY + 1;
        long sizeZ = (long) maxZ - minZ + 1;
        long volume = sizeX * sizeY * sizeZ;
        if (sizeX > MAX_SIDE || sizeY > MAX_SIDE || sizeZ > MAX_SIDE || volume > MAX_VOLUME
                || !level.getWorldBorder().isWithinBounds(first)
                || !level.getWorldBorder().isWithinBounds(second)
                || player.blockPosition().distSqr(first) > MAX_DISTANCE * MAX_DISTANCE
                || player.blockPosition().distSqr(second) > MAX_DISTANCE * MAX_DISTANCE)
        {
            player.displayClientMessage(Component.translatable("message.godofthings.super_fusion.selection_too_large")
                    .withStyle(ChatFormatting.RED), true);
            return List.of();
        }

        List<BlockPos> positions = new ArrayList<>();
        for (int x = minX; x <= maxX; x++)
        {
            for (int y = minY; y <= maxY; y++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.hasChunkAt(pos))
                    {
                        positions.add(pos);
                    }
                }
            }
        }
        return positions;
    }
}
