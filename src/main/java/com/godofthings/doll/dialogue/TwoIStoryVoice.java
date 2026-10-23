package com.godofthings.doll.dialogue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class TwoIStoryVoice {
   private static final SoundEvent[] SOUNDS = new SoundEvent[]{
      sound("two_i_voice"), sound("two_i_voice_soft"), sound("two_i_voice_bright"), sound("player_voice")
   };
   private SimpleSoundInstance active;
   private long pulses;

   private static SoundEvent sound(String name) {
      return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("godofthings", name));
   }

   public void play(TwoIStoryTimeline.Cue cue, boolean enabled) {
      if (enabled && cue.voiced()) {
         this.stop();
         this.active = SimpleSoundInstance.forUI(SOUNDS[cue.timbre().ordinal()], cue.pitch(), cue.volume());
         Minecraft.getInstance().getSoundManager().play(this.active);
         this.pulses++;
      }
   }

   public void stop() {
      if (this.active != null) {
         Minecraft.getInstance().getSoundManager().stop(this.active);
         this.active = null;
      }
   }

   public boolean hasActiveCue() {
      return this.active != null;
   }

   public long pulses() {
      return this.pulses;
   }
}
