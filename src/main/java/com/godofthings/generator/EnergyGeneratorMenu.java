package com.godofthings.generator;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * FE 能量发电机容器。
 * <p>
 * 包含加速槽（0）、充电槽（1）以及玩家背包。
 * 通过数据槽把能量、发电量、下次增长量、无线充电参数、六面开关等同步到客户端用于 GUI 展示，
 * 并在 GUI 中通过按钮（clickMenuButton）修改每台发电机的独立配置。
 */
public class EnergyGeneratorMenu extends AbstractContainerMenu
{
    // 按钮 ID
    public static final int BUTTON_WIRELESS = 0;
    public static final int BUTTON_INTERVAL_DOWN = 1;
    public static final int BUTTON_INTERVAL_UP = 2;
    public static final int BUTTON_RANGE_DOWN = 3;
    public static final int BUTTON_RANGE_UP = 4;
    public static final int BUTTON_REPEAT_DOWN = 5;
    public static final int BUTTON_REPEAT_UP = 6;
    public static final int BUTTON_TRANSFER_DOWN = 7;
    public static final int BUTTON_TRANSFER_UP = 8;
    public static final int BUTTON_TRANSFER_NORTH = 9;
    public static final int BUTTON_TRANSFER_SOUTH = 10;
    public static final int BUTTON_TRANSFER_WEST = 11;
    public static final int BUTTON_TRANSFER_EAST = 12;

    public final EnergyGeneratorEntity entity;

    // 客户端展示数据（服务端通过数据槽同步而来）
    private long clientEnergy;
    private long clientOutput;
    private long clientNextIncrease;
    private int clientTickCount;
    private int clientSecond;
    private boolean clientWirelessOn;
    private int clientInterval;
    private int clientRange;
    private int clientRepeat;
    private boolean clientTransferDown;
    private boolean clientTransferUp;
    private boolean clientTransferNorth;
    private boolean clientTransferSouth;
    private boolean clientTransferWest;
    private boolean clientTransferEast;

