package com.godofthings.advancedmemorycard.item;

import com.godofthings.Godofthings;
import com.godofthings.advancedmemorycard.item.custom.AdvancedMemoryCardItem;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 高级内存卡的物品注册（移植自 suntide_20210418 的 Advanced Memory Card）。
 *
 * <p>按项目约定，{@code DeferredRegister.createItems(...)} 在本类静态初始化执行
 * （Godofthings 构造器第一时间引用本类），{@code register(modEventBus)} 由
 * {@link Godofthings} 构造器调用接线。</p>
 */
public final class AmcItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Godofthings.MODID);

    public static final DeferredItem<AdvancedMemoryCardItem> ADVANCED_MEMORY_CARD =
            ITEMS.registerItem("ae_card",
                    AdvancedMemoryCardItem::new);

    private AmcItems() {
    }

    /** 由 Godofthings 构造器调用，把注册器挂到 mod 事件总线。 */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
