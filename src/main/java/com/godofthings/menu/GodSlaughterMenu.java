package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodSlaughterBlockEntity;
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
 * 神之砍杀菜单：功能 / 存储 / 经验三个板块（顶部按钮切换），27 格存储槽（映射无限存储）。
 * 面配置由单独按钮打开 {@link GodSlaughterConfigMenu}。
 */
public class GodSlaughterMenu extends AbstractContainerMenu
{
    public static final int TAB_FUNCTION = 0;
    public static final int TAB_STORAGE = 1;
    public static final int TAB_EXPERIENCE = 2;

    private final GodSlaughterBlockEntity be;
    private final ContainerLevelAccess access;

    // 客户端缓存（DataSlot 机制）
    private int cachedEnabled = 0;
    private int cachedRange = 16;
    private int cachedLootingEnabled = 0;
    private int cachedLooting = 100;
    private int cachedInstantKill = 1;
    private int cachedExperiencePoints = 0;
    private int cachedAeEnabled = 1;

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
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.getExperiencePoints(); }
            @Override public void set(int value) { cachedExperiencePoints = value; }
        });
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isAeEnabled() ? 1 : 0; }
            @Override public void set(int value) { cachedAeEnabled = value; }
        });

        // 27 格存储槽（映射无限存储前 27 个堆叠）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new SlotItemHandler(be.getStorageView(), row * 9 + col, 8 + col * 18, 17 + row * 18)
                {
                    @Override
                    public boolean isActive()
                    {
                        // 存储槽只在存储板块渲染、hover、可点击，其它板块完全独立
                        return currentTab == TAB_STORAGE;
                    }
                });
            }
        }

        // 玩家物品栏 3×9 + 快捷栏 1×9
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
    public int getExperiencePoints() { return cachedExperiencePoints; }
    public int getExperienceLevel() { return GodSlaughterBlockEntity.xpToLevel(cachedExperiencePoints); }
    public boolean isAeEnabled() { return cachedAeEnabled == 1; }

    public int getCurrentTab() { return currentTab; }
    public void setCurrentTab(int tab) { this.currentTab = tab; }

    // ---- 客户端乐观更新 ----

    public void toggleEnabledLocal() { this.cachedEnabled = this.cachedEnabled == 1 ? 0 : 1; }
    public void toggleLootingEnabledLocal() { this.cachedLootingEnabled = this.cachedLootingEnabled == 1 ? 0 : 1; }
    public void toggleInstantKillLocal() { this.cachedInstantKill = this.cachedInstantKill == 1 ? 0 : 1; }
    public void setRangeLocal(int v) { this.cachedRange = v; }
    public void setLootingLocal(int v) { this.cachedLooting = v; }

    /**
     * 按钮：0=开关，1=抢夺开关，2=秒杀开关，3=打开面配置界面，4=取1级，5=取10级，6=取100级，7=取全部经验，8=AE接入开关。
     */
    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        switch (buttonId)
        {
            case 0 -> be.toggleEnabled();
            case 1 -> be.toggleLootingEnabled();
            case 2 -> be.toggleInstantKill();
            case 3 -> openConfig(player);
            case 4 -> takeXpLevels(player, 1);
            case 5 -> takeXpLevels(player, 10);
            case 6 -> takeXpLevels(player, 100);
            case 7 -> takeXpAll(player);
            case 8 -> be.toggleAeEnabled();
            default -> { return false; }
        }
        this.broadcastChanges();
        return true;
    }

    private void openConfig(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.openMenu(new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("gui.godofthings.face_config");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new GodSlaughterConfigMenu(containerId, inventory, be);
                }
            }, buf -> buf.writeBlockPos(be.getBlockPos()));
        }
    }

    /** 取出指定等级的经验：给玩家升 N 级，从存储扣「升 N 级所需累计点数」（随玩家当前等级递增，与原版一致）。 */
    private void takeXpLevels(Player player, int levels)
    {
        int targetLevel = player.experienceLevel + levels;
        int targetTotal = GodSlaughterBlockEntity.xpToReach(targetLevel);
        int cost = Math.max(0, targetTotal - player.totalExperience);
        int pts = be.takeExperience(cost);
        if (pts > 0)
        {
            player.giveExperiencePoints(pts);
        }
    }

    private void takeXpAll(Player player)
    {
        int pts = be.takeAllExperience();
        if (pts > 0)
        {
            player.giveExperiencePoints(pts);
        }
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
                if (!this.moveItemStackTo(stack, GodSlaughterBlockEntity.STORAGE_SLOTS, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.moveItemStackTo(stack, 0, GodSlaughterBlockEntity.STORAGE_SLOTS, false))
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
            if (stack.getCount() == result.getCount())
            {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }
}
