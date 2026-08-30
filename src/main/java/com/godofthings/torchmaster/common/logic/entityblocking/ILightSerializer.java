package com.godofthings.torchmaster.common.logic.entityblocking;

import net.minecraft.nbt.CompoundTag;

public interface ILightSerializer {
   CompoundTag serializeLight(String var1, IEntityBlockingLight var2);

   IEntityBlockingLight deserializeLight(String var1, CompoundTag var2);

   String getSerializerKey();
}
