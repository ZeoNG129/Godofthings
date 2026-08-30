package com.godofthings.energy;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

/**
 * 创造能量立方 GUI：1 个充能格（只收带 FE 能力的物品）+ 玩家背包。
 * 无能量条数据——能量无限，输出速率恒为最大。
 */
public class CreativeEnergyCubeMenu extends AbstractContainerMenu
{
    private static final int SLOT_X_START = 79; // 单充能格，水平居中（79+9=88 正中）
    private static final int SLOT_Y = 30;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;

    @Nullable
    private final CreativeEnergyCubeEntity entity;

    public CreativeEnergyCubeMenu(int id, Inventory playerInventory, CreativeEnergyCubeEntity entity)
    {
        this(id, playerInventory, (IItemHandler) entity.getItems(), entity);
    }

    /** 网络构造：客户端按方块坐标找回方块实体。 */
    public CreativeEnergyCubeMenu(int id, Inventory playerInventory, BlockPos pos)
    {
        this(id, playerInventory, playerInventory.player.level().getBlockEntity(pos) instanceof CreativeEnergyCubeEntity be
                ? be : null);
    }

    private CreativeEnergyCubeMenu(int id, Inventory playerInventory, IItemHandler chargeHandler,
                                   @Nullable CreativeEnergyCubeEntity entity)
    {
        super(Godofthings.CREATIVE_ENERGY_CUBE_MENU.get(), id);
        this.entity = entity;

        for (int i = 0; i < CreativeEnergyCubeEntity.CHARGE_SLOTS; i++)
        {
            this.addSlot(new ChargeSlot(chargeHandler, i, SLOT_X_START + i * 18, SLOT_Y));
        }
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    @Nullable
    public CreativeEnergyCubeEntity getEntity()
    {
        return entity;
    }

    @Override
    public boolean stillValid(Player player)
    {
        Level level = entity != null ? entity.getLevel() : null;
        return level == null
                || stillValid(ContainerLevelAccess.create(level, entity.getBlockPos()), player,
                        Godofthings.CREATIVE_ENERGY_CUBE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem())
        {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        final int chargeSlots = CreativeEnergyCubeEntity.CHARGE_SLOTS;
        final int totalSlots = this.slots.size();

        if (index < chargeSlots)
        {
            // 充能格 -> 背包
            if (!this.moveItemStackTo(stack, chargeSlots, totalSlots, true))
            {
                return ItemStack.EMPTY;
            }
        }
        else
        {
            // 背包 -> 优先充能格（快速移入时按格校验），失败则背包内整理
            if (!this.moveItemStackTo(stack, 0, chargeSlots, false))
            {
                if (index < chargeSlots + 27)
                {
                    if (!this.moveItemStackTo(stack, chargeSlots + 27, totalSlots, false))
                    {
                        return ItemStack.EMPTY;
                    }
                }
                else if (!this.moveItemStackTo(stack, chargeSlots, chargeSlots + 27, false))
                {
                    return ItemStack.EMPTY;
                }
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
        return original;
    }

    /** 只允许放入带 FE 能力（可充电）的物品。 */
    private static class ChargeSlot extends SlotItemHandler
    {
        public ChargeSlot(IItemHandler handler, int index, int x, int y)
        {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return CreativeEnergyCubeEntity.hasEnergyCapability(stack);
        }
    }
}
