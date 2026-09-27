package com.godofthings.beef.init;

import com.godofthings.beef.UselessMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 无用维度（奇数 / 偶数 / 三维度）三个传送方块对应的物品。
 *
 * <p><b>v5.1.2 起本类只剩这 3 项</b>：照抄 useless_mod 的造化杖（太初洞见之杖 / 造化垂青之杖）
 * 共 7 个物品注册（{@code endless_beaf_item}、{@code _no_wrench}、扳手 / 螺丝刀 / 软锤 / 撬棍 / 铁锤）
 * 已按用户要求移除 —— 本模组只保留「荒辰移晷之杖」一个杖物品。
 * 但基类 {@code com.godofthings.beef.content.items.EndlessBeafItem} <b>保留</b>：
 * 荒辰移晷之杖 {@code extends EndlessBeafItem}，它的采集 / 时运 / 无敌 / 连锁等能力全部来自该基类；
 * 工具形态切换也仍然可用，只是改为写在同一个物品的 {@code CurrentToolTypeComponent} 组件上
 * （见 {@link com.godofthings.beef.content.items.BeefToolVariants}）。</p>
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UselessMod.MODID);

    // 无用维度（奇数 / 偶数 / 三维度）的三个传送方块对应物品。
    // 注册名与上游 ModItems 里这三项一致（上游字段是包级私有，这里提为 public 供创造栏引用）。
    public static final DeferredItem<BlockItem> TELEPORT_BLOCK_ITEM = ITEMS.register(
            "teleport_block",
            () -> new BlockItem(ModBlocks.TELEPORT_BLOCK.get(), new Item.Properties())
    );
    public static final DeferredItem<BlockItem> TELEPORT_BLOCK_ITEM_2 = ITEMS.register(
            "teleport_block_2",
            () -> new BlockItem(ModBlocks.TELEPORT_BLOCK_2.get(), new Item.Properties())
    );
    public static final DeferredItem<BlockItem> TELEPORT_BLOCK_ITEM_3 = ITEMS.register(
            "teleport_block_3",
            () -> new BlockItem(ModBlocks.TELEPORT_BLOCK_3.get(), new Item.Properties())
    );

    private ModItems() {}

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
