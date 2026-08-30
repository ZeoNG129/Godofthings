package com.godofthings.torchmaster.common.logic.entityblocking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface IEntityBlockingLight {
   boolean shouldBlockEntity(Entity var1, BlockPos var2);

   boolean shouldBlockVillageSiege(BlockPos var1);

   String getLightSerializerKey();

   String getName();

   BlockPos getPos();

   boolean cleanupCheck(Level var1);
}
