package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodTransmitterBlockEntity;
import com.godofthings.network.TransmitterMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之传输菜单：四标签页（无线连接 / 玩家充能 / 权限 / 已绑定机器），无槽位。
 * 速率调节由 {@code TransmitterMessages} C2S payload 同步，玩家绑定 / 机器列表由 S2C payload 同步。
 */
public class GodTransmitterMenu extends AbstractContainerMenu
{
    public static final int TAB_WIRELESS = 0;
    public static final int TAB_PLAYER = 1;
    public static final int TAB_PERMISSION = 2;
    public static final int TAB_BOUND = 3;

    private final GodTransmitterBlockEntity be;
    private final ContainerLevelAccess access;

    // 客户端缓存（DataSlot 机制）
    private int cachedMachineRate = 100;
    private int cachedMachineUnlimited = 0;
    private int cachedMachineCrossDimension = 0;
    private int cachedBoundCount = 0;
    private int cachedPlayerEnabled = 1;
    private int cachedPlayerCrossDimension = 0;
    private int cachedPlayerRate = 100;
    private int cachedPlayerUnlimited = 1;

    // 客户端列表缓存（S2C payload 同步）
    private List<String> cachedOnline = new ArrayList<>();
    private List<String> cachedBoundPlayers = new ArrayList<>();
    private List<String> cachedBoundMachines = new ArrayList<>();

    // 当前标签页（纯客户端 UI 状态，不同步）
    private int currentTab = TAB_WIRELESS;

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

        // 服务端：打开菜单时发送在线玩家 + 已绑定玩家 + 已绑定机器列表
        if (playerInv.player instanceof ServerPlayer sp && !sp.level().isClientSide)
        {
            TransmitterMessages.sendList(sp, be);
        }
    }

    public GodTransmitterBlockEntity getBlockEntity()
    {
        return be;
    }

    // ---- 客户端读缓存 ----

    public int getMachineRate() { return cachedMachineRate; }
    public boolean isMachineUnlimited() { return cachedMachineUnlimited == 1; }
    public boolean isMachineCrossDimension() { return cachedMachineCrossDimension == 1; }
    public int getBoundCount() { return cachedBoundCount; }
    public boolean isPlayerEnabled() { return cachedPlayerEnabled == 1; }
    public boolean isPlayerCrossDimension() { return cachedPlayerCrossDimension == 1; }
    public int getPlayerRate() { return cachedPlayerRate; }
    public boolean isPlayerUnlimited() { return cachedPlayerUnlimited == 1; }

    // ---- 列表 ----

    public List<String> getOnline() { return cachedOnline; }
    public List<String> getBoundPlayers() { return cachedBoundPlayers; }
    public List<String> getBoundMachines() { return cachedBoundMachines; }

    public void setList(List<String> online, List<String> boundPlayers, List<String> boundMachines)
    {
        this.cachedOnline = online;
        this.cachedBoundPlayers = boundPlayers;
        this.cachedBoundMachines = boundMachines;
    }

    // ---- 标签页 ----

    public int getCurrentTab() { return currentTab; }
    public void setCurrentTab(int tab) { this.currentTab = tab; }

    // ---- 客户端乐观更新 ----

    public void setMachineRateLocal(int rate) { this.cachedMachineRate = rate; }
    public void setPlayerRateLocal(int rate) { this.cachedPlayerRate = rate; }
    public void toggleMachineUnlimitedLocal() { this.cachedMachineUnlimited = this.cachedMachineUnlimited == 1 ? 0 : 1; }
    public void toggleMachineCrossDimensionLocal() { this.cachedMachineCrossDimension = this.cachedMachineCrossDimension == 1 ? 0 : 1; }
    public void togglePlayerEnabledLocal() { this.cachedPlayerEnabled = this.cachedPlayerEnabled == 1 ? 0 : 1; }
    public void togglePlayerCrossDimensionLocal() { this.cachedPlayerCrossDimension = this.cachedPlayerCrossDimension == 1 ? 0 : 1; }
    public void togglePlayerUnlimitedLocal() { this.cachedPlayerUnlimited = this.cachedPlayerUnlimited == 1 ? 0 : 1; }

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
        if (player instanceof ServerPlayer sp)
        {
            TransmitterMessages.sendList(sp, be);
        }
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
        return ItemStack.EMPTY;
    }
}
