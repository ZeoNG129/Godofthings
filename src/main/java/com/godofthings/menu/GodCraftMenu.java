package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodCraftBlockEntity;
import net.minecraft.core.Direction;
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
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;

/**
 * 神之合成菜单（完全照原版工作台布局）：
 * - 0-8 合成格（3×3，8,17 起始——整体右移 46px，为左侧模板槽留位）
 * - 9 结果槽（124,35，实时预览）
 * 按钮：0 = 锁定/解锁配方；1 = 启动/停止自动合成；2 = 打开面配置；3 = 打开模板详情；
 * 10-17 = 加载模板；20-27 = 保存模板（Shift）。
 */
public class GodCraftMenu extends AbstractContainerMenu
{
    // 主内容右移量（左侧模板槽面板宽度）
    private static final int SHIFT = 46;

    private final GodCraftBlockEntity be;
    private final ContainerLevelAccess access;

    public GodCraftMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv,
                (GodCraftBlockEntity) playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GodCraftMenu(int containerId, Inventory playerInv, GodCraftBlockEntity be)
    {
        super(Godofthings.GOD_CRAFT_MENU.get(), containerId);
        this.be = be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

        // 合成格 3×3（30,17 起始，同原版工作台；右移 SHIFT）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 3; col++)
            {
                this.addSlot(new SlotItemHandler(be.getInputSlots(), row * 3 + col, 30 + SHIFT + col * 18, 17 + row * 18));
            }
        }
        // 输出槽（124,35，同原版结果槽位置；右移 SHIFT）
        this.addSlot(new SlotItemHandler(be.getOutputSlot(), 0, 124 + SHIFT, 35)
        {
            // 输出槽只能取走
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return false;
            }
        });

        // 玩家物品栏（同原版；右移 SHIFT）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + SHIFT + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + SHIFT + col * 18, 142));
        }

        // 同步锁定状态
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isLocked(0) ? 1 : 0; }
            @Override public void set(int value) { be.setLocked(0, value != 0); }
        });
        // 同步启动开关
        this.addDataSlot(new DataSlot()
        {
            @Override public int get() { return be.isEnabled() ? 1 : 0; }
            @Override public void set(int value) { be.setEnabled(value != 0); }
        });
    }

    public GodCraftBlockEntity getBlockEntity()
    {
        return be;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (buttonId == 0)
        {
            be.setLocked(0, !be.isLocked(0));
            this.broadcastChanges();
            return true;
        }
        if (buttonId == 1)
        {
            be.toggleEnabled();
            this.broadcastChanges();
            return true;
        }
        if (buttonId == 2 && player instanceof ServerPlayer serverPlayer)
        {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("gui.godofthings.face_config");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new GodCraftConfigMenu(containerId, inventory, be);
                }
            }, buf -> buf.writeBlockPos(be.getBlockPos()));
            return true;
        }
        if (buttonId == 3 && player instanceof ServerPlayer serverPlayer)
        {
            NetworkHooks.openScreen(serverPlayer, new MenuProvider()
            {
                @Override
                public Component getDisplayName()
                {
                    return Component.translatable("container.godofthings.god_craft");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
                {
                    return new GodCraftTemplateMenu(containerId, inventory, be);
                }
            }, buf -> buf.writeBlockPos(be.getBlockPos()));
            return true;
        }
        // 模板槽：10-17 = 加载模板；20-27 = 保存模板（Shift+单击）
        if (buttonId >= 10 && buttonId < 10 + GodCraftBlockEntity.TEMPLATE_COUNT)
        {
            int t = buttonId - 10;
            if (!be.hasTemplate(t))
            {
                player.displayClientMessage(Component.translatable("gui.godofthings.god_craft.template_none", t + 1), true);
            }
            else if (be.loadTemplateFromInventory(player, t))
            {
                player.displayClientMessage(Component.translatable("gui.godofthings.god_craft.template_loaded", t + 1), true);
            }
            else
            {
                player.displayClientMessage(Component.translatable("gui.godofthings.god_craft.template_missing", t + 1), true);
            }
            this.broadcastChanges();
            return true;
        }
        if (buttonId >= 20 && buttonId < 20 + GodCraftBlockEntity.TEMPLATE_COUNT)
        {
            int t = buttonId - 20;
            if (be.saveTemplate(t))
            {
                player.displayClientMessage(Component.translatable("gui.godofthings.god_craft.template_saved", t + 1), true);
            }
            else
            {
                player.displayClientMessage(Component.translatable("gui.godofthings.god_craft.template_empty"), true);
            }
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_CRAFT.get());
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
                if (!this.moveItemStackTo(stack, 0, 9, false))
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
