package com.godofthings.generator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * 能量发电机工具方法（数值裁剪、无线范围归一化、大数值格式化、无线范围实体充电）。
 */
public final class EnergyGenTool
{
    private EnergyGenTool() {}

    public static long suit(long value)
    {
        return value < 0 ? Long.MAX_VALUE : value;
    }

    public static int suitInt(long value)
    {
        return value < 0 || value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    /**
     * 归一化无线充电区块范围，只允许 1/3/5/7（1x1、3x3、5x5、7x7 区块）。
     * 相比原版（1/3/5），新增 7x7 一档，即"无线充电最大距离增加一个无线距离"。
     */
    public static int normalizeWirelessRange(int range)
    {
        return range <= 1 ? 1 : (range <= 3 ? 3 : (range <= 5 ? 5 : 7));
    }

    /**
     * 无线充电：给无线范围内所有生物（玩家/怪物）携带的全部可充电物品充电。
     * 区域与分片扫描区域一致（以机器所在区块为基准的 range*16 方块列）。
     * energySupplier 读当前储电，consume 扣减实际充入量。
     */
    public static void chargeEntitiesInWirelessRange(Level level, BlockPos pos, int wirelessRange,
                                                     LongSupplier energySupplier, LongConsumer consume)
    {
        int range = normalizeWirelessRange(wirelessRange);
        int half = range >> 1;
        int originX = pos.getX() >> 4 << 4;
        int originZ = pos.getZ() >> 4 << 4;
        double halfWidth = range * 8.0D;
        double centerX = originX - half * 16 + halfWidth;
        double centerZ = originZ - half * 16 + halfWidth;
        AABB box = new AABB(centerX - halfWidth, level.getMinBuildHeight(), centerZ - halfWidth,
                centerX + halfWidth, level.getMaxBuildHeight(), centerZ + halfWidth);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : entities)
        {
            for (ItemStack stack : entity.getAllSlots())
            {
                if (stack.isEmpty())
                {
                    continue;
                }
                long energy = energySupplier.getAsLong();
                if (energy <= 0)
                {
                    return;
                }
                int maxOutput = suitInt(energy);
                int accepted = stack.getCapability(ForgeCapabilities.ENERGY)
                        .map(storage -> storage.canReceive() ? storage.receiveEnergy(maxOutput, false) : 0)
                        .orElse(0);
                if (accepted < 0)
                {
                    accepted = 0;
                }
                if (accepted > maxOutput)
                {
                    accepted = maxOutput;
                }
                if (accepted > 0)
                {
                    consume.accept(accepted);
                }
            }
        }
    }

    /**
     * 格式化大数值用于 GUI 展示。小于 10000 时原样显示，更大时使用 K/M/G/T/P/E 单位缩写。
     */
    public static String formatLong(long value)
    {
        if (value < 0)
        {
            return Long.toString(value);
        }
        if (value < 10_000L)
        {
            return Long.toString(value);
        }
        if (value < 1_000_000L)
        {
            return String.format("%.2fK", value / 1_000.0);
        }
        if (value < 1_000_000_000L)
        {
            return String.format("%.2fM", value / 1_000_000.0);
        }
        if (value < 1_000_000_000_000L)
        {
            return String.format("%.2fG", value / 1_000_000_000.0);
        }
        if (value < 1_000_000_000_000_000L)
        {
            return String.format("%.2fT", value / 1_000_000_000_000.0);
        }
        if (value < 1_000_000_000_000_000_000L)
        {
            return String.format("%.2fP", value / 1_000_000_000_000_000.0);
        }
        return String.format("%.2fE", value / 1_000_000_000_000_000_000.0);
    }
}
