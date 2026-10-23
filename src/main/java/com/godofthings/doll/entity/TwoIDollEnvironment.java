package com.godofthings.doll.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.Precipitation;

public final class TwoIDollEnvironment {
   public static final int RAIN = 1;
   public static final int COLD = 2;
   public static final int THUNDER = 4;

   private TwoIDollEnvironment() {
   }

   public static int conditions(Level level, BlockPos pos) {
      if (level == null) {
         return 0;
      } else {
         Biome biome = (Biome)level.getBiome(pos).value();
         boolean weather = level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling() && biome.getPrecipitationAt(pos) != Precipitation.NONE;
         int flags = biome.coldEnoughToSnow(pos) ? 2 : 0;
         if (weather && level.isRainingAt(pos)) {
            flags |= 1;
         }

         if (weather && level.isThundering()) {
            flags |= 4;
         }

         return flags;
      }
   }

   public static float sneezeProgress(double time, int salt) {
      double phase = (double)Math.floorMod((long)Math.floor(time) + (long)salt, 420) + time - Math.floor(time);
      return phase < 36.0 ? (float)(phase / 36.0) : -1.0F;
   }
}
