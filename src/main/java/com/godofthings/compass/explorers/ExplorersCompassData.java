package com.godofthings.compass.explorers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.godofthings.Godofthings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.mojang.logging.LogUtils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;

/**
 * 探险者指南针的全局状态（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>上游这里是它自己的 mod 主类（{@code @Mod ExplorersCompass}）。本项目<b>不建第二个 {@code @Mod}</b>，
 * 所以这里只保留「服务端同步过来的结构列表 + 传送开关 + 物品引用 + 日志」这几块，物品本体在
 * {@link Godofthings#EXPLORERS_COMPASS} 里注册，网络包在 {@code network.ExplorersCompassMessages} 里注册，
 * 客户端注册在 {@code client.ExplorersCompassClient} 里注册，配置在 {@code Godofthings} 构造器里注册。</p>
 *
 * <p>物品身上的数据走 {@code minecraft:custom_data} 组件（键名与上游 NBT 完全一致：{@code StructureKey} /
 * {@code State} / {@code FoundX} / {@code FoundZ} / {@code Samples} / {@code SearchRadius} /
 * {@code DisplayCoordinates} / {@code HaveSearched}），所以存档里的旧键名不用改。</p>
 */
public final class ExplorersCompassData
{
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 结构键找不到 / 解析失败时的兜底 id（与 {@code StructureUtils} 里「没有结构集」用的是同一个） */
    public static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "none");

    /** 服务端下发的「这个玩家能不能传送」开关 */
    public static boolean canTeleport = false;

    /** 服务端下发的「可搜结构」列表（界面的列表就是它） */
    public static List<ResourceLocation> allowedStructureKeys = new ArrayList<>();

    /** 结构键 → 它能生成的维度列表 */
    public static ListMultimap<ResourceLocation, ResourceLocation> dimensionKeysForAllowedStructureKeys =
            ArrayListMultimap.create();

    /** 结构键 → 它所属的结构集（界面上叫「组」） */
    public static Map<ResourceLocation, ResourceLocation> structureKeysToTypeKeys = new HashMap<>();

    /** 结构集 → 它包含的结构键（「搜索组」按钮用） */
    public static ListMultimap<ResourceLocation, ResourceLocation> typeKeysToStructureKeys = ArrayListMultimap.create();

    private ExplorersCompassData()
    {
    }

    /** 这个物品栈是不是探险者指南针 */
    public static boolean isCompass(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getItem() == Godofthings.EXPLORERS_COMPASS.get();
    }

    /** 取物品上的自定义数据（返回的是副本，改完要 {@link #updateTag} 写回去） */
    public static CompoundTag getTag(ItemStack stack)
    {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** 改物品上的自定义数据（标签空了会自动移除组件，避免物品栏里堆无意义的数据） */
    public static void updateTag(ItemStack stack, java.util.function.Consumer<CompoundTag> action)
    {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, action);
    }
}
