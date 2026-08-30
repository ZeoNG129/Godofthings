package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodDropBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class GodDropMenu extends AbstractContainerMenu
{
    private final GodDropBlockEntity be;
    private final ContainerLevelAccess access;

    public GodDropMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodDropBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodDropMenu(int containerId, Inventory playerInv, GodDropBlockEntity be)
    {
        super(Godofthings.GOD_DROP_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // 输入槽（只接受刷怪蛋），x=79 使槽位居中于 176 宽界面
        this.addSlot(new SlotItemHandler(be.getInputSlot(), 0, 79, 35));

        // 玩家物品栏 3x9 + 快捷栏
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }

        // 内置储存堆数（显示用）
        this.addDataSlot(new DataSlot()
        {
            @Override
            public int get() { return be.getStorageCount(); }
            @Override
            public void set(int value) { /* 只读 */ }
        });
    }

    public GodDropBlockEntity getBlockEntity()
    {
        return be;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_DROP.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index == 0)
            {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                if (!this.moveItemStackTo(stack, 0, 1, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty())
            {
                slot.setByPlayer(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
            if (stack.getCount() == itemstack.getCount())
            {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemstack;
    }
}
