package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.item.GodBinderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 神之传输（能量传输器）方块实体：
 * <ul>
 *   <li>绑定器槽：放入已绑定玩家的神之绑定器，为其物品栏/快捷栏/盔甲/副手中的可充能物品充能（最大速率）。</li>
 *   <li>机器充能：范围内（{@link #getRange()}，默认 64、满加速 160）接受 FE 的方块被充能。</li>
 *   <li>跨维度开关：开启后无视距离与维度给绑定玩家充能。</li>
 *   <li>玩家/机器充能独立开关；神之加速槽按数量线性放大作用范围。</li>
 * </ul>
 */
public class GodTransmitterBlockEntity extends BlockEntity implements MenuProvider
{
    public static final int BASE_RANGE = 64;
    public static final int MAX_RANGE = 160;
    public static final int MAX_ACCEL = 64;
    /** 机器充能扫描节流（每 N tick 扫一次范围）。 */
    public static final int TICKS_PER_SCAN = 10;

    private final ItemStackHandler binderSlot = new ItemStackHandler(1)
    {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack)
        {
            return stack.getItem() instanceof GodBinderItem;
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    private final ItemStackHandler accelSlot = new ItemStackHandler(1)
    {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack)
        {
            return stack.getItem() instanceof GodAcceleratorItem;
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    private boolean crossDimension = false;
    private boolean playerEnabled = true;
    private boolean machineEnabled = true;
    private int scanTimer = 0;

    public GodTransmitterBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_TRANSMITTER_BE.get(), pos, state);
    }

    public ItemStackHandler getBinderSlot()
    {
        return binderSlot;
    }

    public ItemStackHandler getAccelSlot()
    {
        return accelSlot;
    }

    public boolean isCrossDimension()
    {
        return crossDimension;
    }

    public boolean isPlayerEnabled()
    {
        return playerEnabled;
    }

    public boolean isMachineEnabled()
    {
        return machineEnabled;
    }

    public void toggleCrossDimension()
    {
        this.crossDimension = !this.crossDimension;
        setChanged();
    }

    public void togglePlayerEnabled()
    {
        this.playerEnabled = !this.playerEnabled;
        setChanged();
    }

    public void toggleMachineEnabled()
    {
        this.machineEnabled = !this.machineEnabled;
        setChanged();
    }

    public int getAccelCount()
    {
        return accelSlot.getStackInSlot(0).getCount();
    }

    /** 作用范围（格，半径）：默认 64，每 1 个神之加速 +1.5 格，满 64 个 = 160。 */
    public int getRange()
    {
        int accel = Math.min(MAX_ACCEL, getAccelCount());
        return BASE_RANGE + accel * (MAX_RANGE - BASE_RANGE) / MAX_ACCEL;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GodTransmitterBlockEntity be)
    {
        if (be.playerEnabled)
        {
            be.chargePlayer(level);
        }
        if (be.machineEnabled)
        {
            be.scanTimer++;
            if (be.scanTimer >= TICKS_PER_SCAN)
            {
                be.scanTimer = 0;
                be.chargeMachines(level, pos);
            }
        }
    }

    /** 给绑定玩家充能。 */
    private void chargePlayer(Level level)
    {
        ItemStack binder = binderSlot.getStackInSlot(0);
        if (binder.isEmpty() || level.isClientSide || level.getServer() == null)
        {
            return;
        }
        UUID owner = getBoundUUID(binder);
        if (owner == null)
        {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null)
        {
            return; // 玩家不在线
        }
        if (!crossDimension)
        {
            if (player.level().dimension() != level.dimension())
            {
                return;
            }
            int range = getRange();
            if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)
                    > (double) range * range)
            {
                return;
            }
        }

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }
            IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage != null && storage.canReceive())
            {
                storage.receiveEnergy(Integer.MAX_VALUE, false);
            }
        }
    }

    /** 给范围内的 FE 接收方块充能。 */
    private void chargeMachines(Level level, BlockPos pos)
    {
        int range = getRange();
        for (int dx = -range; dx <= range; dx++)
        {
            for (int dz = -range; dz <= range; dz++)
            {
                for (int dy = -2; dy <= 2; dy++)
                {
                    if (dx == 0 && dy == 0 && dz == 0)
                    {
                        continue;
                    }
                    BlockPos target = pos.offset(dx, dy, dz);
                    IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, target, null);
                    if (storage != null && storage.canReceive())
                    {
                        storage.receiveEnergy(Integer.MAX_VALUE, false);
                    }
                }
            }
        }
    }

    @Nullable
    private static UUID getBoundUUID(ItemStack binder)
    {
        CompoundTag tag = binder.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.hasUUID(GodBinderItem.KEY_OWNER))
        {
            return tag.getUUID(GodBinderItem.KEY_OWNER);
        }
        return null;
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        if (tag.contains("BinderSlot"))
        {
            binderSlot.deserializeNBT(registries, tag.getCompound("BinderSlot"));
        }
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(registries, tag.getCompound("AccelSlot"));
        }
        this.crossDimension = tag.getBoolean("CrossDimension");
        this.playerEnabled = tag.contains("PlayerEnabled") ? tag.getBoolean("PlayerEnabled") : true;
        this.machineEnabled = tag.contains("MachineEnabled") ? tag.getBoolean("MachineEnabled") : true;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("BinderSlot", binderSlot.serializeNBT(registries));
        tag.put("AccelSlot", accelSlot.serializeNBT(registries));
        tag.putBoolean("CrossDimension", this.crossDimension);
        tag.putBoolean("PlayerEnabled", this.playerEnabled);
        tag.putBoolean("MachineEnabled", this.machineEnabled);
    }

    // ------------------------------------------------------------------ 菜单

    @Override
    @NotNull
    public Component getDisplayName()
    {
        return Component.translatable("container.godofthings.god_transmitter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player)
    {
        return new com.godofthings.menu.GodTransmitterMenu(id, playerInventory, this);
    }
}
