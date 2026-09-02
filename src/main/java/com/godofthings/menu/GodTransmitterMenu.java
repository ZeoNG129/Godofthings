package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodTransmitterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 神之传输菜单：绑定器槽 + 神之加速槽 + 三个开关（跨维度/玩家充能/机器充能）+ 玩家物品栏。
 */
public class GodTransmitterMenu extends AbstractContainerMenu
{
    private final GodTransmitterBlockEntity be;
    private final ContainerLevelAccess access;

    private int cachedCrossDimension = 0;
    private int cachedPlayerEnabled = 1;
    private int cachedMachineEnabled = 1;

    public GodTransmitterMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodTransmitterBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodTransmitterMenu(int containerId, Inventory playerInv, GodTransmitterBlockEntity be)
    {
        super(Godofthings.GOD_TRANSMITTER_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // 绑定器槽 + 神之加速槽
        this.addSlot(new SlotItemHandler(be.getBinderSlot(), 0, 80, 20));
        this.addSlot(new SlotItemHandler(be.getAccelSlot(), 0, 80, 50));

        // 玩家物品栏 3x9 + 快捷栏 1x9
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

        // 三个开关状态（DataSlot 机制同步：服务端读权威、客户端读缓存）
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isCrossDimension() ? 1 : 0; }
            @Override public void set(int value) { cachedCrossDimension = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isPlayerEnabled() ? 1 : 0; }
            @Override public void set(int value) { cachedPlayerEnabled = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isMachineEnabled() ? 1 : 0; }
            @Override public void set(int value) { cachedMachineEnabled = value; }
        });
    }

    public GodTransmitterBlockEntity getBlockEntity()
    {
        return be;
    }

    /** 客户端读缓存（用于乐观渲染开关）。 */
    public boolean isCrossDimension()
    {
        return cachedCrossDimension == 1;
    }

    public boolean isPlayerEnabled()
    {
        return cachedPlayerEnabled == 1;
    }

    public boolean isMachineEnabled()
    {
        return cachedMachineEnabled == 1;
    }

    public void toggleCrossDimensionLocal()
    {
        this.cachedCrossDimension = this.cachedCrossDimension == 1 ? 0 : 1;
    }

    public void togglePlayerEnabledLocal()
    {
        this.cachedPlayerEnabled = this.cachedPlayerEnabled == 1 ? 0 : 1;
    }

    public void toggleMachineEnabledLocal()
    {
        this.cachedMachineEnabled = this.cachedMachineEnabled == 1 ? 0 : 1;
    }

    /** 按钮：0=跨维度，1=玩家充能，2=机器充能。 */
    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (buttonId >= 0 && buttonId <= 2)
        {
            switch (buttonId)
            {
                case 0 -> be.toggleCrossDimension();
                case 1 -> be.togglePlayerEnabled();
                default -> be.toggleMachineEnabled();
            }
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_TRANSMITTER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 2)
            {
                // 从绑定器/加速槽移到玩家物品栏
                if (!this.moveItemStackTo(stack, 2, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // 从玩家物品栏：先试绑定器槽(0)与加速槽(1)（isItemValid 各自过滤），再移入主物品栏
                if (!this.moveItemStackTo(stack, 0, 2, false)
                        && !this.moveItemStackTo(stack, 2, 38, false))
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
            if (stack.getCount() == result.getCount())
            {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }
}
