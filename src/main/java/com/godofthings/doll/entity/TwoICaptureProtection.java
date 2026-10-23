package com.godofthings.doll.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class TwoICaptureProtection {
   private TwoICaptureProtection() {
   }

   public static boolean containsDoll(CompoundTag tag) {
      if (tag == null) {
         return false;
      } else if ("godofthings:two_i_doll".equals(tag.getString("id"))) {
         return true;
      } else {
         for (Tag passenger : tag.getList("Passengers", 10)) {
            if (containsDoll((CompoundTag)passenger)) {
               return true;
            }
         }

         return false;
      }
   }
}
