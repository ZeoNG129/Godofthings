package com.godofthings.mept;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ME 放置工具系列的数据组件（照抄上游 ModDataComponents）。
 * <p>
 * 上游的 6 个能量相关配置项已随「不消耗能源」要求一并去掉（只删配置与扣电调用，
 * 组件本身上游也不在此处注册能量——能量在 AEBasePoweredItem 内部）。
 */
public final class MeptDataComponents
{
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, com.godofthings.Godofthings.MODID);

    /** 放置配置（物品、流体、选中槽位、放置数量、方向模式） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> PLACEMENT_CONFIG =
            DATA_COMPONENTS.register("mept_placement_config", () ->
                    DataComponentType.<CompoundTag>builder()
                            .persistent(CompoundTag.CODEC)
                            .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                            .build());

    /** 绑定的无线访问点位置 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> LINKED_POSITION =
            DATA_COMPONENTS.register("mept_linked_position", () ->
                    DataComponentType.<GlobalPos>builder()
                            .persistent(GlobalPos.CODEC)
                            .networkSynchronized(GlobalPos.STREAM_CODEC)
                            .build());

    /** 当前页码 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CURRENT_PAGE =
            DATA_COMPONENTS.register("mept_current_page", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    /** 选中槽位 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SELECTED_SLOT =
            DATA_COMPONENTS.register("mept_selected_slot", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    /** 线缆工具：放置模式（LINE / PLANE_FILL / PLANE_BRANCHING） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CABLE_TOOL_MODE =
            DATA_COMPONENTS.register("mept_cable_tool_mode", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    /** 线缆工具：线缆类型（GLASS / COVERED / SMART / DENSE_COVERED / DENSE_SMART） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CABLE_TYPE =
            DATA_COMPONENTS.register("mept_cable_type", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    /** 线缆工具：颜色（AEColor 序号） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CABLE_COLOR =
            DATA_COMPONENTS.register("mept_cable_color", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    /** 线缆工具：已选点位（最多 3 个，编码为 CompoundTag） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> CABLE_POINTS =
            DATA_COMPONENTS.register("mept_cable_points", () ->
                    DataComponentType.<CompoundTag>builder()
                            .persistent(CompoundTag.CODEC)
                            .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                            .build());

    /** 线缆工具：是否装了光谱的钥匙 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> HAS_UPGRADE =
            DATA_COMPONENTS.register("mept_has_upgrade", () ->
                    DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .build());

    /** 线缆工具：颜色快捷栏（5 格，存颜色序号或 -1） */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> COLOR_SHORTCUTS =
            DATA_COMPONENTS.register("mept_color_shortcuts", () ->
                    DataComponentType.<CompoundTag>builder()
                            .persistent(CompoundTag.CODEC)
                            .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                            .build());

    private MeptDataComponents()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        DATA_COMPONENTS.register(modEventBus);
    }
}
