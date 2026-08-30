package com.godofthings.torchmaster.common.logic.entityblocking.megatorch;

import com.godofthings.torchmaster.common.logic.entityblocking.IEntityBlockingLight;
import com.godofthings.torchmaster.common.logic.entityblocking.ILightSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

public class MegatorchSerializer implements ILightSerializer {
   public static final String SERIALIZER_KEY = "megatorch";
   public static final MegatorchSerializer INSTANCE = new MegatorchSerializer();

   private MegatorchSerializer() {
   }

   @Override
   public CompoundTag serializeLight(String lightKey, IEntityBlockingLight ilight) {
      if (ilight == null) {
         throw new IllegalArgumentException("Unable to serialize null");
      } else if (!(ilight instanceof MegatorchEntityBlockingLight light)) {
         throw new IllegalArgumentException(
            "Unable to serialize '" + ilight.getClass().getCanonicalName() + "', expected '" + MegatorchEntityBlockingLight.class.getCanonicalName() + "'"
         );
      } else {
         CompoundTag nbt = new CompoundTag();
         nbt.put("pos", NbtUtils.writeBlockPos(light.getPos()));
         return nbt;
      }
   }

   @Override
   public IEntityBlockingLight deserializeLight(String lightKey, CompoundTag nbt) {
      return new MegatorchEntityBlockingLight(NbtUtils.readBlockPos(nbt.getCompound("pos")));
   }

   @Override
   public String getSerializerKey() {
      return "megatorch";
   }
}
