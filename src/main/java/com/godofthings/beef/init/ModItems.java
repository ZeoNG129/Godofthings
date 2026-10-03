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
 * 已按用户要求移除。</p>
 *
 * <p><b>本次死代码清理</b>：v5.8.0 删掉唯一还继承 {@code EndlessBeafItem} 的「荒辰移晷之杖」之后，
 * 牛排工具框架（工具本体 + 模式轮盘 + 工具按键 / 网络包 / HUD / 挖掘辅助 / 建筑手杖 / 各类
 * 「拿着工具才可能触发」的模式）已整套没有任何入口，连同该基类一并删除。
 * 本类从此只注册无用维度的 3 个传送方块物品。</p>
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
