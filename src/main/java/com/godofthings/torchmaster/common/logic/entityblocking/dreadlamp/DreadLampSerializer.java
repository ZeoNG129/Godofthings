package com.godofthings.torchmaster.common.logic.entityblocking.dreadlamp;

import com.godofthings.torchmaster.common.logic.entityblocking.IEntityBlockingLight;
import com.godofthings.torchmaster.common.logic.entityblocking.ILightSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

public class DreadLampSerializer implements ILightSerializer {
   public static final String SERIALIZER_KEY = "dreadlamp";
   public static final DreadLampSerializer INSTANCE = new DreadLampSerializer();

   private DreadLampSerializer() {
   }

   @Override
   public CompoundTag serializeLight(String lightKey, IEntityBlockingLight ilight) {
      if (ilight == null) {
         throw new IllegalArgumentException("Unable to serialize null");
      } else if (!(ilight instanceof DreadLampEntityBlockingLight light)) {
         throw new IllegalArgumentException(
            "Unable to serialize '" + ilight.getClass().getCanonicalName() + "', expected '" + DreadLampEntityBlockingLight.class.getCanonicalName() + "'"
         );
      } else {
         CompoundTag nbt = new CompoundTag();
         nbt.put("pos", NbtUtils.writeBlockPos(light.getPos()));
         return nbt;
      }
   }

   @Override
   public IEntityBlockingLight deserializeLight(String lightKey, CompoundTag nbt) {
      return new DreadLampEntityBlockingLight(NbtUtils.readBlockPos(nbt.getCompound("pos")));
   }

   @Override
   public String getSerializerKey() {
      return "dreadlamp";
   }
}
