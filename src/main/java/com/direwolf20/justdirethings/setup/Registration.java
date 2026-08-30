package com.direwolf20.justdirethings.setup;

import com.direwolf20.justdirethings.JustDireThings;
import com.direwolf20.justdirethings.common.blockentities.BlockBreakerT1BE;
import com.direwolf20.justdirethings.common.blockentities.BlockBreakerT2BE;
import com.direwolf20.justdirethings.common.blockentities.BlockPlacerT1BE;
import com.direwolf20.justdirethings.common.blockentities.BlockPlacerT2BE;
import com.direwolf20.justdirethings.common.blockentities.BlockSwapperT1BE;
import com.direwolf20.justdirethings.common.blockentities.BlockSwapperT2BE;
import com.direwolf20.justdirethings.common.blockentities.ClickerT1BE;
import com.direwolf20.justdirethings.common.blockentities.ClickerT2BE;
import com.direwolf20.justdirethings.common.blockentities.ItemCollectorBE;
import com.direwolf20.justdirethings.common.blocks.BlockBreakerT1;
import com.direwolf20.justdirethings.common.blocks.BlockBreakerT2;
import com.direwolf20.justdirethings.common.blocks.BlockPlacerT1;
import com.direwolf20.justdirethings.common.blocks.BlockPlacerT2;
import com.direwolf20.justdirethings.common.blocks.BlockSwapperT1;
import com.direwolf20.justdirethings.common.blocks.BlockSwapperT2;
import com.direwolf20.justdirethings.common.blocks.ClickerT1;
import com.direwolf20.justdirethings.common.blocks.ClickerT2;
import com.direwolf20.justdirethings.common.blocks.ItemCollector;
import com.direwolf20.justdirethings.common.containers.BlockBreakerT1Container;
import com.direwolf20.justdirethings.common.containers.BlockBreakerT2Container;
import com.direwolf20.justdirethings.common.containers.BlockPlacerT1Container;
import com.direwolf20.justdirethings.common.containers.BlockPlacerT2Container;
import com.direwolf20.justdirethings.common.containers.BlockSwapperT1Container;
import com.direwolf20.justdirethings.common.containers.BlockSwapperT2Container;
import com.direwolf20.justdirethings.common.containers.ClickerT1Container;
import com.direwolf20.justdirethings.common.containers.ClickerT2Container;
import com.direwolf20.justdirethings.common.containers.ItemCollectorContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 精简注册表：只注册 5 个移植机器（物品拾取器 / 方块破坏器 / 方块放置器 / 点击器 / 替换器，含 T1/T2）。
 */
