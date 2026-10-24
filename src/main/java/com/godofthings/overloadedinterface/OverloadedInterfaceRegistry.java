package com.godofthings.overloadedinterface;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.godofthings.Godofthings;

/**
 * 过载接口的注册（方块 / 物品 / 方块实体 / 菜单）。
 *
 * <p>照项目约定，所有 {@code register(...)} 调用集中在 {@link Godofthings} 的
 * 静态初始化里，本类只持有注册器与条目引用，由 {@link #register(IEventBus)} 接线。</p>
 */
public final class OverloadedInterfaceRegistry
{
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Godofthings.MODID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Godofthings.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE,
                    Godofthings.MODID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU,
                    Godofthings.MODID);

    public static final DeferredBlock<OverloadedInterfaceBlock> OVERLOADED_INTERFACE =
            BLOCKS.register("overloaded_interface", () -> new OverloadedInterfaceBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 8.0F)
                            .sound(net.minecraft.world.level.block.SoundType.METAL)
                            .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> OVERLOADED_INTERFACE_ITEM =
            ITEMS.registerSimpleBlockItem("overloaded_interface", OVERLOADED_INTERFACE);

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<OverloadedInterfaceBlockEntity>> OVERLOADED_INTERFACE_BE =
            BLOCK_ENTITIES.register("overloaded_interface",
                    () -> BlockEntityType.Builder.of(OverloadedInterfaceBlockEntity::new,
                            OVERLOADED_INTERFACE.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,
            net.minecraft.world.inventory.MenuType<OverloadedInterfaceMenu>> OVERLOADED_INTERFACE_MENU =
            MENUS.register("overloaded_interface", () -> OverloadedInterfaceMenu.TYPE);

    private OverloadedInterfaceRegistry()
    {
    }

    /** 由 Godofthings 构造器调用。 */
    public static void register(IEventBus modEventBus)
    {
        OverloadedInterfaceMenu.init();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
    }

    /** 创造栏分类用。 */
    public static boolean isOverloadedInterfaceItem(Item item)
    {
        return item == OVERLOADED_INTERFACE_ITEM.get();
    }
}
