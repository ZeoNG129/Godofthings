package com.godofthings.infinitecell;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.items.AEBaseItem;
import appeng.items.storage.StorageCellTooltipComponent;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.NotNull;

/**
 * 「万物元件」—— 一个元件同时无限供应多种固定资源。
 *
 * <p>取代原来的 4 个单资源无限元件（无限水 / 熔岩 / 圆石 / 空白样板），
 * 并把 **福禄伊克斯色 ME 玻璃线缆**（{@code ae2:fluix_glass_cable}）一并纳入，共 5 种。</p>
 *
 * <p><b>容量口径</b>：向 AE2 报告 {@link Long#MAX_VALUE}（9,223,372,036,854,775,807 ≈ <b>9.2E</b>），
 * 这是 AE2 {@code KeyCounter} 的 long 上限，也是界面上能显示的最大单位（AE2 的单位表是
 * {@code KMGTPE}，最大只到 E）。多个元件叠加时靠 {@code KeyCounterMixin} 把 long 钳制在 MAX、
 * 真实值转 {@link InfinityBigStorage} 的 BigInteger 记账，所以不会溢出成负数。</p>
 *
 * <p>物品本身不存数据（与原来的 4 个元件一致）：只按固定清单「凭空供应」，
 * 往元件里插入同类资源会被接受但不做实际存储（幻影存储）。</p>
 */
public class UniversalCellItem extends AEBaseItem
{
    /** 本元件供应的资源清单（延迟解析，避免在注册阶段提前取 AE2 物品/流体） */
    private final Supplier<List<AEKey>> types;
    @Nullable
    private List<AEKey> records;

    public UniversalCellItem(@NotNull Supplier<List<AEKey>> types, Properties properties)
    {
        super(properties);
        this.types = types;
    }

    /** 本元件供应的全部资源（首次使用时解析并缓存） */
    public List<AEKey> getRecords()
    {
        if (this.records == null)
        {
            this.records = this.types.get();
        }
        return this.records;
    }

    /** 供应的数量口径：AE2 的 long 上限 9.2E */
    public static long getCapacity()
    {
        return Long.MAX_VALUE;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack is)
    {
        return Component.translatable("item.godofthings.universal_cell");
    }

    @NotNull
    @Override
    public Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack)
    {
        // 预览图：把 5 种资源各显示一次（数量用 9.2E 口径）
        List<GenericStack> content = this.getRecords().stream()
                .map(key -> new GenericStack(key, getCapacity()))
                .toList();
        return Optional.of(new StorageCellTooltipComponent(List.of(), content, false, true));
    }

    // tooltip 统一由 GodTooltipHandler（ItemTooltipEvent）按 lore / desc / shift_N 注入，
    // 这里不再重复添加，避免同一条文案出现两遍。
}
