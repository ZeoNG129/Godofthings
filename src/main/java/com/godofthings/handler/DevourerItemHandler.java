package com.godofthings.handler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 吞噬物品栏：暂存放入的物品（正常储存，UI 中可见），退出界面时由 {@link #clear()} 统一销毁。
 */
public class DevourerItemHandler extends ItemStackHandler
{
    public DevourerItemHandler(int slots)
    {
        super(slots);
    }

    /** 是否没有任何物品 */
    public boolean isEmpty()
    {
        for (int i = 0; i < getSlots(); i++)
        {
            if (!getStackInSlot(i).isEmpty())
            {
                return false;
            }
        }
        return true;
    }

    /** 销毁全部暂存物品 */
    public void clear()
    {
        for (int i = 0; i < getSlots(); i++)
        {
            setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
