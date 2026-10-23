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

public final class TwoIStoryData extends SavedData {
   private final Map<UUID, TwoIStoryData.Progress> players = new HashMap<>();

   public static TwoIStoryData get(MinecraftServer server) {
      return (TwoIStoryData)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory<>(TwoIStoryData::new, TwoIStoryData::load), "godofthings_two_i_story");
   }

   public boolean metBefore(UUID player) {
      return this.players.containsKey(player);
   }

   public int completedMask(UUID player) {
      TwoIStoryData.Progress progress = this.players.get(player);
      return progress == null ? 0 : progress.completedMask() & 63;
   }

   public void complete(UUID player) {
      this.complete(player, 0);
   }

   public int nextChapter(UUID player, int bond) {
      TwoIStoryData.Progress progress = this.players.get(player);
      return progress == null ? 0 : TwoIStorySession.nextChapterFor(bond, progress.completedMask(), progress.lastChapter());
   }

   public void complete(UUID player, int chapter) {
      if (chapter >= 0 && chapter <= 5) {
         TwoIStoryData.Progress old = this.players.getOrDefault(player, new TwoIStoryData.Progress(0, -1));
         this.players.put(player, new TwoIStoryData.Progress(old.completedMask() | 1 << chapter, chapter));
         this.setDirty();
      }
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag savedPlayers = new ListTag();

      for (Entry<UUID, TwoIStoryData.Progress> player : this.players.entrySet()) {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("player", player.getKey());
         entry.putInt("completed", player.getValue().completedMask());
         entry.putInt("last", player.getValue().lastChapter());
         savedPlayers.add(entry);
      }

      tag.put("met_players", savedPlayers);
      return tag;
   }

   private static TwoIStoryData load(CompoundTag tag, Provider registries) {
      TwoIStoryData data = new TwoIStoryData();
      ListTag players = tag.getList("met_players", 10);

      for (int index = 0; index < players.size(); index++) {
         CompoundTag entry = players.getCompound(index);
         if (entry.hasUUID("player")) {
            data.players
               .put(
                  entry.getUUID("player"),
                  new TwoIStoryData.Progress(entry.contains("completed") ? entry.getInt("completed") & 63 : 1, Math.max(0, Math.min(5, entry.getInt("last"))))
               );
         }
      }

      return data;
   }

   private static record Progress(int completedMask, int lastChapter) {
   }
}
