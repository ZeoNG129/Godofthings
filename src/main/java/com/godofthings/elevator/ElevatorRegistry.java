package com.godofthings.elevator;

import com.godofthings.Godofthings;
import com.godofthings.elevator.block.ElevatorBlockEntity;
import com.godofthings.elevator.block.ElevatorContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;

/**
 * 电梯的注册表：16 色方块 / 方块物品 / 方块实体 / 菜单 / 音效。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.neoforge.init.Registry}（MIT），逐行对照。
 * 注册 id 统一加 {@code elevator_} 前缀并挂在本模组命名空间下（如
 * {@code godofthings:elevator_white}），避免与上游 modid 冲突。</p>
 *
 * <p><b>注册调用必须留在 {@link Godofthings} 的静态初始化里</b>（本工程硬规则：
 * {@code DeferredRegister.register(...)} 只能在主类静态初始化调用，否则会因
 * RegisterEvent 已触发而崩服）。因此本类只持有 DeferredRegister 与条目，不自己调 register。</p>
 */
public final class ElevatorRegistry
{
    /** 16 色电梯方块。 */
    public static final EnumMap<DyeColor, DeferredBlock<ElevatorBlock>> ELEVATOR_BLOCKS =
            new EnumMap<>(DyeColor.class);

    /** 建型注册器由主类持有并注册，这里只声明。 */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Godofthings.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Godofthings.MODID);

    /**
     * 16 色方块。
     *
     * <p><b>为什么逐个写而不是 for 循环</b>：本工程的 {@code check-lang.ps1} 用正则扫描
     * {@code BLOCKS.register("字面量", ...)} 来校验「方块 ↔ blockstate ↔ 语言键 ↔ 手册条目」
     * 全覆盖；写成 {@code "elevator_" + color.getName()} 会被解析成字面量 {@code elevator_}，
     * 导致这 16 个方块全部被判为「缺 blockstate」。逐个写既满足校验，读起来也更直白。</p>
     */
    static
    {
        ELEVATOR_BLOCKS.put(DyeColor.WHITE, BLOCKS.register("elevator_white", () -> new ElevatorBlock(DyeColor.WHITE)));
        ELEVATOR_BLOCKS.put(DyeColor.ORANGE, BLOCKS.register("elevator_orange", () -> new ElevatorBlock(DyeColor.ORANGE)));
        ELEVATOR_BLOCKS.put(DyeColor.MAGENTA, BLOCKS.register("elevator_magenta", () -> new ElevatorBlock(DyeColor.MAGENTA)));
        ELEVATOR_BLOCKS.put(DyeColor.LIGHT_BLUE, BLOCKS.register("elevator_light_blue", () -> new ElevatorBlock(DyeColor.LIGHT_BLUE)));
        ELEVATOR_BLOCKS.put(DyeColor.YELLOW, BLOCKS.register("elevator_yellow", () -> new ElevatorBlock(DyeColor.YELLOW)));
        ELEVATOR_BLOCKS.put(DyeColor.LIME, BLOCKS.register("elevator_lime", () -> new ElevatorBlock(DyeColor.LIME)));
        ELEVATOR_BLOCKS.put(DyeColor.PINK, BLOCKS.register("elevator_pink", () -> new ElevatorBlock(DyeColor.PINK)));
        ELEVATOR_BLOCKS.put(DyeColor.GRAY, BLOCKS.register("elevator_gray", () -> new ElevatorBlock(DyeColor.GRAY)));
        ELEVATOR_BLOCKS.put(DyeColor.LIGHT_GRAY, BLOCKS.register("elevator_light_gray", () -> new ElevatorBlock(DyeColor.LIGHT_GRAY)));
        ELEVATOR_BLOCKS.put(DyeColor.CYAN, BLOCKS.register("elevator_cyan", () -> new ElevatorBlock(DyeColor.CYAN)));
        ELEVATOR_BLOCKS.put(DyeColor.PURPLE, BLOCKS.register("elevator_purple", () -> new ElevatorBlock(DyeColor.PURPLE)));
        ELEVATOR_BLOCKS.put(DyeColor.BLUE, BLOCKS.register("elevator_blue", () -> new ElevatorBlock(DyeColor.BLUE)));
        ELEVATOR_BLOCKS.put(DyeColor.BROWN, BLOCKS.register("elevator_brown", () -> new ElevatorBlock(DyeColor.BROWN)));
        ELEVATOR_BLOCKS.put(DyeColor.GREEN, BLOCKS.register("elevator_green", () -> new ElevatorBlock(DyeColor.GREEN)));
        ELEVATOR_BLOCKS.put(DyeColor.RED, BLOCKS.register("elevator_red", () -> new ElevatorBlock(DyeColor.RED)));
        ELEVATOR_BLOCKS.put(DyeColor.BLACK, BLOCKS.register("elevator_black", () -> new ElevatorBlock(DyeColor.BLACK)));
    }

    /** 16 色对应的方块物品。 */
    public static final EnumMap<DyeColor, DeferredItem<BlockItem>> ELEVATOR_ITEMS =
            new EnumMap<>(DyeColor.class);

    static
    {
        ELEVATOR_BLOCKS.forEach((color, block) -> ELEVATOR_ITEMS.put(color, ITEMS.registerSimpleBlockItem(block)));
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Godofthings.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElevatorBlockEntity>> ELEVATOR_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("elevator_tile", () -> BlockEntityType.Builder.of(
                    ElevatorBlockEntity::new,
                    ELEVATOR_BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
            ).build(null));

    public static final DeferredRegister<MenuType<?>> CONTAINERS =
            DeferredRegister.create(Registries.MENU, Godofthings.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ElevatorContainer>> ELEVATOR_CONTAINER =
            CONTAINERS.register("elevator_container", () -> IMenuTypeExtension.create(
                    (windowId, inv, data) -> new ElevatorContainer(windowId, data.readBlockPos(), inv.player)));

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Godofthings.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TELEPORT_SOUND =
            SOUNDS.register("elevator_teleport", () -> SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "elevator_teleport")));

    public static final DeferredHolder<SoundEvent, SoundEvent> CAMOUFLAGE_SOUND =
            SOUNDS.register("elevator_camouflage", () -> SoundEvent.createVariableRangeEvent(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "elevator_camouflage")));

    private ElevatorRegistry() {}
}
