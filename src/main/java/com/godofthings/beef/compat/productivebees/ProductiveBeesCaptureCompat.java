package com.godofthings.beef.compat.productivebees;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * 产物蜜蜂（Productive Bees）捕捉兼容的本地空壳（[本地改动]）。
 * 开发环境无 productivebees 编译依赖，恒返回空；调用方已按「空即跳过」处理。
 */
public final class ProductiveBeesCaptureCompat
{
    private ProductiveBeesCaptureCompat()
    {
    }

    public static ItemStack tryCreateSpawnEgg(Entity entity)
    {
        return ItemStack.EMPTY;
    }
}
