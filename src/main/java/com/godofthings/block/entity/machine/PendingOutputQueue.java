package com.godofthings.block.entity.machine;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 产物溢出的内存待处理队列（v5.18.10）。
 *
 * <p>用途：机器的「输出槽 → 内置储存」都装不下的产物余量先暂存在这里，下一 tick 重试写入；
 * <b>队列非空期间机器的生产（挖掘 / 击杀 / 吸收）暂停</b>，队列写空后自动恢复。
 * 这样产物永远不会被弹成 {@code ItemEntity}（旧兜底 {@code InfiniteItemHandler#dropRemainder}
 * 在满档产能下一次就能生成上千万个掉落物把服卡死）。</p>
 *
 * <p><b>取舍：队列是纯内存态、不进存档</b> —— 拆机器 / 掉电 / 区块卸载时队列里的少量余量会消失。
 * 这是有意的：另一条路只有「掉到地上」，按要求宁可丢这一点点余量也不把产物撒进世界。
 * 实际上只有内置储存的「不同物品类型数」上限（4096 种）被塞满时才会用到它，正常游戏碰不到。</p>
 *
 * <p>同类物品在队列里合并为一条，所以 {@link #MAX_ENTRIES} 是「不同物品类型数」上限，
 * 而不是种类数无上限的堆叠表。</p>
 */
public final class PendingOutputQueue
{
    /** 队列最多暂存多少条（不同物品类型）；满了就继续暂停生产，绝不退化成掉落。 */
    public static final int MAX_ENTRIES = 64;

    private final Deque<ItemStack> entries = new ArrayDeque<>();

    public boolean isEmpty()
    {
        return entries.isEmpty();
    }

    public int size()
    {
        return entries.size();
    }

    /** 还有没有空位（能装下一个「全新物品类型」） */
    public boolean hasRoom()
    {
        return entries.size() < MAX_ENTRIES;
    }

    /** 这条物品收不收得下（已有同类可合并，或还没满） */
    public boolean canAccept(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return true;
        }
        if (hasRoom())
        {
            return true;
        }
        for (ItemStack queued : entries)
        {
            if (ItemStack.isSameItemSameComponents(queued, stack))
            {
                return queued.getCount() < Integer.MAX_VALUE;
            }
        }
        return false;
    }

    /**
     * 入队：同类合并到已有条目（数量无上限，仅防 int 溢出），否则新占一条。
     *
     * @return {@code false} = 队列已满且无法合并，调用方必须保持「暂停」而不能丢物 / 掉地
     */
    public boolean offer(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return true;
        }
        int remaining = stack.getCount();
        for (ItemStack queued : entries)
        {
            if (!ItemStack.isSameItemSameComponents(queued, stack))
            {
                continue;
            }
            int room = Integer.MAX_VALUE - queued.getCount();
            int moved = Math.min(Math.max(room, 0), remaining);
            queued.grow(moved);
            remaining -= moved;
            if (remaining <= 0)
            {
                return true;
            }
        }
        if (entries.size() >= MAX_ENTRIES)
        {
            return false;
        }
        entries.addLast(stack.copyWithCount(remaining));
        return true;
    }

    /**
     * 取走队首（已从队列移除；调用方负责写回，写不下的余量用 {@link #offer} 放回）。
     *
     * @return 队列为空时返回 {@link ItemStack#EMPTY}
     */
    public ItemStack poll()
    {
        ItemStack head = entries.pollFirst();
        return head == null ? ItemStack.EMPTY : head;
    }
}
