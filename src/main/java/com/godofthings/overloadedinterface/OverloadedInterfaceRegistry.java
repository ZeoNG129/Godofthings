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
            BLOCKS.register("interface", () -> new OverloadedInterfaceBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.5F, 8.0F)
                            .sound(net.minecraft.world.level.block.SoundType.METAL)
                            .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> OVERLOADED_INTERFACE_ITEM =
            ITEMS.registerSimpleBlockItem("interface", OVERLOADED_INTERFACE);

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<OverloadedInterfaceBlockEntity>> OVERLOADED_INTERFACE_BE =
            BLOCK_ENTITIES.register("interface",
                    () -> BlockEntityType.Builder.of(OverloadedInterfaceBlockEntity::new,
                            OVERLOADED_INTERFACE.get()).build(null));

    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>,
            net.minecraft.world.inventory.MenuType<OverloadedInterfaceMenu>> OVERLOADED_INTERFACE_MENU =
            MENUS.register("interface", () -> OverloadedInterfaceMenu.TYPE);

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

    /**
     * 绑定方块与方块实体（AE2 特有流程，必须在 commonSetup 里调用）。
     *
     * <p>AE2 的 {@code AEBaseEntityBlock.newBlockEntity} 依赖内部的 blockEntityType 字段，
     * 不调用 {@code setBlockEntity} 就会在放置方块时抛
     * {@code NullPointerException: this.blockEntityType is null}。
     * 同时要把方块实体与物品关联起来，否则 AE2 的部分 UI 逻辑拿不到代表物品。</p>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void bindBlockAndEntity()
    {
        var block = (appeng.block.AEBaseEntityBlock) OVERLOADED_INTERFACE.get();
        BlockEntityType<OverloadedInterfaceBlockEntity> type = openBeType();
        block.setBlockEntity(OverloadedInterfaceBlockEntity.class, type, null, null);
        appeng.blockentity.AEBaseBlockEntity.registerBlockEntityItem(
                type, OVERLOADED_INTERFACE_ITEM.get());
    }

    /** 强类型引用（DeferredHolder 的泛型是 BlockEntityType<?>，这里收窄一次）。 */
    @SuppressWarnings("unchecked")
    private static BlockEntityType<OverloadedInterfaceBlockEntity> openBeType()
    {
        return (BlockEntityType<OverloadedInterfaceBlockEntity>) (BlockEntityType<?>)
                OVERLOADED_INTERFACE_BE.get();
    }
}
