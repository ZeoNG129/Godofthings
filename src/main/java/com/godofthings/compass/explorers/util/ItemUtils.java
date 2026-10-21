package com.godofthings.compass.explorers.util;

import java.util.function.Predicate;

import com.godofthings.Godofthings;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 找「玩家手上的探险者指南针」（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 */
public final class ItemUtils
{
    private ItemUtils()
    {
    }

    /** 主手优先、其次副手；都没有就返回空栈 */
    public static ItemStack getHeldExplorersCompass(Player player)
    {
        return getHeldItem(player, stack -> stack.getItem() == Godofthings.EXPLORERS_COMPASS.get());
    }

    public static ItemStack getHeldItem(Player player, Item item)
    {
        return getHeldItem(player, stack -> stack.getItem() == item);
    }

    /** 主手优先、其次副手；都不匹配就返回 {@link ItemStack#EMPTY} */
    public static ItemStack getHeldItem(Player player, Predicate<ItemStack> predicate)
    {
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && predicate.test(main))
        {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty() && predicate.test(off))
        {
            return off;
        }
        return ItemStack.EMPTY;
    }
}
