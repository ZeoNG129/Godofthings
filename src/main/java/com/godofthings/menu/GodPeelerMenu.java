package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodPeelerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 神之去皮界面：槽 0 = 输入（只收可去皮原木）、槽 1 = 输出（只出不进）、2.. = 玩家物品栏。
 * 机器逻辑无需客户端同步值，纯 vanilla 菜单同步。
 */
public class GodPeelerMenu extends AbstractContainerMenu
{
    /** 输入槽（GUI 坐标） */
    public static final int INPUT_X = 61;
    public static final int INPUT_Y = 30;
    /** 输出槽（GUI 坐标） */
    public static final int OUTPUT_X = 97;
    public static final int OUTPUT_Y = 30;

    private final GodPeelerBlockEntity be;
    private final ContainerLevelAccess access;

    public GodPeelerMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodPeelerBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodPeelerMenu(int containerId, Inventory playerInv, GodPeelerBlockEntity be)
    {
        super(Godofthings.GOD_PEELER_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        this.addSlot(new SlotItemHandler(be.getInputSlot(), 0, INPUT_X, INPUT_Y));
        this.addSlot(new SlotItemHandler(be.getOutputSlot(), 0, OUTPUT_X, OUTPUT_Y));

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
    }

    public GodPeelerBlockEntity getBlockEntity()
    {
        return be;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_PEELER.get());
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
            if (index < 2)
            {
                // 机器槽 → 玩家物品栏（输出槽的物品也只往玩家栏走）
                if (!this.moveItemStackTo(stack, 2, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // 玩家物品栏 → 只试输入槽（输出槽 handler 拒绝插入，天然塞不进）
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
