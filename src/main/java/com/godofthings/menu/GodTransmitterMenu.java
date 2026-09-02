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
 * 神之传输菜单：三分区（无线连接 / 玩家充能 / 权限），无玩家物品栏。
 * <ul>
 *   <li>权限：绑定器槽 + 神之加速槽。</li>
 *   <li>无线连接：机器速率（DataSlot）+ 无上限 + 跨维度 + 绑定设备数（只读）。</li>
 *   <li>玩家充能：开关 + 跨维度 + 玩家速率 + 无上限。</li>
 * </ul>
 * 速率调节（滑块 / 手动输入）由 {@code network/TransmitterMessages} 的 C2S payload 同步。
 */
public class GodTransmitterMenu extends AbstractContainerMenu
{
    public static final int SLOT_BINDER = 0;
    public static final int SLOT_ACCEL = 1;
    public static final int SLOT_COUNT = 2;

    private final GodTransmitterBlockEntity be;
    private final ContainerLevelAccess access;
    private final Inventory playerInv;

    // 客户端缓存（DataSlot 机制：get 服务端读权威、客户端读缓存；set 写缓存）
    private int cachedMachineRate = 100;
    private int cachedMachineUnlimited = 0;
    private int cachedMachineCrossDimension = 0;
    private int cachedBoundCount = 0;
    private int cachedPlayerEnabled = 1;
    private int cachedPlayerCrossDimension = 0;
    private int cachedPlayerRate = 100;
    private int cachedPlayerUnlimited = 1;

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
        this.playerInv = playerInv;

        this.addSlot(new SlotItemHandler(be.getBinderSlot(), 0, 10, 248));
        this.addSlot(new SlotItemHandler(be.getAccelSlot(), 0, 40, 248));

        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.getMachineRate(); }
            @Override public void set(int value) { cachedMachineRate = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isMachineUnlimited() ? 1 : 0; }
            @Override public void set(int value) { cachedMachineUnlimited = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isMachineCrossDimension() ? 1 : 0; }
            @Override public void set(int value) { cachedMachineCrossDimension = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.getBoundCount(); }
            @Override public void set(int value) { cachedBoundCount = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isPlayerEnabled() ? 1 : 0; }
            @Override public void set(int value) { cachedPlayerEnabled = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isPlayerCrossDimension() ? 1 : 0; }
            @Override public void set(int value) { cachedPlayerCrossDimension = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.getPlayerRate(); }
            @Override public void set(int value) { cachedPlayerRate = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isPlayerUnlimited() ? 1 : 0; }
            @Override public void set(int value) { cachedPlayerUnlimited = value; }
        });
    }

    public GodTransmitterBlockEntity getBlockEntity()
    {
        return be;
    }

    // ---- 客户端读缓存 ----

    public int getMachineRate()
    {
        return cachedMachineRate;
    }

    public boolean isMachineUnlimited()
    {
        return cachedMachineUnlimited == 1;
    }

    public boolean isMachineCrossDimension()
    {
        return cachedMachineCrossDimension == 1;
    }

    public int getBoundCount()
    {
        return cachedBoundCount;
    }

    public boolean isPlayerEnabled()
    {
        return cachedPlayerEnabled == 1;
    }

    public boolean isPlayerCrossDimension()
    {
        return cachedPlayerCrossDimension == 1;
    }

    public int getPlayerRate()
    {
        return cachedPlayerRate;
    }

    public boolean isPlayerUnlimited()
    {
        return cachedPlayerUnlimited == 1;
    }

    // ---- 客户端乐观更新 ----

    public void setMachineRateLocal(int rate)
    {
        this.cachedMachineRate = rate;
    }

    public void setPlayerRateLocal(int rate)
    {
        this.cachedPlayerRate = rate;
    }

    public void toggleMachineUnlimitedLocal()
    {
        this.cachedMachineUnlimited = this.cachedMachineUnlimited == 1 ? 0 : 1;
    }

    public void toggleMachineCrossDimensionLocal()
    {
        this.cachedMachineCrossDimension = this.cachedMachineCrossDimension == 1 ? 0 : 1;
    }

    public void togglePlayerEnabledLocal()
    {
        this.cachedPlayerEnabled = this.cachedPlayerEnabled == 1 ? 0 : 1;
    }

    public void togglePlayerCrossDimensionLocal()
    {
        this.cachedPlayerCrossDimension = this.cachedPlayerCrossDimension == 1 ? 0 : 1;
    }

    public void togglePlayerUnlimitedLocal()
    {
        this.cachedPlayerUnlimited = this.cachedPlayerUnlimited == 1 ? 0 : 1;
    }

    /** 按钮：0=机器无上限，1=机器跨维度，2=清除全部绑定，3=玩家充能开关，4=玩家跨维度，5=玩家无上限。 */
    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        switch (buttonId)
        {
            case 0 -> be.toggleMachineUnlimited();
            case 1 -> be.toggleMachineCrossDimension();
            case 2 -> be.clearAllBindings();
            case 3 -> be.togglePlayerEnabled();
            case 4 -> be.togglePlayerCrossDimension();
            case 5 -> be.togglePlayerUnlimited();
            default -> { return false; }
        }
        this.broadcastChanges();
        return true;
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
            if (index < SLOT_COUNT)
            {
                // 从绑定器/加速槽移回玩家背包
                if (!playerInv.add(stack))
                {
                    return ItemStack.EMPTY;
                }
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                return ItemStack.EMPTY;
            }
        }
        return result;
    }
}
