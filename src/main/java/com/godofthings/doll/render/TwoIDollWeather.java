package com.godofthings.doll.render;

import com.godofthings.doll.entity.TwoIDollEnvironment;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class TwoIDollWeather {
   private static final WeakHashMap<Object, TwoIDollWeather.State> STATES = new WeakHashMap<>();

   private TwoIDollWeather() {
   }

   public static TwoIDollWeather.Pose sample(Object key, Level level, BlockPos pos, float partial, int salt) {
      if (level == null) {
         return TwoIDollWeather.Pose.CLEAR;
      } else {
         TwoIDollWeather.State state = STATES.computeIfAbsent(key, ignored -> new TwoIDollWeather.State());
         long now = level.getGameTime();
         if (state.level != level || now < state.last) {
            state.level = level;
            state.last = now - 1L;
            state.open = 0.0F;
         }

         if (now != state.last) {
            state.flags = TwoIDollEnvironment.conditions(level, pos);
            float step = (float)Math.min(20L, now - state.last) / 20.0F;
            state.open = Mth.clamp(state.open + ((state.flags & 1) != 0 ? step : -step), 0.0F, 1.0F);
            state.last = now;
         }

         return new TwoIDollWeather.Pose(
            state.flags, state.open, (state.flags & 2) != 0 ? TwoIDollEnvironment.sneezeProgress((double)((float)now + partial), salt) : -1.0F
         );
      }
   }

   public static record Pose(int flags, float open, float sneeze) {
      public static final TwoIDollWeather.Pose CLEAR = new TwoIDollWeather.Pose(0, 0.0F, -1.0F);
   }

   private static final class State {
      Level level;
      long last = Long.MIN_VALUE;
      float open;
      int flags;
   }
}
