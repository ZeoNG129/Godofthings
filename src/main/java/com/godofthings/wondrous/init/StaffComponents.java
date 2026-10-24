package com.godofthings.wondrous.init;

import com.godofthings.Godofthings;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.mojang.serialization.Codec;

import java.util.List;
import java.util.UUID;

/**
 * 荒辰移晷之杖的数据组件（照抄上游 StretcherComponents 中杖用到的条目，前缀不变——
 * 键名带 wondrous_/staff_/entity_timer_/range_ 前缀，不会与本项目既有组件撞名）。
 * [本地改动] BeefTimeAccelerationEnabledComponent（基 mod G 菜单开关）在本地改名为
 * {@link #STAFF_TIME_ACCELERATION}（wondrous_staff_enabled）。
 */
public final class StaffComponents
{
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Godofthings.MODID);

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> integer(String name)
    {
        return DATA_COMPONENTS.register(name, () -> DataComponentType.<Integer>builder()
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT)
                .build());
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> bool(String name, boolean network)
    {
        return DATA_COMPONENTS.register(name, () -> DataComponentType.<Boolean>builder()
                .persistent(Codec.BOOL)
                .networkSynchronized(ByteBufCodecs.BOOL)
                .build());
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WONDROUS_STAFF_SPEED =
            integer("wondrous_staff_speed");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ENTITY_TIMER_MODE =
            bool("entity_timer_mode", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENTITY_TIMER_SPEED =
            integer("entity_timer_speed");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WONDROUS_STAFF_PERMANENT =
            bool("wondrous_staff_permanent", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WONDROUS_STAFF_MODE =
            integer("wondrous_staff_mode");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WONDROUS_STAFF_SUMMON_ENABLED =
            bool("wondrous_staff_summon_enabled", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WONDROUS_STAFF_LOOT_REFRESH =
            bool("wondrous_staff_loot_refresh", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WONDROUS_STAFF_PHASE_ENABLED =
            bool("wondrous_staff_phase_enabled", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WONDROUS_STAFF_PHASE_INTERACTION =
            bool("wondrous_staff_phase_interaction", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> WONDROUS_STAFF_RADIATION_PROTECTION =
            bool("wondrous_staff_radiation_protection", true);
    /** [本地改动] 上游 BeefTimeAccelerationEnabledComponent 的本地对应。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> STAFF_TIME_ACCELERATION =
            bool("wondrous_staff_enabled", true);

    // ---- 范围加速场（照抄上游） ----
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RANGE_SIZE_X =
            integer("range_size_x");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RANGE_SIZE_Y =
            integer("range_size_y");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RANGE_SIZE_Z =
            integer("range_size_z");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RANGE_OFFSET_X =
            integer("range_offset_x");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RANGE_OFFSET_Y =
            integer("range_offset_y");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RANGE_OFFSET_Z =
            integer("range_offset_z");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RANGE_SPEED =
            integer("range_speed");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> RANGE_PLACEMENT_MODE =
            bool("range_placement_mode", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> RANGE_FILTER_MARKING_MODE =
            bool("range_filter_marking_mode", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> RANGE_WHITELIST_MODE =
            bool("range_whitelist_mode", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> RANGE_MARK_SLEEP_LIST =
            bool("range_mark_sleep_list", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> RANGE_IDLE_THROTTLE_DISABLED =
            bool("range_idle_throttle_disabled", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RANGE_ACCELERATION_MARK_COUNT =
            integer("range_mark_count");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RANGE_SLEEP_MARK_COUNT =
            integer("range_sleep_mark_count");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ResourceLocation>>> RANGE_FILTERS =
            DATA_COMPONENTS.register("range_filters", () -> DataComponentType.<List<ResourceLocation>>builder()
                    .persistent(ResourceLocation.CODEC.listOf())
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(ResourceLocation.CODEC).apply(ByteBufCodecs.list()))
                    .build());

    // ---- APOC 附魔台兼容（照抄上游） ----
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> APOTHEOSIS_TABLE_SELECTION =
            bool("apotheosis_table_selection", true);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> APOTHEOSIS_ETERNA =
            integer("apotheosis_eterna");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> APOTHEOSIS_QUANTA =
            integer("apotheosis_quanta");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> APOTHEOSIS_ARCANA =
            integer("apotheosis_arcana");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> APOTHEOSIS_CLUES =
            integer("apotheosis_clues");
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> APOTHEOSIS_WORLD_TIER =
            DATA_COMPONENTS.register("apotheosis_world_tier", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    private StaffComponents()
    {
    }

    /** 由 Godofthings 构造器调用。 */
    public static void register(IEventBus modEventBus)
    {
        DATA_COMPONENTS.register(modEventBus);
    }
}
