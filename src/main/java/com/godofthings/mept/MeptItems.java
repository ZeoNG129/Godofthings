package com.godofthings.mept;

import com.godofthings.Godofthings;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.api.features.GridLinkables;

/**
 * ME 放置工具的物品注册（照抄上游 MEPlacementToolMod 的注册段；fumo 摆件方块不移植）。
 * 上游三个工具继承 AEBasePoweredItem（电量系统），本地全部换成不耗能源版（见 BasePlacementToolItem）。
 */
public final class MeptItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Godofthings.MODID);

    public static final DeferredItem<ItemMEPlacementTool> ME_PLACEMENT_TOOL =
            ITEMS.registerItem("me_placement_tool",
                    props -> new ItemMEPlacementTool(props.stacksTo(1)));

    public static final DeferredItem<ItemMultiblockPlacementTool> MULTIBLOCK_PLACEMENT_TOOL =
            ITEMS.registerItem("multiblock_placement_tool",
                    props -> new ItemMultiblockPlacementTool(props.stacksTo(1)));

    public static final DeferredItem<ItemKeyOfSpectrum> KEY_OF_SPECTRUM =
            ITEMS.registerItem("key_of_spectrum",
                    props -> new ItemKeyOfSpectrum(props.stacksTo(64)));

    public static final DeferredItem<ItemPrismCore> PRISM_CORE =
            ITEMS.registerItem("prism_core",
                    props -> new ItemPrismCore(props.stacksTo(64)));

    public static final DeferredItem<ItemMECablePlacementTool> ME_CABLE_PLACEMENT_TOOL =
            ITEMS.registerItem("me_cable_placement_tool",
                    props -> new ItemMECablePlacementTool(props.stacksTo(1)));

    private MeptItems()
    {
    }

    /** 由 Godofthings 构造器调用，把注册器挂到 mod 事件总线。 */
    public static void register(IEventBus modEventBus)
    {
        ITEMS.register(modEventBus);
    }

    /** AE2 可绑定物注册（必须在 commonSetup enqueueWork 里调用，构造期取 DeferredHolder 会 NPE）。 */
    public static void registerAeLinkables()
    {
        GridLinkables.register(ME_PLACEMENT_TOOL.get(), BasePlacementToolItem.LINKABLE_HANDLER);
        GridLinkables.register(MULTIBLOCK_PLACEMENT_TOOL.get(), BasePlacementToolItem.LINKABLE_HANDLER);
        GridLinkables.register(ME_CABLE_PLACEMENT_TOOL.get(), BasePlacementToolItem.LINKABLE_HANDLER);
    }
}
