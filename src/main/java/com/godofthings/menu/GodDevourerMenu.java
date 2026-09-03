package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodDevourerBlockEntity;
import com.godofthings.handler.DevourerItemHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 神之吞噬菜单：大箱子 9×6 布局，放入即销毁。
 * 方块版绑定 {@link GodDevourerBlockEntity}；便携版（背包按钮）无方块，使用独立黑洞物品栏。
 */
public class GodDevourerMenu extends AbstractContainerMenu
{
    public static final int SLOT_COUNT = 54;

    private final @Nullable GodDevourerBlockEntity be;
    private final DevourerItemHandler itemHandler;
    private final ContainerLevelAccess access;
    private int cachedAeEnabled = 1;

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
        if (!playerInv.player.level().isClientSide)
        {
            be.onMenuOpened();
        }
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isAeEnabled() ? 1 : 0; }
            @Override public void set(int value) { cachedAeEnabled = value; }
        });
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
                this.addSlot(new SlotItemHandler(itemHandler, row * 9 + col, 8 + col * 18, 18 + row * 18));
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

    public boolean isAeEnabled()
    {
        return cachedAeEnabled == 1;
    }

    // 客户端点击「AE」按钮 → ServerboundContainerButtonClickPacket(containerId, 7)
    // 服务端切换 AE 接入开关（便携版无方块，be 为 null 时忽略）
    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (buttonId == 7)
        {
            if (be != null)
            {
                be.toggleAeEnabled();
                this.broadcastChanges();
            }
            return true;
        }
        return false;
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        if (!player.level().isClientSide)
        {
            if (be != null)
            {
                be.onMenuClosed();
            }
            else
            {
                // 便携版：关闭界面销毁暂存物品
                itemHandler.clear();
            }
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
                // 吞噬槽（放入即销毁，理论上恒空）：防御性移回玩家
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
}
