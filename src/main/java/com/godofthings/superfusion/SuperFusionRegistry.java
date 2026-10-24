package com.godofthings.superfusion;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

/**
 * 超级融合卡的物品注册（软依赖 Mekanism）。
 * <p>
 * 注意：这里的 {@code register(...)} 调用发生在 {@link #getInstance()} 被第一次调用时，
 * 而 {@code Godofthings} 只在确认 Mekanism 已加载后才会调用它——因此没装 Mekanism 时
 * 这个物品根本不会被注册，也就不会出现在创造栏或 JEI 里。
 * <p>
 * {@code DeferredRegister} 本身挂在 {@code Godofthings.ITEMS} 上，保证「所有 register 调用
 * 都源自 Godofthings 的静态初始化」这一约定不被破坏。
 */
public final class SuperFusionRegistry
{
    private static boolean initialized = false;
    private static DeferredHolder<Item, SuperFusionItem> superFusionItem;

    private SuperFusionRegistry()
    {
    }

    /** 在 {@code Godofthings} 构造函数里调用（仅当 Mekanism 已加载） */
    public static void init()
    {
        if (initialized)
        {
            return;
        }
        initialized = true;
        superFusionItem = com.godofthings.Godofthings.ITEMS.registerItem(
                "super_fusion_card", props -> new SuperFusionItem());
        // 数据组件注册表也在这里挂到 mod 总线（同样只在 Mekanism 在场时才注册）
    }

    /** 注册数据组件用的总线挂载（在 init 之后由 Godofthings 调用） */
    public static void registerDataComponents(IEventBus modEventBus)
    {
        if (initialized)
        {
            SuperFusionComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        }
    }

    /** 物品本体；未启用时为 null */
    public static DeferredHolder<Item, SuperFusionItem> item()
    {
        return superFusionItem;
    }

    /** 创造栏 / 手册用：未启用时返回 null，调用方要判空 */
    public static Item itemOrNull()
    {
        return superFusionItem == null ? null : superFusionItem.get();
    }

    /** 子系统是否已启用 */
    public static boolean isEnabled()
    {
        return initialized && superFusionItem != null;
    }

    /** 内部：让 {@code SuperFusionItem} 能读到自身注册项（供能力注册等使用） */
    static DeferredHolder<Item, SuperFusionItem> requireItem()
    {
        if (superFusionItem == null)
        {
            throw new IllegalStateException("super fusion card was not registered");
        }
        return superFusionItem;
    }

    /** 供能力注册直接引用 */
}
