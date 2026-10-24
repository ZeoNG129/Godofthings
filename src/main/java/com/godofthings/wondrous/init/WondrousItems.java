package com.godofthings.wondrous.init;

import com.godofthings.Godofthings;
import com.godofthings.wondrous.content.item.WondrousStaffItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 荒辰移晷之杖的物品注册（照抄上游 ModItems.WONDROUS_STAFF 的注册形态）。
 * [本地改动] 上游的 STAFF_LEAF_REWARD / RANGE_RECLAIMER 是独立物品；本地只移植杖本体，
 * 树叶奖励实体不改名（StaffLeafRewardEntity 不需要独立物品形态），
 * 回收界面用杖本体承载 personal 模式。
 */
public final class WondrousItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Godofthings.MODID);

    public static final DeferredItem<WondrousStaffItem> WONDROUS_STAFF =
            ITEMS.registerItem("wondrous_staff",
                    props -> new WondrousStaffItem(props.stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    private WondrousItems()
    {
    }

    /** 由 Godofthings 构造器调用。 */
    public static void register(IEventBus modEventBus)
    {
        ITEMS.register(modEventBus);
    }
}
