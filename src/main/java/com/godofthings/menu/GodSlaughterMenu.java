package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodSlaughterBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 神之砍杀菜单：功能 / 存储两个板块（按钮切换），27 格存储槽（映射无限存储）。
 */
public class GodSlaughterMenu extends AbstractContainerMenu
{
    public static final int TAB_FUNCTION = 0;
    public static final int TAB_STORAGE = 1;

    private final GodSlaughterBlockEntity be;
    private final ContainerLevelAccess access;

    // 客户端缓存（DataSlot 机制）
    private int cachedEnabled = 0;
    private int cachedRange = 16;
    private int cachedLootingEnabled = 0;
    private int cachedLooting = 100;
    private int cachedInstantKill = 1;
    private final int[] cachedFaceModes = new int[6];

    // 当前标签页（纯客户端状态）
    private int currentTab = TAB_FUNCTION;

    public GodSlaughterMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodSlaughterBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodSlaughterMenu(int containerId, Inventory playerInv, GodSlaughterBlockEntity be)
    {
        super(Godofthings.GOD_SLAUGHTER_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // 功能 DataSlot
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isEnabled() ? 1 : 0; }
            @Override public void set(int value) { cachedEnabled = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.getRange(); }
            @Override public void set(int value) { cachedRange = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isLootingEnabled() ? 1 : 0; }
            @Override public void set(int value) { cachedLootingEnabled = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.getLooting(); }
            @Override public void set(int value) { cachedLooting = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isInstantKill() ? 1 : 0; }
            @Override public void set(int value) { cachedInstantKill = value; }
        });
        for (int i = 0; i < 6; i++)
        {
            final int idx = i;
            this.addDataSlot(new DataSlot()
            {
                @Override public int get() { return be.getFaceMode(Direction.values()[idx]); }
                @Override public void set(int value) { cachedFaceModes[idx] = value; }
            });
        }

        // 27 格存储槽（映射无限存储前 27 个堆叠）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new SlotItemHandler(be.getStorageView(), row * 9 + col, 8 + col * 18, 60 + row * 18));
            }
        }

        // 玩家物品栏 3×9 + 快捷栏 1×9
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 180));
        }
    }

    public GodSlaughterBlockEntity getBlockEntity()
    {
        return be;
    }

    // ---- 客户端读缓存 ----

    public boolean isEnabled() { return cachedEnabled == 1; }
    public int getRange() { return cachedRange; }
    public boolean isLootingEnabled() { return cachedLootingEnabled == 1; }
    public int getLooting() { return cachedLooting; }
    public boolean isInstantKill() { return cachedInstantKill == 1; }
    public int getFaceMode(int dirIndex) { return cachedFaceModes[dirIndex]; }

    public int getCurrentTab() { return currentTab; }
    public void setCurrentTab(int tab) { this.currentTab = tab; }

    // ---- 客户端乐观更新 ----

    public void toggleEnabledLocal() { this.cachedEnabled = this.cachedEnabled == 1 ? 0 : 1; }
    public void toggleLootingEnabledLocal() { this.cachedLootingEnabled = this.cachedLootingEnabled == 1 ? 0 : 1; }
    public void toggleInstantKillLocal() { this.cachedInstantKill = this.cachedInstantKill == 1 ? 0 : 1; }
    public void setRangeLocal(int v) { this.cachedRange = v; }
    public void setLootingLocal(int v) { this.cachedLooting = v; }

    /**
     * 按钮：0=开关，1=抢夺开关，2=秒杀开关，3-8=六面面模式循环。
     * 范围 / 抢夺强度调节走 C2S payload（支持 Shift/Ctrl 步进手势）。
     */
    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        switch (buttonId)
        {
            case 0 -> be.toggleEnabled();
            case 1 -> be.toggleLootingEnabled();
            case 2 -> be.toggleInstantKill();
            case 3, 4, 5, 6, 7, 8 -> be.cycleFaceMode(Direction.values()[buttonId - 3]);
            default -> { return false; }
        }
        this.broadcastChanges();
        return true;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_SLAUGHTER.get());
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
            if (index < GodSlaughterBlockEntity.STORAGE_SLOTS)
            {
                // 存储槽 → 玩家物品栏
                if (!this.moveItemStackTo(stack, GodSlaughterBlockEntity.STORAGE_SLOTS, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // 玩家物品栏 → 存储槽
                if (!this.moveItemStackTo(stack, 0, GodSlaughterBlockEntity.STORAGE_SLOTS, false))
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
