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
import net.neoforged.neoforge.items.SlotItemHandler;

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

        // 输入槽 3×3（只接受刷怪蛋）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 3; col++)
            {
                this.addSlot(new SlotItemHandler(be.getInputSlot(), row * 3 + col, 61 + col * 18, 17 + row * 18));
            }
        }
        // 神之加速槽（只接受神之加速，最多 64 个），位于输入槽左侧
        this.addSlot(new SlotItemHandler(be.getAccelSlot(), 0, 43, 35));

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
            if (index < 10)
            {
                if (!this.moveItemStackTo(stack, 10, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // 玩家物品栏：先试加速槽（只收神之加速），再试输入槽（只收刷怪蛋）
                if (!this.moveItemStackTo(stack, 9, 10, false)
                        && !this.moveItemStackTo(stack, 0, 9, false))
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
