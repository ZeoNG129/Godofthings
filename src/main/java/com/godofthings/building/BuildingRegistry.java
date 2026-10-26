package com.godofthings.building;

import java.util.EnumMap;
import java.util.Map;

import com.godofthings.Godofthings;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 众神建材（移植自 AE2 Lightning Tech 的猪咪建材）：纯装饰方块 + 连接贴图（CTM）。
 *
 * <p>注册名沿用上游（{@code white_pigmee_building_panel} 等），显示名按需求把「猪咪」
 * 换成「众神」；通用款 {@code pigmee_building_block} / {@code pigmee_building_slab}
 * 的显示名保持「猪咪建材方块 / 猪咪建材台阶」。</p>
 *
 * <p>方块共 66 个：素板 16 色 + 框板 16 色 + 素板台阶 16 色 + 框板台阶 16 色 + 建材方块 + 建材台阶，
 * 另加一个合成材料「染料基底」物品。创造页 {@code 众神之物：方块} 一并在这里注册。
 * 方块物品直接复用主类的 {@code Godofthings.BLOCKS / ITEMS}（DeferredRegister.Blocks/Items），
 * 避免 66 个方块各自再建注册表。</p>
 */
public final class BuildingRegistry
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Godofthings.MODID);

    // ---- 染料基底（变种配方的中心材料）----
    public static final DeferredItem<Item> DYE_BASE =
            Godofthings.ITEMS.registerItem("dye_base", Item::new);

    // ---- 猪咪建材方块 / 台阶（通用款，原样移植）----
    public static final DeferredBlock<PigmeeBuildingBlock> PIGMEE_BUILDING_BLOCK =
            Godofthings.BLOCKS.register("pigmee_building_block", PigmeeBuildingBlock::new);
    public static final DeferredItem<BlockItem> PIGMEE_BUILDING_BLOCK_ITEM =
            Godofthings.ITEMS.registerSimpleBlockItem(PIGMEE_BUILDING_BLOCK, new Item.Properties());

    public static final DeferredBlock<PigmeeBuildingSlabBlock> PIGMEE_BUILDING_SLAB =
            Godofthings.BLOCKS.register("pigmee_building_slab",
                    () -> new PigmeeBuildingSlabBlock(MapColor.COLOR_PINK));
    public static final DeferredItem<BlockItem> PIGMEE_BUILDING_SLAB_ITEM =
            Godofthings.ITEMS.registerSimpleBlockItem(PIGMEE_BUILDING_SLAB, new Item.Properties());

    // ---- 16 色变种：素板 / 框板 / 素板台阶 / 框板台阶 ----
    public static final Map<DyeColor, DeferredBlock<PigmeeBuildingPanelBlock>> BUILDING_PANELS =
            registerPanels(false);
    public static final Map<DyeColor, DeferredBlock<PigmeeBuildingPanelBlock>> FRAMED_BUILDING_PANELS =
            registerPanels(true);
    public static final Map<DyeColor, DeferredBlock<PigmeeBuildingSlabBlock>> BUILDING_SLABS =
            registerSlabs(false);
    public static final Map<DyeColor, DeferredBlock<PigmeeBuildingSlabBlock>> FRAMED_BUILDING_SLABS =
            registerSlabs(true);

    private static Map<DyeColor, DeferredBlock<PigmeeBuildingPanelBlock>> registerPanels(boolean framed) {
        var panels = new EnumMap<DyeColor, DeferredBlock<PigmeeBuildingPanelBlock>>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName() + (framed ? "_pigmee_framed_building_panel" : "_pigmee_building_panel");
            panels.put(color, Godofthings.BLOCKS.register(name,
                    () -> new PigmeeBuildingPanelBlock(color, framed)));
        }
        return java.util.Collections.unmodifiableMap(panels);
    }

    private static Map<DyeColor, DeferredBlock<PigmeeBuildingSlabBlock>> registerSlabs(boolean framed) {
        var slabs = new EnumMap<DyeColor, DeferredBlock<PigmeeBuildingSlabBlock>>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            String name = color.getName() + (framed ? "_pigmee_framed_building_slab" : "_pigmee_building_slab");
            slabs.put(color, Godofthings.BLOCKS.register(name,
                    () -> new PigmeeBuildingSlabBlock(color.getMapColor())));
        }
        return java.util.Collections.unmodifiableMap(slabs);
    }

    // ---- 16 色变种的方块物品（registerSimpleBlockItem 无名字面量，须逐个补挂）----
    public static final Map<DyeColor, DeferredItem<BlockItem>> BUILDING_PANEL_ITEMS = registerPanelItems(BUILDING_PANELS);
    public static final Map<DyeColor, DeferredItem<BlockItem>> FRAMED_BUILDING_PANEL_ITEMS = registerPanelItems(FRAMED_BUILDING_PANELS);
    public static final Map<DyeColor, DeferredItem<BlockItem>> BUILDING_SLAB_ITEMS = registerSlabItems(BUILDING_SLABS);
    public static final Map<DyeColor, DeferredItem<BlockItem>> FRAMED_BUILDING_SLAB_ITEMS = registerSlabItems(FRAMED_BUILDING_SLABS);

    private static Map<DyeColor, DeferredItem<BlockItem>> registerPanelItems(
            Map<DyeColor, DeferredBlock<PigmeeBuildingPanelBlock>> blocks) {
        var items = new EnumMap<DyeColor, DeferredItem<BlockItem>>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            items.put(color, Godofthings.ITEMS.registerSimpleBlockItem(blocks.get(color), new Item.Properties()));
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    private static Map<DyeColor, DeferredItem<BlockItem>> registerSlabItems(
            Map<DyeColor, DeferredBlock<PigmeeBuildingSlabBlock>> blocks) {
        var items = new EnumMap<DyeColor, DeferredItem<BlockItem>>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            items.put(color, Godofthings.ITEMS.registerSimpleBlockItem(blocks.get(color), new Item.Properties()));
        }
        return java.util.Collections.unmodifiableMap(items);
    }

    // ---- 创造物品栏：众神之物：方块 ----
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GOD_BUILDING_TAB =
            CREATIVE_MODE_TABS.register("god_building_tab", () -> CreativeModeTab.builder()
                    .icon(() -> BUILDING_PANEL_ITEMS.get(DyeColor.WHITE).get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.godofthings.god_building_tab"))
                    .displayItems((parameters, output) ->
                    {
                        output.accept(DYE_BASE.get());
                        output.accept(PIGMEE_BUILDING_BLOCK_ITEM.get());
                        output.accept(PIGMEE_BUILDING_SLAB_ITEM.get());
                        for (DyeColor color : DyeColor.values()) {
                            output.accept(BUILDING_PANEL_ITEMS.get(color).get());
                            output.accept(FRAMED_BUILDING_PANEL_ITEMS.get(color).get());
                            output.accept(BUILDING_SLAB_ITEMS.get(color).get());
                            output.accept(FRAMED_BUILDING_SLAB_ITEMS.get(color).get());
                        }
                        // 开放式电梯（移植自 ElevatorMod，MIT）：16 色排在「众神之物：方块」最后
                        com.godofthings.elevator.ElevatorRegistry.ELEVATOR_ITEMS
                                .values().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    public static void register(IEventBus modEventBus)
    {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private BuildingRegistry()
    {
    }
}