public class Registration {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
            JustDireThings.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            JustDireThings.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, JustDireThings.MODID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU,
            JustDireThings.MODID);

    public static void init(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        CONTAINERS.register(eventBus);
    }

    // ---- Blocks + Items ----
    public static final RegistryObject<ItemCollector> ItemCollector = BLOCKS.register("itemcollector",
            ItemCollector::new);
    public static final RegistryObject<BlockItem> ItemCollector_ITEM = ITEMS.register("itemcollector",
            () -> new BlockItem(ItemCollector.get(), new Item.Properties()));

    public static final RegistryObject<BlockBreakerT1> BlockBreakerT1 = BLOCKS.register("blockbreakert1",
            BlockBreakerT1::new);
    public static final RegistryObject<BlockItem> BlockBreakerT1_ITEM = ITEMS.register("blockbreakert1",
            () -> new BlockItem(BlockBreakerT1.get(), new Item.Properties()));
    public static final RegistryObject<BlockBreakerT2> BlockBreakerT2 = BLOCKS.register("blockbreakert2",
            BlockBreakerT2::new);
    public static final RegistryObject<BlockItem> BlockBreakerT2_ITEM = ITEMS.register("blockbreakert2",
            () -> new BlockItem(BlockBreakerT2.get(), new Item.Properties()));

    public static final RegistryObject<BlockPlacerT1> BlockPlacerT1 = BLOCKS.register("blockplacert1",
            BlockPlacerT1::new);
    public static final RegistryObject<BlockItem> BlockPlacerT1_ITEM = ITEMS.register("blockplacert1",
            () -> new BlockItem(BlockPlacerT1.get(), new Item.Properties()));
    public static final RegistryObject<BlockPlacerT2> BlockPlacerT2 = BLOCKS.register("blockplacert2",
            BlockPlacerT2::new);
    public static final RegistryObject<BlockItem> BlockPlacerT2_ITEM = ITEMS.register("blockplacert2",
            () -> new BlockItem(BlockPlacerT2.get(), new Item.Properties()));

    public static final RegistryObject<ClickerT1> ClickerT1 = BLOCKS.register("clickert1", ClickerT1::new);
    public static final RegistryObject<BlockItem> ClickerT1_ITEM = ITEMS.register("clickert1",
            () -> new BlockItem(ClickerT1.get(), new Item.Properties()));
    public static final RegistryObject<ClickerT2> ClickerT2 = BLOCKS.register("clickert2", ClickerT2::new);
    public static final RegistryObject<BlockItem> ClickerT2_ITEM = ITEMS.register("clickert2",
            () -> new BlockItem(ClickerT2.get(), new Item.Properties()));

    public static final RegistryObject<BlockSwapperT1> BlockSwapperT1 = BLOCKS.register("blockswappert1",
            BlockSwapperT1::new);
    public static final RegistryObject<BlockItem> BlockSwapperT1_ITEM = ITEMS.register("blockswappert1",
            () -> new BlockItem(BlockSwapperT1.get(), new Item.Properties()));
    public static final RegistryObject<BlockSwapperT2> BlockSwapperT2 = BLOCKS.register("blockswappert2",
            BlockSwapperT2::new);
    public static final RegistryObject<BlockItem> BlockSwapperT2_ITEM = ITEMS.register("blockswappert2",
            () -> new BlockItem(BlockSwapperT2.get(), new Item.Properties()));

    // ---- Block Entities ----
    public static final RegistryObject<BlockEntityType<ItemCollectorBE>> ItemCollectorBE = BLOCK_ENTITIES.register(
            "itemcollectorbe", () -> BlockEntityType.Builder.of(ItemCollectorBE::new, ItemCollector.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockBreakerT1BE>> BlockBreakerT1BE = BLOCK_ENTITIES.register(
            "blockbreakert1", () -> BlockEntityType.Builder.of(BlockBreakerT1BE::new, BlockBreakerT1.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockBreakerT2BE>> BlockBreakerT2BE = BLOCK_ENTITIES.register(
            "blockbreakert2", () -> BlockEntityType.Builder.of(BlockBreakerT2BE::new, BlockBreakerT2.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockPlacerT1BE>> BlockPlacerT1BE = BLOCK_ENTITIES.register(
            "blockplacert1", () -> BlockEntityType.Builder.of(BlockPlacerT1BE::new, BlockPlacerT1.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockPlacerT2BE>> BlockPlacerT2BE = BLOCK_ENTITIES.register(
            "blockplacert2", () -> BlockEntityType.Builder.of(BlockPlacerT2BE::new, BlockPlacerT2.get()).build(null));
    public static final RegistryObject<BlockEntityType<ClickerT1BE>> ClickerT1BE = BLOCK_ENTITIES.register("clickert1",
            () -> BlockEntityType.Builder.of(ClickerT1BE::new, ClickerT1.get()).build(null));
    public static final RegistryObject<BlockEntityType<ClickerT2BE>> ClickerT2BE = BLOCK_ENTITIES.register("clickert2",
            () -> BlockEntityType.Builder.of(ClickerT2BE::new, ClickerT2.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockSwapperT1BE>> BlockSwapperT1BE = BLOCK_ENTITIES.register(
            "blockswappert1",
            () -> BlockEntityType.Builder.of(BlockSwapperT1BE::new, BlockSwapperT1.get()).build(null));
    public static final RegistryObject<BlockEntityType<BlockSwapperT2BE>> BlockSwapperT2BE = BLOCK_ENTITIES.register(
            "blockswappert2",
            () -> BlockEntityType.Builder.of(BlockSwapperT2BE::new, BlockSwapperT2.get()).build(null));

    // ---- Containers ----
    public static final RegistryObject<MenuType<ItemCollectorContainer>> Item_Collector_Container = CONTAINERS
            .register("item_collector_container", () -> IForgeMenuType.create(ItemCollectorContainer::new));
    public static final RegistryObject<MenuType<BlockBreakerT1Container>> BlockBreakerT1_Container = CONTAINERS
            .register("blockbreakert1_container", () -> IForgeMenuType.create(BlockBreakerT1Container::new));
    public static final RegistryObject<MenuType<BlockBreakerT2Container>> BlockBreakerT2_Container = CONTAINERS
            .register("blockbreakert2_container", () -> IForgeMenuType.create(BlockBreakerT2Container::new));
    public static final RegistryObject<MenuType<BlockPlacerT1Container>> BlockPlacerT1_Container = CONTAINERS
            .register("blockplacert1_container", () -> IForgeMenuType.create(BlockPlacerT1Container::new));
    public static final RegistryObject<MenuType<BlockPlacerT2Container>> BlockPlacerT2_Container = CONTAINERS
            .register("blockplacert2_container", () -> IForgeMenuType.create(BlockPlacerT2Container::new));
    public static final RegistryObject<MenuType<ClickerT1Container>> ClickerT1_Container = CONTAINERS
            .register("clickert1_container", () -> IForgeMenuType.create(ClickerT1Container::new));
    public static final RegistryObject<MenuType<ClickerT2Container>> ClickerT2_Container = CONTAINERS
            .register("clickert2_container", () -> IForgeMenuType.create(ClickerT2Container::new));
    public static final RegistryObject<MenuType<BlockSwapperT1Container>> BlockSwapperT1_Container = CONTAINERS
            .register("blockswappert1_container", () -> IForgeMenuType.create(BlockSwapperT1Container::new));
    public static final RegistryObject<MenuType<BlockSwapperT2Container>> BlockSwapperT2_Container = CONTAINERS
            .register("blockswappert2_container", () -> IForgeMenuType.create(BlockSwapperT2Container::new));
}
