package com.godofthings.torchmaster.common.logic.entityblocking;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;

public interface ITEBLightRegistry extends INBTSerializable<CompoundTag> {
   boolean shouldBlockEntity(Entity var1, BlockPos var2);

   boolean shouldBlockVillageSiege(BlockPos var1);

   void registerLight(String var1, IEntityBlockingLight var2);

   void unregisterLight(String var1);

   IEntityBlockingLight getLight(String var1);

   void onGlobalTick(Level var1);
}
