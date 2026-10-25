package com.godofthings.wt.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 无线终端的物品标签（照抄 ae2wtlib 的 {@code AE2wtlibTags}，[本地改动] 命名空间改为
 * godofthings；数据文件 data/godofthings/tags/item/no_restock.json）。
 */
public final class WtTags
{
    private WtTags()
    {
    }

    public static final TagKey<Item> NO_RESTOCK = TagKey.create(Registries.ITEM,
            WtComponents.godofthingsId("no_restock"));
}
