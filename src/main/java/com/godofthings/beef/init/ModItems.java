package com.godofthings.beef.init;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.api.enums.tool.ToolTypeMode;
import com.godofthings.beef.content.items.EndlessBeafItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 造化杖的 8 个物品注册项。
 *
 * <p>逐字照抄上游 {@code com.sorrowmist.useless.init.ModItems} 中 {@code ENDLESS_BEAK_*} 这一段
 * （注册名、模式参数、构造调用完全一致），上游其余物品属于别的子系统，未随本次照抄带入。</p>
 *
 * <p>显示名由 {@link EndlessBeafItem#getName} 按附魔模式切换：
 * 精准采集 → <b>太初洞见之杖</b>，时运 → <b>造化垂青之杖</b>。</p>
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UselessMod.MODID);

    // 扳手子类物品注册
    public static final DeferredItem<EndlessBeafItem> ENDLESS_BEAF_WRENCH = ITEMS.register(
            "endless_beaf_wrench",
            () -> new EndlessBeafItem(ToolTypeMode.WRENCH_MODE)
    );
    // 螺丝刀子类物品注册
    public static final DeferredItem<EndlessBeafItem> ENDLESS_BEAF_SCREWDRIVER = ITEMS.register(
            "endless_beaf_screwdriver",
            () -> new EndlessBeafItem(ToolTypeMode.SCREWDRIVER_MODE)
    );
    // 软锤子类物品注册
    public static final DeferredItem<EndlessBeafItem> ENDLESS_BEAF_MALLET = ITEMS.register(
            "endless_beaf_mallet",
            () -> new EndlessBeafItem(ToolTypeMode.MALLET_MODE)
    );
    // 撬棍子类物品注册
    public static final DeferredItem<EndlessBeafItem> ENDLESS_BEAF_CROWBAR = ITEMS.register(
            "endless_beaf_crowbar",
            () -> new EndlessBeafItem(ToolTypeMode.CROWBAR_MODE)
    );
    // 硬锤子类物品注册
    public static final DeferredItem<EndlessBeafItem> ENDLESS_BEAF_HAMMER = ITEMS.register(
            "endless_beaf_hammer",
            () -> new EndlessBeafItem(ToolTypeMode.HAMMER_MODE)
    );
    public static final DeferredItem<EndlessBeafItem> ENDLESS_BEAF_ITEM = ITEMS.register(
            "endless_beaf_item",
            () -> new EndlessBeafItem()
    );
    public static final DeferredItem<EndlessBeafItem> ENDLESS_BEAF_ITEM_NO_WRENCH = ITEMS.register(
            "endless_beaf_item_no_wrench",
            () -> new EndlessBeafItem(ToolTypeMode.NONE_MODE, false)
    );

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
