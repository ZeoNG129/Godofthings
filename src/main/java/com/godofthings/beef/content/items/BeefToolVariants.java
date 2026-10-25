package com.godofthings.beef.content.items;

import com.godofthings.beef.api.enums.tool.ToolTypeMode;
import com.godofthings.beef.core.component.UComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具形态切换（扳手 / 螺丝刀 / 软锤 / 撬棍 / 铁锤）。
 *
 * <p><b>与上游的差异（本项目 v5.1.2 起）</b>：上游为每种形态注册了**独立的物品**
 * （{@code endless_beaf_wrench} 等 5 个）外加基础形态与关闭扳手标记形态，切形态时
 * <b>换掉手上的物品</b>。本项目只保留一个杖物品「荒辰移晷之杖」，因此这里改为
 * <b>不换物品、只改物品上的 {@code CurrentToolTypeComponent} 数据组件</b> ——
 * 工具形态的能力本来就由该组件决定（见 {@link EndlessBeafItem}），所以
 * 5 种形态的能力全部保留，只是不再各占一个物品/注册名。</p>
 *
 * <p>同理，{@code withWrenchTag} 上游是在「基础形态」与「关闭扳手标记形态」两个物品之间切换，
 * 这里改为只写 {@code WrenchTagEnabledComponent} 组件。</p>
 */
public final class BeefToolVariants {
    private BeefToolVariants() {}

    /** 任何继承了 {@link EndlessBeafItem} 的物品（即本项目的杖）都算基础形态。 */
    public static boolean isBaseVariant(ItemStack stack) {
        return stack.getItem() instanceof EndlessBeafItem;
    }

    /**
     * 判断物品是否为杖（荒辰移晷之杖）。
     * <p>
     * 凡属于 {@link EndlessBeafItem} 的物品都算 —— 本项目只剩荒辰移晷之杖一个物品，
     * 它继承自 {@code EndlessBeafItem}，故这里与 {@link #isBaseVariant} 等价。
     */
    public static boolean isBeafTool(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof EndlessBeafItem;
    }

    /**
     * 枚举杖物品。
     * <p>
     * 以 {@link EndlessBeafItem} 类型为准直接从注册表收集，保证与
     * {@link #isBeafTool(ItemStack)} 的判定范围始终一致（需要枚举具体物品的场景如模具 Ingredient、
     * JEI 展示等不会漏项）。
     */
    public static List<Item> allVariantItems() {
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof EndlessBeafItem) {
                items.add(item);
            }
        }
        return List.copyOf(items);
    }

    public static boolean isWrenchTagEnabled(ItemStack stack) {
        return stack.getOrDefault(UComponents.WrenchTagEnabledComponent.get(), true);
    }

    /**
     * 开关「扳手标签」：上游会换成另一个物品，这里只在同一个物品上写组件。
     */
    public static ItemStack withWrenchTag(ItemStack source, boolean enabled) {
        ItemStack result = source.copy();
        result.set(UComponents.WrenchTagEnabledComponent.get(), enabled);
        result.set(UComponents.CurrentToolTypeComponent.get(), ToolTypeMode.NONE_MODE);
        return result;
    }

    /**
     * 切换工具形态：上游按形态换成对应的独立物品，这里只在同一个物品上写
     * {@code CurrentToolTypeComponent}。{@code OMNITOOL_MODE} 仍与上游一致地返回空
     * （它由「全能工具」模组提供，见 {@code ToolTypeModeSwitchPacket}）。
     */
    public static ItemStack createForToolMode(ItemStack source, ToolTypeMode mode) {
        ItemStack result = switch (mode) {
            case NONE_MODE -> withWrenchTag(source, isWrenchTagEnabled(source));
            case WRENCH_MODE, SCREWDRIVER_MODE, MALLET_MODE, CROWBAR_MODE, HAMMER_MODE -> source.copy();
            case OMNITOOL_MODE -> ItemStack.EMPTY;
        };
        if (!result.isEmpty()) {
            result.set(UComponents.CurrentToolTypeComponent.get(), mode);
        }
        return result;
    }
}
