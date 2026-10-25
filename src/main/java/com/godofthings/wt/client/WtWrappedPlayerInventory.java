package com.godofthings.wt.client;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;

/**
 * 玩家盔甲槽位的包装库存（照抄 ae2wtlib 的 {@code WrappedPlayerInventory} 全文）。
 */
public class WtWrappedPlayerInventory implements InternalInventory
{
    private final Inventory playerInventory;

    public WtWrappedPlayerInventory(Inventory playerInventory)
    {
        this.playerInventory = playerInventory;
    }

    @Override
    public int size()
    {
        return playerInventory.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex)
    {
        return switch (slotIndex)
        {
            case 36, 37, 38, 39, Inventory.SLOT_OFFHAND -> playerInventory.getItem(slotIndex);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void setItemDirect(int slotIndex, ItemStack stack)
    {
        playerInventory.setItem(slotIndex, stack);
    }
}
