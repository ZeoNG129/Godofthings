package com.godofthings.wt.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import appeng.api.config.IncludeExclude;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;

/**
 * 无耗能无线通用终端的组件键（照抄 ae2wtlib 的 {@code AE2wtlibComponents}，[本地改动]
 * 注册到本模组的 DATA_COMPONENTS、命名空间改为 godofthings）。
 */
public final class WtComponents
{
    private WtComponents()
    {
    }

    private static final Consumer<DataComponentType.Builder<CompoundTag>> COMPOUND_TAG_CODECS = builder -> builder
            .persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG);
    private static final Consumer<DataComponentType.Builder<IncludeExclude>> INCLUDE_EXCLUDE_CODECS = builder -> builder
            .persistent(Codec.BOOL.xmap(WtComponents::booleanToIncludeExclude,
                    WtComponents::includeExcludeToBoolean))
            .networkSynchronized(NeoForgeStreamCodecs.enumCodec(IncludeExclude.class));

    public static final StreamCodec<FriendlyByteBuf, ItemMenuHostLocator> MENU_HOST_LOCATOR_STREAM_CODEC =
            StreamCodec.ofMember(
                    (locator, buf) -> MenuLocators.writeToPacket(buf, locator),
                    (buf) -> (ItemMenuHostLocator) MenuLocators.readFromPacket(buf));

    /** 记录 build() 完成的组件，供 Godofthings 构造器批量注册进 DATA_COMPONENTS。 */
    public static final Map<ResourceLocation, DataComponentType<?>> BUILT = new HashMap<>();

    /** 当前终端选择（照抄 CURRENT_TERMINAL；WTDefinition 的编解码由注册表注册时再装配）。 */
    public static final DataComponentType<String> CURRENT_TERMINAL = register("wt_current_terminal",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DataComponentType<ItemStack> SINGULARITY = register("wt_singularity", builder -> builder
            .persistent(ItemStack.OPTIONAL_CODEC).networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC));
    public static final DataComponentType<ItemContainerContents> VIEW_CELL_INVENTORY =
            register("wt_view_cell_inv", builder -> builder.persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    public static final DataComponentType<CompoundTag> PICKUP_CONFIG = register("wt_pickup_config",
            COMPOUND_TAG_CODECS);
    public static final DataComponentType<CompoundTag> INSERT_CONFIG = register("wt_insert_config",
            COMPOUND_TAG_CODECS);
    public static final DataComponentType<IncludeExclude> PICKUP_MODE = register("wt_pickup_mode",
            INCLUDE_EXCLUDE_CODECS);
    public static final DataComponentType<IncludeExclude> INSERT_MODE = register("wt_insert_mode",
            INCLUDE_EXCLUDE_CODECS);
    public static final DataComponentType<Boolean> RESTOCK = register("wt_restock", builder -> builder
            .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final DataComponentType<Boolean> PICK_BLOCK = register("wt_pick_block", builder -> builder
            .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final DataComponentType<Boolean> CRAFT_IF_MISSING = register("wt_craft_if_missing",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DataComponentType<CompoundTag> PATTERN_ENCODING_LOGIC = register("wt_pattern_encoding_logic",
            COMPOUND_TAG_CODECS);

    /** 附属无线终端列表（v7.15.5：任何 AE2 WirelessTerminalItem 子类合并进来后存这里）。 */
    public static final DataComponentType<java.util.List<ItemStack>> EXTRA_TERMINALS = register("wt_extra_terminals",
            builder -> builder.persistent(ItemStack.CODEC.listOf())
                    .networkSynchronized(ItemStack.STREAM_CODEC
                            .apply(net.minecraft.network.codec.ByteBufCodecs.list())));
    public static final DataComponentType<Integer> EXTRA_CURRENT = register("wt_extra_current",
            builder -> builder.persistent(Codec.intRange(-1, Integer.MAX_VALUE))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static <T> DataComponentType<T> register(String name,
            Consumer<DataComponentType.Builder<T>> customizer)
    {
        var builder = DataComponentType.<T>builder();
        customizer.accept(builder);
        var componentType = builder.build();
        BUILT.put(godofthingsId(name), componentType);
        return componentType;
    }

    private static boolean includeExcludeToBoolean(IncludeExclude includeExclude)
    {
        return switch (includeExclude)
        {
            case WHITELIST -> true;
            case BLACKLIST -> false;
        };
    }

    private static IncludeExclude booleanToIncludeExclude(boolean b)
    {
        return b ? IncludeExclude.WHITELIST : IncludeExclude.BLACKLIST;
    }

    public static ResourceLocation godofthingsId(String name)
    {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                com.godofthings.Godofthings.MODID, name);
    }
}
