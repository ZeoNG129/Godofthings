package com.godofthings.handler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 吞噬物品栏：一个黑洞 —— 任何路径（手动放入 / Shift 快捷移动 / 漏斗 / 管道 {@code insertItem}）进来的物品
 * 都在进入的同一 tick 内清零销毁，{@link #getStackInSlot} 永远返回 {@link ItemStack#EMPTY}，槽里不残留任何东西。
 * 仍保留 {@link #getSlots()} 个槽位，好让界面画出 9×6 的空格子。
 */
public class DevourerItemHandler extends ItemStackHandler
{
    public DevourerItemHandler(int slots)
    {
        super(slots);
    }

    /** 永远为空：销毁发生在插入瞬间，槽里不留东西 */
    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return ItemStack.EMPTY;
    }

    /** 写入即销毁：不进内部 storage，直接丢弃 */
    @Override
    public void setStackInSlot(int slot, ItemStack stack)
    {
        // 有意留空：内部 stacks 始终是 getSlots() 个 EMPTY
    }

    /** 插入即销毁：整堆收下并清零（返回 EMPTY = 调用方认为「已全部插入」） */
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return ItemStack.EMPTY;
    }

    /** 销毁不可逆：没有东西可以抽出来 */
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return ItemStack.EMPTY;
    }

    /** 是否没有任何物品（恒为 true：放入即销毁） */
    public boolean isEmpty()
    {
        return true;
    }

    /** 兜底清空：正常路径下槽里本来就恒为空 */
    public void clear()
    {
        for (int i = 0; i < getSlots(); i++)
        {
            stacks.set(i, ItemStack.EMPTY);
        }
    }
}
