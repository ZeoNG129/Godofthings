package com.godofthings.energy;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 创造能量立方：
 * <ul>
 *   <li>六面均暴露 FE 能量源——无限能量，输出速率恒为最大（Integer.MAX_VALUE FE/t 每面）；
 *       {@code extractEnergy} 无条件返回请求量（创造方块的标准行为）。</li>
 *   <li>每 tick 自动向相邻六个面的 FE 接收端推送能量。</li>
 *   <li>GUI 内 {@value #CHARGE_SLOTS} 个充能格，只能放入带 FE 能力的物品，
 *       每 tick 以最大速率（Integer.MAX_VALUE）为其充能。</li>
 * </ul>
 */
public class CreativeEnergyCubeEntity extends BlockEntity implements ICapabilityProvider, MenuProvider
{
    public static final int CHARGE_SLOTS = 1;

    private final ItemStackHandler items = new ItemStackHandler(CHARGE_SLOTS)
    {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack)
        {
            return hasEnergyCapability(stack);
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(CreativeEnergySource::new);
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);

    public CreativeEnergyCubeEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.CREATIVE_ENERGY_CUBE_BE.get(), pos, state);
    }

    public ItemStackHandler getItems()
    {
        return items;
    }

    public static boolean hasEnergyCapability(ItemStack stack)
    {
        return !stack.isEmpty() && stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
    }

    /** 服务端 tick：先把充能格里的物品充满，再向六个相邻面的 FE 接收端推送能量。 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativeEnergyCubeEntity be)
    {
        // 1) 充能格：以最大速率为可充电物品充能
        for (int i = 0; i < CHARGE_SLOTS; i++)
        {
            ItemStack stack = be.items.getStackInSlot(i);
            if (stack.isEmpty())
            {
                continue;
            }
            stack.getCapability(ForgeCapabilities.ENERGY).resolve()
                    .filter(IEnergyStorage::canReceive)
                    .ifPresent(storage -> storage.receiveEnergy(Integer.MAX_VALUE, false));
        }

        // 2) 对外输出：向相邻六个面的接收端推送（每面速率恒为最大）
        for (Direction dir : Direction.values())
        {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
            if (neighbor == null)
            {
                continue;
            }
            neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).resolve()
                    .filter(IEnergyStorage::canReceive)
                    .ifPresent(storage -> storage.receiveEnergy(Integer.MAX_VALUE, false));
        }
    }

    // ------------------------------------------------------------------ 能量源

    /** 创造能量源：永远满仓，提取无条件满足，不接受输入。 */
    private class CreativeEnergySource implements IEnergyStorage
    {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate)
        {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate)
        {
            return Math.max(0, maxExtract);
        }

        @Override
        public int getEnergyStored()
        {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxEnergyStored()
        {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract()
        {
            return true;
        }

        @Override
        public boolean canReceive()
        {
            return false;
        }
    }

    // ------------------------------------------------------------------ 能力

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side)
    {
        if (cap == ForgeCapabilities.ENERGY)
        {
            return energyCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER)
        {
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        energyCap.invalidate();
        itemCap.invalidate();
    }

    // ------------------------------------------------------------------ NBT

    @Override
    public void load(@Nonnull CompoundTag tag)
    {
        super.load(tag);
        if (tag.contains("Items"))
        {
            items.deserializeNBT(tag.getCompound("Items"));
        }
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
    }

    // ------------------------------------------------------------------ 菜单

    @Override
    @Nonnull
    public Component getDisplayName()
    {
        return Component.translatable("container.godofthings.creative_energy_cube");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory playerInventory, @Nonnull Player player)
    {
        return new CreativeEnergyCubeMenu(id, playerInventory, this);
    }
}
