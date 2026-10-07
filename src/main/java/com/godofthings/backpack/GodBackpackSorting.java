package com.godofthings.backpack;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 整理（排序）实现。
 *
 * <p><b>只重排「可整理槽位」</b>：记忆格与忽略整理格的内容原地不动（等于排序时把这些下标跳过），
 * 其余槽位取出内容 → 排序 → 按原下标顺序放回，所以空出来的格子会集中到末尾。</p>
 *
 * <p><b>排序键</b>（空堆叠恒排最后）：</p>
 * <ul>
 *   <li>NAME：{@link ItemStack#getHoverName()} 的字符串（中文按 Unicode 码位比较，够用）</li>
 *   <li>MOD：物品注册名的命名空间（模组 id）</li>
 *   <li>COUNT：数量<b>多 → 少</b></li>
 *   <li>TAG：物品身上的所有物品标签（{@code TagKey<Item>}）按 {@code namespace:path} 字典序排序后
 *       用 "," 连成一个字符串，于是「标签集合」本身也按字典序比 —— 稳定、可复现；
 *       没有任何标签的物品键是空串，排在所有有标签的物品前面</li>
 * </ul>
 *
 * <p>主键相同的再按「注册名 → 数量降序 → 组件串」兜底，保证是全序（同样输入每次结果一样，
 * 也让 GameTest 能断言"其余格按当前排序键有序"）。</p>
 */
public final class GodBackpackSorting
{
    private GodBackpackSorting() {}

    /** 整理：按 settings.sortBy() 重排可整理槽位（记忆格 / 忽略整理格原地不动） */
    public static void sort(GodBackpackContainer container, GodBackpackSettings settings)
    {
        List<Integer> sortable = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < GodBackpackItem.SIZE; slot++)
        {
            if (settings.isMemory(slot) || settings.isNoSort(slot))
            {
                continue;
            }
            sortable.add(slot);
            // 先拷贝再排：排序过程中会写回容器，边读边写会串味
            stacks.add(container.getItem(slot).copy());
        }

        stacks.sort(comparator(settings.sortBy()));

        for (int i = 0; i < sortable.size(); i++)
        {
            container.setItem(sortable.get(i), stacks.get(i));
        }
        container.setChanged();
    }

    /** 排序比较器：主键 + 注册名 + 数量降序 + 组件串兜底；空堆叠恒最后 */
    public static Comparator<ItemStack> comparator(SortBy sortBy)
    {
        Comparator<ItemStack> primary = switch (sortBy)
        {
            case NAME -> Comparator.comparing(GodBackpackSorting::nameKey);
            case MOD -> Comparator.comparing(GodBackpackSorting::modKey);
            case COUNT -> Comparator.comparingInt(ItemStack::getCount).reversed();
            case TAG -> Comparator.comparing(GodBackpackSorting::tagKey);
        };
        return (left, right) ->
        {
            if (left.isEmpty() || right.isEmpty())
            {
                // 空堆叠排最后（两个都空算相等）
                return left.isEmpty() && right.isEmpty() ? 0 : (left.isEmpty() ? 1 : -1);
            }
            int result = primary.compare(left, right);
            if (result != 0)
            {
                return result;
            }
            result = idKey(left).compareTo(idKey(right));
            if (result != 0)
            {
                return result;
            }
            result = Integer.compare(right.getCount(), left.getCount());
            if (result != 0)
            {
                return result;
            }
            return left.getComponents().toString().compareTo(right.getComponents().toString());
        };
    }

    /** NAME 键：物品显示名（已本地化，中文按码位比较） */
    public static String nameKey(ItemStack stack)
    {
        return stack.getHoverName().getString();
    }

    /** MOD 键：注册名的命名空间（模组 id） */
    public static String modKey(ItemStack stack)
    {
        return id(stack).getNamespace();
    }

    /** 注册名（namespace:path），排序兜底用 */
    public static String idKey(ItemStack stack)
    {
        return id(stack).toString();
    }

    /** TAG 键：标签集合按字典序拼串（规则见类注释） */
    public static String tagKey(ItemStack stack)
    {
        return stack.getTags()
                .map(tag -> tag.location().toString())
                .sorted()
                .collect(Collectors.joining(","));
    }

    private static ResourceLocation id(ItemStack stack)
    {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
