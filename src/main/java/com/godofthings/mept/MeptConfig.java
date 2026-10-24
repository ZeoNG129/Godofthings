package com.godofthings.mept;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;

/**
 * 放置工具的物品匹配与 NBT 白名单逻辑（照抄上游 Config 的静态部分）。
 * <p>
 * [本地改动] 上游的 6 个能量配置项（容量/每次消耗）已随「不消耗能源」要求删除；
 * NBT 白名单不再走 TOML 配置文件，改为固定默认值（ae2:facade 保留 NBT 匹配，与上游默认一致）。
 */
public final class MeptConfig
{
    private static final Set<String> NBT_WHITELIST_MODS = Set.of();
    private static final Set<String> NBT_WHITELIST_ITEMS = Set.of("ae2:facade");

    private MeptConfig()
    {
    }

    /** 该模组 ID 是否在 NBT 白名单内（照抄上游 isModInNbtWhitelist） */
    public static boolean isModInNbtWhitelist(String modId)
    {
        return NBT_WHITELIST_MODS.contains(modId.toLowerCase());
    }

    /** 该物品是否在 NBT 白名单内（照抄上游 isItemInNbtWhitelist） */
    public static boolean isItemInNbtWhitelist(Item item)
    {
        if (NBT_WHITELIST_ITEMS.isEmpty())
        {
            return false;
        }

        var itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null)
        {
            return false;
        }

        String itemIdStr = itemId.toString().toLowerCase();
        String modId = itemId.getNamespace().toLowerCase();

        if (NBT_WHITELIST_ITEMS.contains(itemIdStr))
        {
            return true;
        }

        String wildcard = modId + ":*";
        return NBT_WHITELIST_ITEMS.contains(wildcard);
    }

    /** 该物品在 ME 网络匹配时是否忽略 NBT（照抄上游 shouldIgnoreNbt） */
    public static boolean shouldIgnoreNbt(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return false;
        }

        if (isItemInNbtWhitelist(stack.getItem()))
        {
            return false;
        }

        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null)
        {
            return false;
        }

        // 特例：AE 饰面永远保留 NBT（照抄上游）
        try
        {
            if (stack.getItem() instanceof appeng.api.implementations.items.IFacadeItem)
            {
                return false;
            }
        }
        catch (Throwable ignored)
        {
        }

        return !isModInNbtWhitelist(itemId.getNamespace());
    }

    /** 找出 ME 网络里与目标物品匹配的全部 AEItemKey（照抄上游 findAllMatchingKeys） */
    public static List<Map.Entry<AEItemKey, Long>> findAllMatchingKeys(MEStorage storage, ItemStack target)
    {
        List<Map.Entry<AEItemKey, Long>> result = new ArrayList<>();

        if (target == null || target.isEmpty())
        {
            return result;
        }

        var itemId = BuiltInRegistries.ITEM.getKey(target.getItem());
        if (itemId == null)
        {
            var key = AEItemKey.of(target);
            if (key != null)
            {
                long count = storage.extract(key, Long.MAX_VALUE, appeng.api.config.Actionable.SIMULATE, null);
                if (count > 0)
                {
                    result.add(Map.entry(key, count));
                }
            }
            return result;
        }

        boolean ignoreNbt = shouldIgnoreNbt(target);
        var targetItem = target.getItem();
        var availableStacks = storage.getAvailableStacks();

        if (ignoreNbt)
        {
            for (var entry : availableStacks)
            {
                var key = entry.getKey();
                if (key instanceof AEItemKey itemKey)
                {
                    if (itemKey.getItem() == targetItem && entry.getLongValue() > 0)
                    {
                        result.add(Map.entry(itemKey, entry.getLongValue()));
                    }
                }
            }
        }
        else
        {
            var exactKey = AEItemKey.of(target);
            if (exactKey != null)
            {
                for (var entry : availableStacks)
                {
                    var key = entry.getKey();
                    if (key instanceof AEItemKey itemKey)
                    {
                        if (itemKey.equals(exactKey) && entry.getLongValue() > 0)
                        {
                            result.add(Map.entry(itemKey, entry.getLongValue()));
                        }
                    }
                }
            }
        }

        return result;
    }

    /** ME 网络里与目标物品匹配的总数（照抄上游 getTotalMatchingCount） */
    public static long getTotalMatchingCount(MEStorage storage, ItemStack target)
    {
        return findAllMatchingKeys(storage, target).stream()
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    /** 从 ME 网络里找一个与目标物品匹配的 AEItemKey（照抄上游 findMatchingKey） */
    public static AEItemKey findMatchingKey(MEStorage storage, ItemStack target)
    {
        var matches = findAllMatchingKeys(storage, target);
        return matches.isEmpty() ? null : matches.get(0).getKey();
    }
}
