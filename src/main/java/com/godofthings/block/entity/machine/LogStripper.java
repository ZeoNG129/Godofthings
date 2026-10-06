package com.godofthings.block.entity.machine;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 原木 → 去皮原木 映射表（神之去皮）。
 *
 * <p>按惯例启发式扫描注册表：物品路径形如 {@code xxx_log / xxx_wood / xxx_stem / xxx_hyphae /
 * bamboo_block}（且不带 {@code stripped_} 前缀）时，取同命名空间下 {@code stripped_ + 路径} 的物品，
 * <b>目标真实存在才收录</b>。原版 11 组（9 种原木/木头 + 下界菌柄/蕈柄 + 竹块）与绝大多数
 * 按惯例命名的模组原木都会被覆盖；不同命名的模组原木不收录（保守做法，宁缺勿错）。
 * 已经是去皮变体（{@code stripped_} 前缀）的不收，所以机器天然拒绝「已去皮」的输入。</p>
 *
 * <p>映射懒构建（首次使用时注册表已就绪）。独立成类是为了 GameTest 能静态测
 * （同 {@link DropLootRoller} / {@link SpawnEggHelper} 的做法）。</p>
 */
public final class LogStripper
{
    private static volatile Map<Item, Item> stripMap;

    private LogStripper()
    {
    }

    /** 原木 → 对应去皮原木（数量保留；不是可去皮原木返回 EMPTY）。 */
    public static ItemStack strip(ItemStack input)
    {
        if (input.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        Item out = map().get(input.getItem());
        if (out == null)
        {
            return ItemStack.EMPTY;
        }
        return new ItemStack(out, input.getCount());
    }

    /** 是否为可去皮原木（神之去皮输入槽的过滤条件）。 */
    public static boolean isStrippable(ItemStack stack)
    {
        return !strip(stack).isEmpty();
    }

    /** 原木 → 去皮原木 映射（懒构建，只读）。 */
    public static Map<Item, Item> map()
    {
        Map<Item, Item> m = stripMap;
        if (m == null)
        {
            m = build();
            stripMap = m;
        }
        return m;
    }

    private static Map<Item, Item> build()
    {
        Map<Item, Item> map = new HashMap<>();
        // 惯例启发式：路径像原木且 stripped_ 孪生真实存在才收录（原版 11 组全覆盖，模组原木按惯例自动支持）
        for (Item item : BuiltInRegistries.ITEM)
        {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String path = id.getPath();
            if (path.startsWith("stripped_") || !isLogLike(path))
            {
                continue;
            }
            Item stripped = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "stripped_" + path));
            if (stripped != Items.AIR)
            {
                map.put(item, stripped);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private static boolean isLogLike(String path)
    {
        return path.equals("log") || path.equals("wood") || path.equals("stem") || path.equals("hyphae")
                || path.equals("bamboo_block")
                || path.endsWith("_log") || path.endsWith("_wood")
                || path.endsWith("_stem") || path.endsWith("_hyphae");
    }
}
