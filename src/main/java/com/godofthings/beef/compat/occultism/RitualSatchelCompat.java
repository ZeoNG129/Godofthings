package com.godofthings.beef.compat.occultism;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;

/**
 * 匠心（Occultism）仪式挎包兼容的本地空壳（[本地改动]）。
 * 开发环境没有 occultism / modonomicon 编译依赖（v5.12.1 已把第三方 jar 移出 libs），
 * 全部方法返回「未处理」；调用方均有 ModList 守卫，运行时装了 occultism 也只走降级路径。
 */
public final class RitualSatchelCompat
{
    private RitualSatchelCompat()
    {
    }

    public static boolean placeFromAe(ServerLevel level, Player player, ItemStack stack,
                                      ResourceLocation multiblockId, BlockPos anchor, Rotation facing)
    {
        return false;
    }
}