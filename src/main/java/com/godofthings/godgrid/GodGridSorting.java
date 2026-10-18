package com.godofthings.godgrid;

import com.godofthings.backpack.GodBackpackSorting;
import com.godofthings.backpack.SortBy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 网格机器输出槽的整理：先把同类合并成一堆，再按当前排序键重排，空位集中在末尾。
 *
 * <p>排序比较器直接复用背包的 {@link GodBackpackSorting#comparator(SortBy)}，
 * 保证「按名称 / 按模组 / 按数量」的观感在全模组一致。</p>
 */
public final class GodGridSorting
{
    /** 界面只轮换这三种（背包的 TAG 排序对机器输出没意义，故不提供） */
    public static final int MODE_NAME = 0;
    public static final int MODE_MOD = 1;
    public static final int MODE_COUNT = 2;
    public static final int MODE_COUNT_TOTAL = 3;

    private GodGridSorting()
    {
    }

    /** 模式序号 → 排序键 */
    public static SortBy sortBy(int mode)
    {
        return switch (mode)
        {
            case MODE_MOD -> SortBy.MOD;
            case MODE_COUNT -> SortBy.COUNT;
            default -> SortBy.NAME;
        };
    }

    /** 排序模式按钮上的字（标题语言键由界面负责，这里给按钮内单字） */
    public static String modeLabelKey(int mode)
    {
        return switch (mode)
        {
            case MODE_MOD -> "gui.godofthings.grid.sort.mod";
            case MODE_COUNT -> "gui.godofthings.grid.sort.count";
            default -> "gui.godofthings.grid.sort.name";
        };
    }

    public static int nextMode(int mode)
    {
        return (mode + 1) % MODE_COUNT_TOTAL;
    }

    /**
     * 整理 [start, start+count) 区间的槽位。
     *
     * <p>合并同类时按槽位上限拆分：一堆装不下就续到下一格，绝不丢量
     * （1.21 的 ItemStack 数量序列化上限是 99，超了会在存档/掉落时崩）。</p>
     */
    public static void sort(ItemStackHandler handler, int start, int count, int mode)
    {
        List<ItemStack> merged = new ArrayList<>();
        for (int i = 0; i < count; i++)
        {
            ItemStack stack = handler.getStackInSlot(start + i);
            if (stack.isEmpty())
            {
                continue;
            }
            int remaining = stack.getCount();
            int limit = Math.max(1, handler.getSlotLimit(start + i));
            for (int m = 0; m < merged.size() && remaining > 0; m++)
            {
                ItemStack target = merged.get(m);
                if (ItemStack.isSameItemSameComponents(target, stack))
                {
                    int room = limit - target.getCount();
                    if (room > 0)
                    {
                        int moved = Math.min(room, remaining);
                        target.grow(moved);
                        remaining -= moved;
                    }
                }
            }
            while (remaining > 0)
            {
                ItemStack part = stack.copy();
                int take = Math.min(limit, remaining);
                part.setCount(take);
                merged.add(part);
                remaining -= take;
            }
        }

        merged.sort(GodBackpackSorting.comparator(sortBy(mode)));

        for (int i = 0; i < count; i++)
        {
            ItemStack value = i < merged.size() ? merged.get(i) : ItemStack.EMPTY;
            handler.setStackInSlot(start + i, value);
        }
    }
}
