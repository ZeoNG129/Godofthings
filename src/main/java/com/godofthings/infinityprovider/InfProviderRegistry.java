package com.godofthings.infinityprovider;

import java.util.function.Supplier;

import com.godofthings.Godofthings;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

/**
 * 无限样板供应器注册表（移植自 InfinityPatternProvider 2.0.0）。
 *
 * <p>四档供应器 = 1 / 32 / 1024 / 32723（无限）个样板槽，每档都有
 * 方块形态与线缆部件（part）形态。注册名沿用上游
 * （{@code _1slots_pattern_provider} 等，前导下划线是上游为规避 Java
 * 标识符不能以数字开头的写法），仅命名空间换成 {@code godofthings}。</p>
 *
 * <p>上游的独立创造页在本模组里改为挂进「众神之物」主页（见 Godofthings 的 tab 定义）。</p>
 */
public final class InfProviderRegistry {

    public static final Blocks BLOCKS = DeferredRegister.createBlocks(Godofthings.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Godofthings.MODID);
    public static final Items ITEMS = DeferredRegister.createItems(Godofthings.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Godofthings.MODID);

    // ---- 方块（四档）----
    public static final DeferredBlock<InfProviderBlockEntity.Provider1Block> P1_BLOCK =
            BLOCKS.register("_1slots_pattern_provider", InfProviderBlockEntity.Provider1Block::new);
    public static final DeferredBlock<InfProviderBlockEntity.Provider32Block> P32_BLOCK =
            BLOCKS.register("_32slots_pattern_provider", InfProviderBlockEntity.Provider32Block::new);
    public static final DeferredBlock<InfProviderBlockEntity.Provider1024Block> P1024_BLOCK =
            BLOCKS.register("_1024slots_pattern_provider", InfProviderBlockEntity.Provider1024Block::new);
    public static final DeferredBlock<InfProviderBlockEntity.ProviderInfBlock> PINF_BLOCK =
            BLOCKS.register("infinity_pattern_provider", InfProviderBlockEntity.ProviderInfBlock::new);

    // ---- 方块物品 ----
    public static final DeferredItem<BlockItem> P1_ITEM =
            ITEMS.registerSimpleBlockItem("_1slots_pattern_provider", P1_BLOCK);
    public static final DeferredItem<BlockItem> P32_ITEM =
            ITEMS.registerSimpleBlockItem("_32slots_pattern_provider", P32_BLOCK);
    public static final DeferredItem<BlockItem> P1024_ITEM =
            ITEMS.registerSimpleBlockItem("_1024slots_pattern_provider", P1024_BLOCK);
    public static final DeferredItem<BlockItem> PINF_ITEM =
            ITEMS.registerSimpleBlockItem("infinity_pattern_provider", PINF_BLOCK);

    // ---- 部件（part）物品：挂在 AE2 线缆上的形态 ----
    public static final DeferredItem<InfProviderPart.Provider1PartItem> P1_PART_ITEM =
            ITEMS.registerItem("_1slots_pattern_provider_part",
                    props -> new InfProviderPart.Provider1PartItem(props), new Item.Properties());
    public static final DeferredItem<InfProviderPart.Provider32PartItem> P32_PART_ITEM =
            ITEMS.registerItem("_32slots_pattern_provider_part",
                    props -> new InfProviderPart.Provider32PartItem(props), new Item.Properties());
    public static final DeferredItem<InfProviderPart.Provider1024PartItem> P1024_PART_ITEM =
            ITEMS.registerItem("_1024slots_pattern_provider_part",
                    props -> new InfProviderPart.Provider1024PartItem(props), new Item.Properties());
    public static final DeferredItem<InfProviderPart.ProviderInfPartItem> PINF_PART_ITEM =
            ITEMS.registerItem("infinity_pattern_provider_part",
                    props -> new InfProviderPart.ProviderInfPartItem(props), new Item.Properties());

    // ---- 方块实体（四档）----
    public static final Supplier<BlockEntityType<InfProviderBlockEntity.Provider1>> P1_BE =
            BLOCK_ENTITIES.register("_1slots_pattern_provider",
                    () -> BlockEntityType.Builder.of(InfProviderBlockEntity.Provider1::new, P1_BLOCK.get()).build(null));
    public static final Supplier<BlockEntityType<InfProviderBlockEntity.Provider32>> P32_BE =
            BLOCK_ENTITIES.register("_32slots_pattern_provider",
                    () -> BlockEntityType.Builder.of(InfProviderBlockEntity.Provider32::new, P32_BLOCK.get()).build(null));
    public static final Supplier<BlockEntityType<InfProviderBlockEntity.Provider1024>> P1024_BE =
            BLOCK_ENTITIES.register("_1024slots_pattern_provider",
                    () -> BlockEntityType.Builder.of(InfProviderBlockEntity.Provider1024::new, P1024_BLOCK.get()).build(null));
    public static final Supplier<BlockEntityType<InfProviderBlockEntity.ProviderInf>> PINF_BE =
            BLOCK_ENTITIES.register("infinity_pattern_provider",
                    () -> BlockEntityType.Builder.of(InfProviderBlockEntity.ProviderInf::new, PINF_BLOCK.get()).build(null));

    // ---- 菜单（四档）----
    public static final Supplier<MenuType<InfProviderMenu.Provider1Menu>> P1_MENU =
            MENUS.register("_1slots_pattern_provider", () -> InfProviderMenu.Provider1Menu.TYPE);
    public static final Supplier<MenuType<InfProviderMenu.Provider32Menu>> P32_MENU =
            MENUS.register("_32slots_pattern_provider", () -> InfProviderMenu.Provider32Menu.TYPE);
    public static final Supplier<MenuType<InfProviderMenu.Provider1024Menu>> P1024_MENU =
            MENUS.register("_1024slots_pattern_provider", () -> InfProviderMenu.Provider1024Menu.TYPE);
    public static final Supplier<MenuType<InfProviderMenu.ProviderInfMenu>> PINF_MENU =
            MENUS.register("infinity_pattern_provider", () -> InfProviderMenu.ProviderInfMenu.TYPE);

    static {
        // 菜单 TYPE 由 MenuTypeBuilder 构造，须在注册器条目求值前先初始化好
        InfProviderMenu.Provider1Menu.init();
        InfProviderMenu.Provider32Menu.init();
        InfProviderMenu.Provider1024Menu.init();
        InfProviderMenu.ProviderInfMenu.init();
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
    }

    private InfProviderRegistry() {
    }
}
