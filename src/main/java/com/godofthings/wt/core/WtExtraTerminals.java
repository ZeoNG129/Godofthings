package com.godofthings.wt.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import appeng.items.tools.powered.WirelessTerminalItem;

/**
 * 附属无线终端合并支持（v7.15.5，[本地改动] 对应上游生态里「附属模组往 WTDefinition 注册
 * 自己的终端」的开放点——本模组是独立实现，改为数据驱动：任何 AE2
 * {@link WirelessTerminalItem} 子类的终端都能合并进通用终端，物品栈存
 * {@link WtComponents#EXTRA_TERMINALS}，打开时走该终端物品自己的 AE2 原生菜单）。
 */
public final class WtExtraTerminals
{
    private WtExtraTerminals()
    {
    }

    /** 该物品是否可作为附属终端合并（任何 AE2 无线终端，排除本模组的通用终端自身）。 */
    public static boolean isMergeable(ItemStack stack)
    {
        return !stack.isEmpty()
                && stack.getItem() instanceof WirelessTerminalItem
                && !(stack.getItem() instanceof WtUniversalTerminalItem);
    }

    /** 合并一台附属终端（同物品只留一台，耐久/电量取新来的）。返回 null 表示拒绝。 */
    public static ItemStack merge(ItemStack wut, ItemStack extra)
    {
        if (!isMergeable(extra))
        {
            return null;
        }
        var list = new ArrayList<>(wut.getOrDefault(WtComponents.EXTRA_TERMINALS, List.<ItemStack>of()));
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i).getItem() == extra.getItem())
            {
                // 同型号终端重复合并：替换为新副本（带走新终端的电量与升级）
                list.set(i, extra.copyWithCount(1));
                wut.set(WtComponents.EXTRA_TERMINALS, List.copyOf(list));
                return wut;
            }
        }
        list.add(extra.copyWithCount(1));
        wut.set(WtComponents.EXTRA_TERMINALS, List.copyOf(list));
        return wut;
    }

    public static List<ItemStack> list(ItemStack wut)
    {
        return wut.getOrDefault(WtComponents.EXTRA_TERMINALS, List.<ItemStack>of());
    }

    public static int count(ItemStack wut)
    {
        return list(wut).size();
    }

    /** 循环游标指向的附属终端（-1 = 未指向附属终端）。 */
    public static int current(ItemStack wut)
    {
        return wut.getOrDefault(WtComponents.EXTRA_CURRENT, -1);
    }

    public static void setCurrent(ItemStack wut, int index)
    {
        wut.set(WtComponents.EXTRA_CURRENT, index);
    }

    /** 已装附属终端的显示名（选择按钮 / tooltip 用）。 */
    public static Component displayName(ItemStack extra)
    {
        return extra.getHoverName();
    }
}
