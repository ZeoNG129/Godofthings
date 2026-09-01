package com.godofthings.handler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 吞噬物品栏（黑洞）：任何放入的物品立即销毁，不可取出、恒为空。
 * <p>
 * insertItem 恒返回 {@link ItemStack#EMPTY}（表示「全部接收并销毁」）；
 * getStackInSlot/extractItem 恒返回 EMPTY；setStackInSlot 直接忽略（放入即销毁）。
 */
public class DevourerItemHandler implements IItemHandlerModifiable
{
    private final int slots;

    public DevourerItemHandler(int slots)
    {
        this.slots = slots;
    }

    @Override
    public int getSlots()
    {
        return slots;
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        return ItemStack.EMPTY; // 全部接收并销毁
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        return ItemStack.EMPTY; // 不可取出
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 99;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return true;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack)
    {
        // 放入即销毁，忽略
    }
}
