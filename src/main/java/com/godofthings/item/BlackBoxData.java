package com.godofthings.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之黑盒的数据管理：读写黑盒 ItemStack 的 CUSTOM_DATA 组件。
 * <p>
 * 四个键：开关(BlackBoxEnabled)、过滤模式(BlackBoxMode：0=白名单/1=黑名单)、
 * 过滤槽(BlackBoxFilter)、无限储存(BlackBoxStorage)。
 * 储存格式沿用 {@code InfiniteItemHandler}：每条
 * {@code {"Item": <count=1 的物品序列化>, "Count": <真实数量 int>}}，
 * 绕开 ItemStack.save 的 99 硬上限，实现「过滤命中的物品无堆叠上限保留」。
 */
public final class BlackBoxData
{
    public static final String KEY_ENABLED = "BlackBoxEnabled";
    public static final String KEY_MODE = "BlackBoxMode";
    public static final String KEY_FILTER = "BlackBoxFilter";
    public static final String KEY_STORAGE = "BlackBoxStorage";
    /** 过滤槽数量（3×3）。 */
    public static final int FILTER_SLOTS = 9;
    /** 过滤模式：白名单。 */
    public static final int MODE_WHITELIST = 0;
    /** 过滤模式：黑名单。 */
    public static final int MODE_BLACKLIST = 1;

    private BlackBoxData()
    {
    }

    private static CustomData data(ItemStack box)
    {
        return box.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
    }

    // ---- 开关 ----
    public static boolean isEnabled(ItemStack box)
    {
        return data(box).copyTag().getBoolean(KEY_ENABLED);
    }

    public static void setEnabled(ItemStack box, boolean enabled)
    {
        CustomData.update(DataComponents.CUSTOM_DATA, box, tag -> tag.putBoolean(KEY_ENABLED, enabled));
    }

    // ---- 过滤模式（白名单 / 黑名单）----
    public static boolean isWhitelistMode(ItemStack box)
    {
        return data(box).copyTag().getInt(KEY_MODE) == MODE_WHITELIST;
    }

    public static void setMode(ItemStack box, int mode)
    {
        CustomData.update(DataComponents.CUSTOM_DATA, box, tag -> tag.putInt(KEY_MODE, mode));
    }

    // ---- 白名单过滤槽 ----
    public static List<ItemStack> getFilter(ItemStack box, HolderLookup.Provider provider)
    {
        List<ItemStack> result = new ArrayList<>();
        CompoundTag tag = data(box).copyTag();
        if (tag.contains(KEY_FILTER, Tag.TAG_LIST))
        {
            ListTag list = tag.getList(KEY_FILTER, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && result.size() < FILTER_SLOTS; i++)
            {
                ItemStack s = ItemStack.parseOptional(provider, list.getCompound(i));
                if (!s.isEmpty())
                {
                    result.add(s);
                }
            }
        }
        return result;
    }

    public static void setFilter(ItemStack box, List<ItemStack> filter, HolderLookup.Provider provider)
    {
        ListTag list = new ListTag();
        for (ItemStack s : filter)
        {
            if (!s.isEmpty())
            {
                list.add(s.save(provider));
            }
        }
        CustomData.update(DataComponents.CUSTOM_DATA, box, tag -> tag.put(KEY_FILTER, list));
    }

    // ---- 无限储存（同类恒单堆、数量无上限）----
    public static void addToStorage(ItemStack box, ItemStack toAdd, HolderLookup.Provider provider)
    {
        if (toAdd.isEmpty())
        {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, box, tag ->
        {
            ListTag list = tag.contains(KEY_STORAGE, Tag.TAG_LIST)
                    ? tag.getList(KEY_STORAGE, Tag.TAG_COMPOUND)
                    : new ListTag();
            if (!tag.contains(KEY_STORAGE, Tag.TAG_LIST))
            {
                tag.put(KEY_STORAGE, list);
            }

            ItemStack unit = toAdd.copy();
            unit.setCount(1);
            boolean merged = false;
            for (int i = 0; i < list.size(); i++)
            {
                CompoundTag entry = list.getCompound(i);
                ItemStack existing = ItemStack.parseOptional(provider, entry.getCompound("Item"));
                // 按物品类型合并（同类恒单堆、数量无上限），避免 components 差异导致同类无法累加
                if (ItemStack.isSameItem(existing, unit))
                {
                    long total = (long) entry.getInt("Count") + toAdd.getCount();
                    entry.putInt("Count", (int) Math.min(Integer.MAX_VALUE, total));
                    merged = true;
                    break;
                }
            }
            if (!merged)
            {
                CompoundTag entry = new CompoundTag();
                entry.put("Item", unit.save(provider));
                entry.putInt("Count", toAdd.getCount());
                list.add(entry);
            }
        });
    }

    /** 储存的物品总数（用于 tooltip 显示）。 */
    public static int getStorageCount(ItemStack box)
    {
        CompoundTag tag = data(box).copyTag();
        if (!tag.contains(KEY_STORAGE, Tag.TAG_LIST))
        {
            return 0;
        }
        ListTag list = tag.getList(KEY_STORAGE, Tag.TAG_COMPOUND);
        int total = 0;
        for (int i = 0; i < list.size(); i++)
        {
            total += list.getCompound(i).getInt("Count");
        }
        return total;
    }

    /**
     * 拾取物品是否保留（存入黑盒）：
     * 过滤槽为空 → 全部保留；
     * 白名单模式 → 仅过滤槽内物品保留，其余销毁；
     * 黑名单模式 → 过滤槽内物品销毁，其余保留。
     */
    public static boolean shouldKeep(ItemStack box, ItemStack stack, HolderLookup.Provider provider)
    {
        List<ItemStack> filter = getFilter(box, provider);
        if (filter.isEmpty())
        {
            return true;
        }
        boolean inFilter = false;
        for (ItemStack f : filter)
        {
            if (stack.is(f.getItem()))
            {
                inFilter = true;
                break;
            }
        }
        return isWhitelistMode(box) ? inFilter : !inFilter;
    }
}
