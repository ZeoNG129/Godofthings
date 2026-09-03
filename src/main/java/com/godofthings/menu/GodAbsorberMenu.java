package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodAbsorberBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 神之吸收菜单：功能 / 存储 / 经验三个板块（顶部按钮切换），27 格存储槽，面配置由按钮打开独立界面。
 */
public class GodAbsorberMenu extends AbstractContainerMenu
{
    public static final int TAB_FUNCTION = 0;
    public static final int TAB_STORAGE = 1;
    public static final int TAB_EXPERIENCE = 2;

    private final GodAbsorberBlockEntity be;
    private final ContainerLevelAccess access;

    private int cachedEnabled = 0;
    private int cachedRange = 16;
    private int cachedExperiencePoints = 0;
    private int cachedAeEnabled = 1;
    private int currentTab = TAB_FUNCTION;

    public GodAbsorberMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodAbsorberBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodAbsorberMenu(int containerId, Inventory playerInv, GodAbsorberBlockEntity be)
    {
        super(Godofthings.GOD_ABSORBER_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        this.addDataSlot(new DataSlot() { @Override public int get() { return be.isEnabled() ? 1 : 0; } @Override public void set(int v) { cachedEnabled = v; } });
        this.addDataSlot(new DataSlot() { @Override public int get() { return be.getRange(); } @Override public void set(int v) { cachedRange = v; } });
        this.addDataSlot(new DataSlot() { @Override public int get() { return be.getExperiencePoints(); } @Override public void set(int v) { cachedExperiencePoints = v; } });
        this.addDataSlot(new DataSlot() { @Override public int get() { return be.isAeEnabled() ? 1 : 0; } @Override public void set(int v) { cachedAeEnabled = v; } });

        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new SlotItemHandler(be.getStorageView(), row * 9 + col, 8 + col * 18, 17 + row * 18)
                {
                    @Override
                    public boolean isActive() { return currentTab == TAB_STORAGE; }
                });
            }
        }

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

    public GodAbsorberBlockEntity getBlockEntity() { return be; }

    public boolean isEnabled() { return cachedEnabled == 1; }
    public int getRange() { return cachedRange; }
    public int getExperiencePoints() { return cachedExperiencePoints; }
    public int getExperienceLevel() { return GodAbsorberBlockEntity.xpToLevel(cachedExperiencePoints); }
    public boolean isAeEnabled() { return cachedAeEnabled == 1; }
    public int getCurrentTab() { return currentTab; }
    public void setCurrentTab(int tab) { this.currentTab = tab; }

    public void toggleEnabledLocal() { this.cachedEnabled = this.cachedEnabled == 1 ? 0 : 1; }
    public void toggleAeEnabledLocal() { this.cachedAeEnabled = this.cachedAeEnabled == 1 ? 0 : 1; }
    public void setRangeLocal(int v) { this.cachedRange = v; }

    /** 按钮：0=开关，1=面配置，2=取1级，3=取10级，4=取100级，5=取全部，6=AE开关。 */
    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        switch (buttonId)
        {
            case 0 -> be.toggleEnabled();
            case 1 -> openConfig(player);
            case 2 -> takeXp(player, 1);
            case 3 -> takeXp(player, 10);
            case 4 -> takeXp(player, 100);
            case 5 -> takeXp(player, Integer.MAX_VALUE);
            case 6 -> be.toggleAeEnabled();
            default -> { return false; }
        }
        this.broadcastChanges();
        return true;
    }

    private void openConfig(Player player)
    {
        if (player instanceof ServerPlayer sp)
        {
            sp.openMenu(new MenuProvider()
            {
                @Override public Component getDisplayName() { return Component.translatable("gui.godofthings.face_config"); }
                @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new GodAbsorberConfigMenu(id, inv, be); }
            }, buf -> buf.writeBlockPos(be.getBlockPos()));
        }
    }

    private void takeXp(Player player, int levels)
    {
        int pts;
        if (levels >= Integer.MAX_VALUE)
        {
            pts = be.takeAllExperience();
        }
        else
        {
            int targetLevel = player.experienceLevel + levels;
            int targetTotal = GodAbsorberBlockEntity.xpToReach(targetLevel);
            pts = be.takeExperience(Math.max(0, targetTotal - player.totalExperience));
        }
        if (pts > 0) player.giveExperiencePoints(pts);
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_ABSORBER.get());
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
            if (index < GodAbsorberBlockEntity.STORAGE_SLOTS)
            {
                if (!this.moveItemStackTo(stack, GodAbsorberBlockEntity.STORAGE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            }
            else if (!this.moveItemStackTo(stack, 0, GodAbsorberBlockEntity.STORAGE_SLOTS, false))
            {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }
}
