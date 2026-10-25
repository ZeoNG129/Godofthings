package com.godofthings.beef.compat.enderio;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ender IO 旅行锚兼容的本地空壳（[本地改动]）。
 * 开发环境没有 enderio 编译依赖（v5.12.1 已把第三方 jar 移出 libs），
 * 全部方法返回「未处理」；ModList 守卫保证运行时装了 enderio 也只会走降级路径。
 */
public final class EnderIOTravelCompat
{
    public static final String MOD_ID = "enderio";

    private EnderIOTravelCompat()
    {
    }

    public static Properties markAsTravelItem(Properties properties)
    {
        return properties;
    }

    public static void setTravelItemEnabled(ItemStack stack, boolean enabled)
    {
    }

    public static InteractionResult tryAnchorTeleport(Level level, Player player)
    {
        return InteractionResult.PASS;
    }
}
