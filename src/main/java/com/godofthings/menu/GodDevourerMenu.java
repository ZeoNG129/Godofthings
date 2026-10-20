package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodDevourerBlockEntity;
import com.godofthings.handler.DevourerItemHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 神之吞噬菜单：大箱子 9×6 布局，放入即销毁（销毁槽是黑洞，插入的同一 tick 内清零）。
 * 方块版绑定 {@link GodDevourerBlockEntity}；便携版（背包按钮）无方块，使用独立黑洞物品栏。
 */
public class GodDevourerMenu extends AbstractContainerMenu
{
    public static final int SLOT_COUNT = 54;

    private final @Nullable GodDevourerBlockEntity be;
    private final DevourerItemHandler itemHandler;
    private final ContainerLevelAccess access;

    // 客户端构造（方块版，从 buf 读 BlockPos）
    public GodDevourerMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodDevourerBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    // 服务端/本地构造（方块版）
    public GodDevourerMenu(int containerId, Inventory playerInv, GodDevourerBlockEntity be)
    {
        super(Godofthings.GOD_DEVOURER_MENU.get(), containerId);
        this.be = be;
        this.itemHandler = be.getItemHandler();
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        addSlots(playerInv);
    }

    // 服务端构造（便携版：背包按钮打开，无方块）
    public GodDevourerMenu(int containerId, Inventory playerInv)
    {
        super(Godofthings.PORTABLE_DEVOURER_MENU.get(), containerId);
        this.be = null;
        this.itemHandler = new DevourerItemHandler(SLOT_COUNT);
        this.access = ContainerLevelAccess.NULL;
        addSlots(playerInv);
    }

    private void addSlots(Inventory playerInv)
    {
        // 吞噬槽：9×6，放入即销毁
        for (int row = 0; row < 6; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new DevourerSlot(itemHandler, row * 9 + col, 8 + col * 18, 18 + row * 18));
            }
        }
        // 玩家物品栏 3×9 + 快捷栏 1×9
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 198));
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        if (be == null)
        {
            return true; // 便携版
        }
        return stillValid(this.access, player, Godofthings.GOD_DEVOURER.get());
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        if (!player.level().isClientSide)
        {
            // 兜底：物品本应在插入瞬间就被销毁，这里再清一次，方块版 / 便携版都是同一个黑洞物品栏
            itemHandler.clear();
        }
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
            if (index < SLOT_COUNT)
            {
                // 吞噬槽（放入即销毁，理论恒空）：防御性移回玩家
                if (!this.moveItemStackTo(stack, SLOT_COUNT, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.moveItemStackTo(stack, 0, SLOT_COUNT, false))
            {
                return ItemStack.EMPTY;
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

    @Nullable
    public GodDevourerBlockEntity getBlockEntity()
    {
        return be;
    }

    /**
     * 吞噬槽（黑洞格子）：放进去的东西在放入的同一次点击里就被销毁，槽里永远显示空。
     * 三种写入入口（set / setByPlayer / safeInsert）全部在本类覆盖，不依赖父类内部实现，
     * 手动点击放入、Shift 快捷移动、拖拽、数字键交换都走得到这里。
     */
    private static class DevourerSlot extends SlotItemHandler
    {
        DevourerSlot(IItemHandler handler, int index, int xPosition, int yPosition)
        {
            super(handler, index, xPosition, yPosition);
        }

        /** 恒空：物品从未真正落进槽里 */
        @Override
        public ItemStack getItem()
        {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean hasItem()
        {
            return false;
        }

        @Override
        public void set(ItemStack stack)
        {
            // 立即销毁
        }

        @Override
        public void setByPlayer(ItemStack newStack)
        {
            // 立即销毁
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack)
        {
            // 立即销毁
        }

        /** 放入多少就销毁多少：整堆传入 = 全部销毁，{@code amount} 限定数量时只销毁那部分（右键放 1 个只毁 1 个） */
        @Override
        public ItemStack safeInsert(ItemStack stack)
        {
            return this.safeInsert(stack, stack.getCount());
        }

        @Override
        public ItemStack safeInsert(ItemStack stack, int amount)
        {
            if (!stack.isEmpty() && amount > 0)
            {
                stack.shrink(Math.min(amount, stack.getCount()));
            }
            return stack;
        }

        @Override
        public ItemStack remove(int amount)
        {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean mayPickup(Player player)
        {
            return false;
        }
    }
}
