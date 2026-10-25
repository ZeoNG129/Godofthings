package com.godofthings.wt.client;

import java.util.HashMap;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;

/**
 * 无线终端的客户端状态（补货数量缓存）。
 */
public final class WtClientState
{
    private WtClientState()
    {
    }

    private static HashMap<Holder<Item>, Long> restockAmounts = new HashMap<>();

    public static void setRestockAmounts(HashMap<Holder<Item>, Long> amounts)
    {
        restockAmounts = amounts;
    }

    public static HashMap<Holder<Item>, Long> getRestockAmounts()
    {
        return restockAmounts;
    }
}
