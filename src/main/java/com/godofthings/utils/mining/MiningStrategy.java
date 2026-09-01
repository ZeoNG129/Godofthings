package com.godofthings.utils.mining;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.level.BlockEvent;

public interface MiningStrategy
{
    void handleBreak(BlockEvent.BreakEvent event, ItemStack item, Player player);
}
