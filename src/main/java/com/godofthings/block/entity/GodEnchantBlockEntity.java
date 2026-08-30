package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodEnchantMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 神之附魔台：单个物品槽，任何物品都能被任意附魔（无需条件、自选等级）。
 */
public class GodEnchantBlockEntity extends BlockEntity implements MenuProvider
{
    private final ItemStackHandler itemHandler = new ItemStackHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> cap = LazyOptional.of(() -> itemHandler);

    public GodEnchantBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_ENCHANT_BE.get(), pos, state);
    }

    public ItemStackHandler getItemHandler()
    {
        return itemHandler;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side)
    {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
        {
            return this.cap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        cap.invalidate();
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_enchant");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        // 天神附魔方块 → 选择附魔默认最高等级
        boolean heavenly = this.getBlockState().is(com.godofthings.Godofthings.GOD_HEAVEN_ENCHANT.get());
        return new GodEnchantMenu(containerId, inventory, this, heavenly);
    }
}
