package com.godofthings.godgrid;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 「整格全量搬一格」的公共搬运逻辑（v5.18.7 抽出，供砍杀机 / 吸收机 / 合成台复用）。
 *
 * <p>把 {@link LongSlotHandler} 某一格的全部存量一次性推给任意 {@link IItemHandler}：</p>
 * <ul>
 *   <li>整格取出（{@link LongSlotHandler#extractLong}）；</li>
 *   <li>目标若是本模组 {@link LongSlotHandler}：逐格 {@link LongSlotHandler#insertLong} 全量插；</li>
 *   <li>目标单格上限 &gt; 99 的其它大容量容器：整堆一次性 {@code insertItem}；</li>
 *   <li>普通容器（上限 ≤99）：按 ≤99 分片循环插入，直到搬完或该轮零进展（防死循环）；</li>
 *   <li>搬不走的余量立刻 {@link LongSlotHandler#insertLong} 写回原格 —— 绝不丢物、绝不复制。</li>
 * </ul>
 *
 * <p>六台网格机器（熔炉 / 矿机 / 资源三机 / 去皮 / 掉落 / 怪蛋）里的同名逻辑是各自的内联实现，
 * 保持原样不动；这里只为新接入的三台提供一份共享实现。</p>
 */
public final class LongSlotTransfer
{
    private LongSlotTransfer()
    {
    }

    /**
     * 把 {@code source} 第 {@code slot} 格的全部存量推给 {@code target}。
     *
     * @return 是否真的搬动了任何物品
     */
    public static boolean pushSlot(LongSlotHandler source, int slot, IItemHandler target)
    {
        if (source == null || target == null)
        {
            return false;
        }
        ItemStack key = source.key(slot);
        long available = source.amount(slot);
        if (key.isEmpty() || available <= 0L)
        {
            return false;
        }
        source.extractLong(slot, available, false);
        long remaining = available;
        boolean movedAny = false;

        if (target instanceof LongSlotHandler ls)
        {
            for (int s = 0; s < ls.getSlots() && remaining > 0L; s++)
            {
                long inserted = ls.insertLong(s, key, remaining, false);
                if (inserted > 0L)
                {
                    remaining -= inserted;
                    movedAny = true;
                }
            }
        }
        else if (target.getSlots() > 0 && target.getSlotLimit(0) > LongSlotHandler.VIEW_CAP)
        {
            ItemStack toPush = key.copyWithCount((int) Math.min(remaining, Integer.MAX_VALUE));
            for (int s = 0; s < target.getSlots() && !toPush.isEmpty(); s++)
            {
                ItemStack leftover = target.insertItem(s, toPush, false);
                int moved = toPush.getCount() - leftover.getCount();
                if (moved > 0)
                {
                    remaining -= moved;
                    movedAny = true;
                }
                toPush = leftover;
            }
        }
        else
        {
            while (remaining > 0L)
            {
                ItemStack toPush = key.copyWithCount((int) Math.min(remaining, LongSlotHandler.VIEW_CAP));
                boolean roundMoved = false;
                for (int s = 0; s < target.getSlots() && !toPush.isEmpty(); s++)
                {
                    ItemStack leftover = target.insertItem(s, toPush, false);
                    int moved = toPush.getCount() - leftover.getCount();
                    if (moved > 0)
                    {
                        remaining -= moved;
                        movedAny = true;
                        roundMoved = true;
                    }
                    toPush = leftover;
                }
                if (!roundMoved)
                {
                    break; // 这一轮零进展：目标已满，停
                }
            }
        }

        if (remaining > 0L)
        {
            source.insertLong(slot, key, remaining, false);
        }
        return movedAny;
    }
}