    public EnergyGeneratorMenu(int id, Inventory playerInventory, BlockPos pos)
    {
        super(Godofthings.ENERGY_GENERATOR_MENU.get(), id);
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        this.entity = (EnergyGeneratorEntity) blockEntity;

        // 机器槽位：0=加速，1=充电
        addSlot(new SlotItemHandler(entity.starSlot, 0, 8, 190));
        addSlot(new SlotItemHandler(entity.chargeSlot, 0, 152, 190));
        addPlayerInventory(playerInventory);

        // 数据同步（long 拆成高低 32 位两个数据槽）
        addDataSlot(makeDataSlot(() -> hiWord(entity.energy), v -> clientEnergy = mergeLong(v, loWord(clientEnergy))));
        addDataSlot(makeDataSlot(() -> loWord(entity.energy), v -> clientEnergy = mergeLong(hiWord(clientEnergy), v)));
        addDataSlot(makeDataSlot(() -> hiWord(entity.output), v -> clientOutput = mergeLong(v, loWord(clientOutput))));
        addDataSlot(makeDataSlot(() -> loWord(entity.output), v -> clientOutput = mergeLong(hiWord(clientOutput), v)));
        addDataSlot(makeDataSlot(() -> hiWord(entity.nextIncrease), v -> clientNextIncrease = mergeLong(v, loWord(clientNextIncrease))));
        addDataSlot(makeDataSlot(() -> loWord(entity.nextIncrease), v -> clientNextIncrease = mergeLong(hiWord(clientNextIncrease), v)));
        addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, entity.tickCount), v -> clientTickCount = v));
        addDataSlot(makeDataSlot(() -> (int) Math.min(Integer.MAX_VALUE, EnergyGenConfig.SECOND), v -> clientSecond = v));
        addDataSlot(makeDataSlot(() -> entity.wirelessOn ? 1 : 0, v -> clientWirelessOn = v != 0));
        addDataSlot(makeDataSlot(() -> entity.wirelessInterval, v -> clientInterval = v));
        addDataSlot(makeDataSlot(() -> EnergyGenTool.normalizeWirelessRange(entity.wirelessRange), v -> clientRange = v));
        addDataSlot(makeDataSlot(() -> entity.transferRepeat, v -> clientRepeat = v));
        addDataSlot(makeDataSlot(() -> entity.transferDown ? 1 : 0, v -> clientTransferDown = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferUp ? 1 : 0, v -> clientTransferUp = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferNorth ? 1 : 0, v -> clientTransferNorth = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferSouth ? 1 : 0, v -> clientTransferSouth = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferWest ? 1 : 0, v -> clientTransferWest = v != 0));
        addDataSlot(makeDataSlot(() -> entity.transferEast ? 1 : 0, v -> clientTransferEast = v != 0));
    }

    public long getEnergy()
    {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.energy : clientEnergy;
    }

    public long getOutput()
    {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.output : clientOutput;
    }

    public long getNextIncrease()
    {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.nextIncrease : clientNextIncrease;
    }

    public long getMax()
    {
        return EnergyGenConfig.MAX;
    }

    public int getTickCount()
    {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? (int) Math.min(Integer.MAX_VALUE, entity.tickCount) : clientTickCount;
    }

    public int getSecond()
    {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? (int) Math.min(Integer.MAX_VALUE, EnergyGenConfig.SECOND) : clientSecond;
    }

    public boolean isWirelessOn()
    {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.wirelessOn : clientWirelessOn;
    }

    public int getInterval()
    {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.wirelessInterval : clientInterval;
    }

    public int getRange()
    {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? EnergyGenTool.normalizeWirelessRange(entity.wirelessRange) : clientRange;
    }

    public int getRepeat()
    {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide ? entity.transferRepeat : clientRepeat;
    }

    public boolean isFaceEnabled(Direction direction)
    {
        if (entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide)
        {
            return entity.isTransferEnabled(direction);
        }
        return switch (direction)
        {
            case DOWN -> clientTransferDown;
            case UP -> clientTransferUp;
            case NORTH -> clientTransferNorth;
            case SOUTH -> clientTransferSouth;
            case WEST -> clientTransferWest;
            case EAST -> clientTransferEast;
        };
    }

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int id)
    {
        if (entity == null || player.level().isClientSide)
        {
            return false;
        }
        switch (id)
        {
            case BUTTON_WIRELESS -> entity.wirelessOn = !entity.wirelessOn;
            case BUTTON_INTERVAL_DOWN -> entity.wirelessInterval = Math.max(1, entity.wirelessInterval - 1);
            case BUTTON_INTERVAL_UP -> entity.wirelessInterval = Math.min(3600, entity.wirelessInterval + 1);
            case BUTTON_RANGE_DOWN -> entity.wirelessRange = stepRange(entity.wirelessRange, -1);
            case BUTTON_RANGE_UP -> entity.wirelessRange = stepRange(entity.wirelessRange, 1);
            case BUTTON_REPEAT_DOWN -> entity.transferRepeat = Math.max(1, entity.transferRepeat - 1);
            case BUTTON_REPEAT_UP -> entity.transferRepeat = Math.min(256, entity.transferRepeat + 1);
            case BUTTON_TRANSFER_DOWN -> entity.transferDown = !entity.transferDown;
            case BUTTON_TRANSFER_UP -> entity.transferUp = !entity.transferUp;
            case BUTTON_TRANSFER_NORTH -> entity.transferNorth = !entity.transferNorth;
            case BUTTON_TRANSFER_SOUTH -> entity.transferSouth = !entity.transferSouth;
            case BUTTON_TRANSFER_WEST -> entity.transferWest = !entity.transferWest;
            case BUTTON_TRANSFER_EAST -> entity.transferEast = !entity.transferEast;
            default -> {
                return false;
            }
        }
        entity.setChanged();
        return true;
    }

    /** 区块范围步进：1 -> 3 -> 5 -> 7 -> 1 循环（7x7 为新增的最大档位） */
    private static int stepRange(int current, int delta)
    {
        if (delta > 0)
        {
            return current >= 7 ? 1 : current + 2;
        }
        return current <= 1 ? 7 : current - 2;
    }

    @Override
    public boolean stillValid(@Nonnull Player player)
    {
        if (entity == null)
        {
            return false;
        }
        return entity.getLevel() != null && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index)
    {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            itemStack = stack.copy();
            if (index < 2)
            {
                // 机器槽 -> 玩家背包
                if (!this.moveItemStackTo(stack, 2, 38, true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else
            {
                // 玩家背包 -> 优先充电槽，其次加速槽，其余留在背包
                if (!this.moveItemStackTo(stack, 1, 2, false))
                {
                    if (!this.moveItemStackTo(stack, 0, 1, false))
                    {
                        return ItemStack.EMPTY;
                    }
                }
            }
            if (stack.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
            if (stack.getCount() == itemStack.getCount())
            {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return itemStack;
    }

    private void addPlayerInventory(Inventory playerInventory)
    {
        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 230 + i * 18));
            }
        }
        for (int i = 0; i < 9; ++i)
        {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 284));
        }
    }

    /** 加速槽所需物品 */
    @Nonnull
    public Item getStarItem()
    {
        return EnergyGenConfig.STAR_ITEM;
    }

    private static DataSlot makeDataSlot(IntSupplier getter, IntConsumer setter)
    {
        return new DataSlot()
        {
            @Override
            public int get()
            {
                return getter.getAsInt();
            }

            @Override
            public void set(int value)
            {
                setter.accept(value);
            }
        };
    }

    private static int hiWord(long value)
    {
        return (int) (value >> 32);
    }

    private static int loWord(long value)
    {
        return (int) (value & 0xFFFFFFFFL);
    }

    private static long mergeLong(int hi, int lo)
    {
        return ((long) hi << 32) | (lo & 0xFFFFFFFFL);
    }
}
