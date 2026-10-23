package com.godofthings.doll.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record TwoIDollMemory(TwoIDollMemory.Cooldowns cooldowns, TwoIDollMemory.Interaction interaction, long lastCompanionSample) {
   public static final Codec<TwoIDollMemory> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               TwoIDollMemory.Cooldowns.CODEC.fieldOf("cooldowns").forGetter(TwoIDollMemory::cooldowns),
               TwoIDollMemory.Interaction.CODEC.fieldOf("interaction").forGetter(TwoIDollMemory::interaction),
               Codec.LONG.optionalFieldOf("last_companion_sample", -1L).forGetter(TwoIDollMemory::lastCompanionSample)
            )
            .apply(instance, TwoIDollMemory::new)
   );

   public TwoIDollMemory(TwoIDollMemory.Cooldowns cooldowns, TwoIDollMemory.Interaction interaction) {
      this(cooldowns, interaction, -1L);
   }

   public static TwoIDollMemory empty() {
      return new TwoIDollMemory(new TwoIDollMemory.Cooldowns(0, 0, 0, 0, 0, 0, -1, false), TwoIDollMemory.Interaction.empty());
   }

   public static record Cooldowns(int pat, int gift, int talk, int prank, int timeStop, int panic, int lastPrank, boolean panicDragonSpawned) {
      public static final Codec<TwoIDollMemory.Cooldowns> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.intRange(0, 24000).fieldOf("pat").forGetter(TwoIDollMemory.Cooldowns::pat),
                  Codec.intRange(0, 24000).fieldOf("gift").forGetter(TwoIDollMemory.Cooldowns::gift),
                  Codec.intRange(0, 24000).fieldOf("talk").forGetter(TwoIDollMemory.Cooldowns::talk),
                  Codec.intRange(0, 24000).fieldOf("prank").forGetter(TwoIDollMemory.Cooldowns::prank),
                  Codec.intRange(0, 24000).fieldOf("time_stop").forGetter(TwoIDollMemory.Cooldowns::timeStop),
                  Codec.intRange(0, 24000).fieldOf("panic").forGetter(TwoIDollMemory.Cooldowns::panic),
                  Codec.intRange(-1, 11).fieldOf("last_prank").forGetter(TwoIDollMemory.Cooldowns::lastPrank),
                  Codec.BOOL.fieldOf("panic_dragon_spawned").forGetter(TwoIDollMemory.Cooldowns::panicDragonSpawned)
               )
               .apply(instance, TwoIDollMemory.Cooldowns::new)
      );
   }

   public static record Interaction(
      long day, int pats, int talks, int stories, int companionship, int companionTicks, int lastChat, String lastGift, int repeatedGift, int gifts
   ) {
      public static final Codec<TwoIDollMemory.Interaction> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.LONG.fieldOf("day").forGetter(TwoIDollMemory.Interaction::day),
                  Codec.intRange(0, 2).fieldOf("pats").forGetter(TwoIDollMemory.Interaction::pats),
                  Codec.intRange(0, 2).fieldOf("talks").forGetter(TwoIDollMemory.Interaction::talks),
                  Codec.intRange(0, 3).fieldOf("stories").forGetter(TwoIDollMemory.Interaction::stories),
                  Codec.intRange(0, 2).fieldOf("companionship").forGetter(TwoIDollMemory.Interaction::companionship),
                  Codec.intRange(0, 12000).fieldOf("companion_ticks").forGetter(TwoIDollMemory.Interaction::companionTicks),
                  Codec.intRange(-1, 41).fieldOf("last_chat").forGetter(TwoIDollMemory.Interaction::lastChat),
                  Codec.STRING.fieldOf("last_gift").forGetter(TwoIDollMemory.Interaction::lastGift),
                  Codec.intRange(0, 4).fieldOf("repeated_gift").forGetter(TwoIDollMemory.Interaction::repeatedGift),
                  Codec.intRange(0, 1).optionalFieldOf("gifts", 0).forGetter(TwoIDollMemory.Interaction::gifts)
               )
               .apply(instance, TwoIDollMemory.Interaction::new)
      );

      public Interaction(long day, int pats, int talks, int stories, int companionship, int companionTicks, int lastChat, String lastGift, int repeatedGift) {
         this(day, pats, talks, stories, companionship, companionTicks, lastChat, lastGift, repeatedGift, 0);
      }

      public static TwoIDollMemory.Interaction empty() {
         return new TwoIDollMemory.Interaction(-1L, 0, 0, 0, 0, 0, -1, "", 0);
      }

      public TwoIDollMemory.Interaction forDay(long gameTime) {
         long today = Math.max(0L, gameTime / 24000L);
         return today == this.day ? this : new TwoIDollMemory.Interaction(today, 0, 0, 0, 0, 0, this.lastChat, "", 0);
      }
   }
}
