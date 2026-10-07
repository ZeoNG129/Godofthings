package com.godofthings.backpack;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * 记忆格的匹配与「优先插入」。
 *
 * <p><b>同类判定</b>（记忆格模板匹配、转移时的堆叠合并都用它）：</p>
 * <ul>
 *   <li>默认：{@link ItemStack#isSameItemSameComponents}（物品 + 全部组件都一致）</li>
 *   <li>开了「忽略耐久」：把 {@link DataComponents#DAMAGE} 摘掉再比（同物品、同附魔，耐久不同也算同类）</li>
 *   <li>开了「忽略 NBT」：只看物品本身（{@link ItemStack#isSameItem}）</li>
 * </ul>
 *
 * <p><b>插入顺序</b>（{@link #insert}）：记忆格优先 —— 先并入「已经有同类物品」的记忆格，
 * 再把剩下的放进「空着且模板匹配」的记忆格；然后才轮到普通格（并入同类 → 找空格）。
 * 记忆格只吃它记住的那类物品，别的东西不会占掉它。忽略整理格在存放上算普通格（它只是不参与整理）。</p>
 */
public final class GodBackpackMemory
{
    private GodBackpackMemory() {}

    /**
     * 记忆格模板与物品是否算「同类」。
     *
     * @param ignoreDurability 忽略耐久（摘掉 DAMAGE 组件再比）
     * @param ignoreNbt        忽略 NBT（只看物品本身，优先级高于 ignoreDurability）
     */
    public static boolean matches(ItemStack template, ItemStack stack, boolean ignoreDurability, boolean ignoreNbt)
    {
        if (template.isEmpty() || stack.isEmpty() || !ItemStack.isSameItem(template, stack))
        {
            return false;
        }
        if (ignoreNbt)
        {
            return true;
        }
        if (ignoreDurability)
        {
            return ItemStack.isSameItemSameComponents(withoutDamage(template), withoutDamage(stack));
        }
        return ItemStack.isSameItemSameComponents(template, stack);
    }

    /** 复制一份并摘掉耐久组件（原堆叠不动） */
    private static ItemStack withoutDamage(ItemStack stack)
    {
        ItemStack copy = stack.copy();
        copy.remove(DataComponents.DAMAGE);
        return copy;
    }

    /**
     * 把 stack 塞进背包：记忆格优先，返回剩余数量（0 = 全塞进去了）。
     * 会直接消耗传入堆叠的数量（与 {@code AbstractContainerMenu#moveItemStackTo} 同一套约定）。
     */
    public static int insert(GodBackpackContainer container, GodBackpackSettings settings, ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return 0;
        }
        int remaining = stack.getCount();
        remaining = fillMemory(container, settings, stack, remaining);
        if (remaining > 0)
        {
            remaining = fillNormal(container, settings, stack, remaining);
        }
        stack.setCount(remaining);
        return remaining;
    }

    /** 记忆格优先：① 并入已有同类堆叠 ② 放进空着的、模板匹配的记忆格 */
    private static int fillMemory(GodBackpackContainer container, GodBackpackSettings settings,
                                  ItemStack stack, int remaining)
    {
        if (settings.memory().isEmpty())
        {
            return remaining;
        }
        for (int slot = 0; slot < GodBackpackItem.SIZE && remaining > 0; slot++)
        {
            if (!isActiveMemory(settings, slot))
            {
                continue;
            }
            ItemStack existing = container.getItem(slot);
            if (!existing.isEmpty() && matches(existing, stack, settings.ignoreDurability(), settings.ignoreNbt()))
            {
                remaining = mergeInto(container, slot, existing, remaining);
            }
        }
        for (int slot = 0; slot < GodBackpackItem.SIZE && remaining > 0; slot++)
        {
            if (!isActiveMemory(settings, slot))
            {
                continue;
            }
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()
                    && matches(settings.memoryTemplate(slot), stack, settings.ignoreDurability(), settings.ignoreNbt()))
            {
                remaining = placeInto(container, slot, stack, remaining);
            }
        }
        return remaining;
    }

    /** 普通格：① 并入已有同类堆叠 ② 找空格（记忆格跳过，它只吃自己记住的东西） */
    private static int fillNormal(GodBackpackContainer container, GodBackpackSettings settings,
                                  ItemStack stack, int remaining)
    {
        for (int slot = 0; slot < GodBackpackItem.SIZE && remaining > 0; slot++)
        {
            if (settings.isMemory(slot))
            {
                continue;
            }
            ItemStack existing = container.getItem(slot);
            if (!existing.isEmpty() && matches(existing, stack, settings.ignoreDurability(), settings.ignoreNbt()))
            {
                remaining = mergeInto(container, slot, existing, remaining);
            }
        }
        for (int slot = 0; slot < GodBackpackItem.SIZE && remaining > 0; slot++)
        {
            if (settings.isMemory(slot))
            {
                continue;
            }
            if (container.getItem(slot).isEmpty())
            {
                remaining = placeInto(container, slot, stack, remaining);
            }
        }
        return remaining;
    }

    /** 记忆格且没被标记成忽略整理格 */
    private static boolean isActiveMemory(GodBackpackSettings settings, int slot)
    {
        return settings.isMemory(slot) && !settings.isNoSort(slot);
    }

    /** 把 remaining 个并进 slot 里已有的堆叠，返回剩余数量 */
    private static int mergeInto(GodBackpackContainer container, int slot, ItemStack existing, int remaining)
    {
        int limit = Math.min(container.getMaxStackSize(), existing.getMaxStackSize());
        int space = limit - existing.getCount();
        if (space <= 0)
        {
            return remaining;
        }
        int moved = Math.min(space, remaining);
        existing.grow(moved);
        container.setChanged();
        return remaining - moved;
    }

    /** 往空格里放一份（可能放不满，剩下的继续找下一个格子），返回剩余数量 */
    private static int placeInto(GodBackpackContainer container, int slot, ItemStack stack, int remaining)
    {
        int limit = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
        int moved = Math.min(limit, remaining);
        if (moved <= 0)
        {
            return remaining;
        }
        container.setItem(slot, stack.copyWithCount(moved));
        return remaining - moved;
    }
}
