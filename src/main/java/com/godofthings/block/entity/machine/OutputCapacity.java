package com.godofthings.block.entity.machine;

import com.godofthings.block.entity.InfiniteItemHandler;
import com.godofthings.godgrid.LongSlotHandler;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 产出容量预判（v5.18.9）：机器每轮生产<b>之前</b>先判断这一批产物能不能全部存下，
 * 存不下就整轮跳过——绝不「部分产出 + 把余量掉到世界里」。
 *
 * <p>为什么需要：{@code GodGridLayout#roundBatch} 把满 64 个神之加速时的单轮处理量提到 int 上限，
 * 单轮产物可能远超输出槽余量；旧版兜底会 {@code InfiniteItemHandler#dropRemainder} 到机器上方，
 * 满档下一次就是上千万个 ItemEntity，直接把服卡死。</p>
 *
 * <p>判定顺序与真实写入顺序一致：先输出槽（同类格累加 → 空槽按单格容量），
 * 再内置无限储存（{@link InfiniteItemHandler}，按「不同物品类型数」上限判定）。
 * 所有输出槽的单格容量相同（{@link LongSlotHandler#DEFAULT_CAPACITY}），
 * 所以「按槽位顺序模拟分配」与机器实际的写入顺序（配对槽优先等）判定结论一致。</p>
 */
public final class OutputCapacity
{
    private OutputCapacity()
    {
    }

    /**
     * 这一批产物能否全部存下（只做预判，不写入任何东西）。
     *
     * @param handler  机器网格槽，可用输出区是 {@code [start, start + count)}
     * @param start    输出区起始槽
     * @param count    输出槽数量
     * @param products 本轮产物（数量已按并行倍率算好）
     * @param inner    内置兜底储存；没有内置储存的机器传 {@code null}（= 只有输出槽可用）
     * @return {@code true} = 全部放得下；{@code false} = 放不下，调用方应跳过本轮
     */
    public static boolean fits(LongSlotHandler handler, int start, int count,
                               List<ItemStack> products, @Nullable InfiniteItemHandler inner)
    {
        if (handler == null || products == null || products.isEmpty())
        {
            return true;
        }
        int size = Math.max(0, count);
        // 1) 快照输出区：每格的物品身份 + 剩余容量（不写入真实槽位）
        long[] room = new long[size];
        ItemStack[] keys = new ItemStack[size];
        for (int i = 0; i < size; i++)
        {
            int slot = start + i;
            room[i] = handler.room(slot);
            keys[i] = handler.isEmpty(slot) ? ItemStack.EMPTY : handler.key(slot);
        }
        // 2) 逐个产物模拟分配：(a) 同类格累加；(b) 空槽按容量；都吃不下才算「溢出到兜底储存」
        List<ItemStack> overflow = new ArrayList<>();
        for (ItemStack product : products)
        {
            if (product == null || product.isEmpty())
            {
                continue;
            }
            long need = product.getCount();
            for (int i = 0; i < size && need > 0L; i++)
            {
                if (keys[i].isEmpty() || !ItemStack.isSameItemSameComponents(keys[i], product))
                {
                    continue;
                }
                long take = Math.min(room[i], need);
                room[i] -= take;
                need -= take;
            }
            for (int i = 0; i < size && need > 0L; i++)
            {
                if (!keys[i].isEmpty())
                {
                    continue;
                }
                long take = Math.min(room[i], need);
                if (take <= 0L)
                {
                    continue;
                }
                // 空槽一旦被占用就变成「同类格」，后面的同种产物可以接着往里累加
                keys[i] = product.copyWithCount(1);
                room[i] -= take;
                need -= take;
            }
            if (need > 0L)
            {
                overflow.add(product.copyWithCount((int) Math.min(Integer.MAX_VALUE, need)));
            }
        }
        if (overflow.isEmpty())
        {
            return true;
        }
        // 3) 输出槽放不下的部分：内置无限储存还收得下吗（它也受「物品类型数」上限约束）
        return inner != null && inner.canAcceptAll(overflow);
    }
}
