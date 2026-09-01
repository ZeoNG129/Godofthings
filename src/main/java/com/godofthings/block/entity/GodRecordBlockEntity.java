package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodRecordMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 神之记录方块实体：仅作 MenuProvider，无物品栏、无 tick。
 */
public class GodRecordBlockEntity extends BlockEntity implements MenuProvider
{
    public GodRecordBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_RECORD_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_record");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player)
    {
        return new GodRecordMenu(containerId, playerInv, getBlockPos());
    }
}
