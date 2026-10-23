package com.godofthings.doll.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class TwoICompanionshipData extends SavedData {
   public static final int CHECK_INTERVAL = 100;
   public static final int REWARD_INTERVAL = 6000;
   private static final int DAILY_CAP = 2;
   private static final Factory<TwoICompanionshipData> FACTORY = new Factory<>(TwoICompanionshipData::new, TwoICompanionshipData::load);
   private final Map<UUID, TwoICompanionshipData.Progress> dolls = new HashMap<>();

   public static TwoICompanionshipData get(MinecraftServer server) {
      return (TwoICompanionshipData)server.overworld().getDataStorage().computeIfAbsent(FACTORY, "godofthings_two_i_companionship");
   }

   public TwoICompanionshipData.Progress progress(UUID doll, TwoIDollMemory legacy) {
      TwoICompanionshipData.Progress existing = this.dolls.get(doll);
      if (existing != null) {
         return existing;
      } else {
         TwoIDollMemory.Interaction old = legacy.interaction();
         TwoICompanionshipData.Progress imported = new TwoICompanionshipData.Progress(
            old.day(), old.companionship(), Math.clamp((long)old.companionTicks(), 0, 5999), legacy.lastCompanionSample()
         );
         this.dolls.put(doll, imported);
         this.setDirty();
         return imported;
      }
   }

   public TwoICompanionshipData.Sample sample(UUID doll, TwoIDollMemory legacy, long time) {
      TwoICompanionshipData.Progress stored = this.progress(doll, legacy);
      TwoICompanionshipData.Progress before = stored.forDay(time);
      boolean awarded = false;
      TwoICompanionshipData.Progress next = before;
      if (time % 100L == 0L && before.lastSample() < time && before.earned() < 2) {
         int ticks = before.ticks() + 100;
         awarded = ticks >= 6000;
         next = new TwoICompanionshipData.Progress(before.day(), before.earned() + (awarded ? 1 : 0), awarded ? ticks - 6000 : ticks, time);
      }

      if (!next.equals(stored)) {
         this.dolls.put(doll, next);
         this.setDirty();
      }

      return new TwoICompanionshipData.Sample(next, awarded);
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag list = new ListTag();

      for (Entry<UUID, TwoICompanionshipData.Progress> record : this.dolls.entrySet()) {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("doll", record.getKey());
         TwoICompanionshipData.Progress p = record.getValue();
         entry.putLong("day", p.day());
         entry.putInt("earned", p.earned());
         entry.putInt("ticks", p.ticks());
         entry.putLong("last_sample", p.lastSample());
         list.add(entry);
      }

      tag.put("dolls", list);
      return tag;
   }

   public static TwoICompanionshipData load(CompoundTag tag, Provider registries) {
      TwoICompanionshipData data = new TwoICompanionshipData();
      ListTag list = tag.getList("dolls", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag entry = list.getCompound(i);
         if (entry.hasUUID("doll")) {
            data.dolls
               .put(
                  entry.getUUID("doll"),
                  new TwoICompanionshipData.Progress(
                     Math.max(-1L, entry.getLong("day")),
                     Math.clamp((long)entry.getInt("earned"), 0, 2),
                     Math.clamp((long)entry.getInt("ticks"), 0, 5999),
                     entry.contains("last_sample") ? Math.max(-1L, entry.getLong("last_sample")) : -1L
                  )
               );
         }
      }

      return data;
   }

   public static record Progress(long day, int earned, int ticks, long lastSample) {
      private TwoICompanionshipData.Progress forDay(long time) {
         long today = Math.max(0L, time / 24000L);
         return this.day == today ? this : new TwoICompanionshipData.Progress(today, 0, 0, this.lastSample);
      }

      public TwoIDollMemory merge(TwoIDollMemory memory) {
         TwoIDollMemory.Interaction old = memory.interaction();
         long mergedDay = Math.max(old.day(), this.day);
         if (old.day() < mergedDay) {
            old = old.forDay(Math.max(0L, mergedDay) * 24000L);
         }

         return new TwoIDollMemory(
            memory.cooldowns(),
            new TwoIDollMemory.Interaction(
               mergedDay,
               old.pats(),
               old.talks(),
               old.stories(),
               this.day == mergedDay ? this.earned : 0,
               this.day == mergedDay ? this.ticks : 0,
               old.lastChat(),
               old.lastGift(),
               old.repeatedGift(),
               old.gifts()
            ),
            this.lastSample
         );
      }
   }

   public static record Sample(TwoICompanionshipData.Progress progress, boolean awarded) {
   }
}
