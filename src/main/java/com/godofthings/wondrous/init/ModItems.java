package com.godofthings.wondrous.init;

import com.godofthings.doll.init.DollRegistry;
import com.godofthings.wondrous.content.item.WondrousStaffItem;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.item.Item;

/**
 * 上游 ModItems 的本地替身（[本地改动]）。
 * <p>
 * 上游杖用到的物品只有两件：杖本体与紬紬玩偶（树叶奖励）。杖本体在
 * {@link WondrousItems} 注册；玩偶是本模组 doll 包的既有物品，这里只做别名引用，
 * 让转换后的代码保持 {@code ModItems.X.get()} 的上游写法。
 */
public final class ModItems
{
    public static final DeferredHolder<Item, WondrousStaffItem> WONDROUS_STAFF = WondrousItems.WONDROUS_STAFF;

    /** [本地改动] 上游 TWO_I_DOLL 指向本模组已有的紬紬玩偶物品。 */
    public static final DeferredHolder<Item, com.godofthings.doll.item.TwoIDollItem> TWO_I_DOLL =
            DollRegistry.TWO_I_DOLL;

    private ModItems()
    {
    }
}
