package com.godofthings.doll.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public final class TwoIDollAcquisitionData extends SavedData {
   private final Set<UUID> announced = new HashSet<>();

   public static TwoIDollAcquisitionData get(MinecraftServer server) {
      return (TwoIDollAcquisitionData)server.overworld()
         .getDataStorage()
         .computeIfAbsent(new Factory<>(TwoIDollAcquisitionData::new, TwoIDollAcquisitionData::load), "godofthings_two_i_acquisitions");
   }

   public boolean markIfNew(UUID dollId) {
      if (dollId != null && this.announced.add(dollId)) {
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag ids = new ListTag();

      for (UUID id : this.announced) {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("id", id);
         ids.add(entry);
      }

      tag.put("announced", ids);
      return tag;
   }

   private static TwoIDollAcquisitionData load(CompoundTag tag, Provider registries) {
      TwoIDollAcquisitionData data = new TwoIDollAcquisitionData();
      ListTag ids = tag.getList("announced", 10);

      for (int i = 0; i < ids.size(); i++) {
         CompoundTag entry = ids.getCompound(i);
         if (entry.hasUUID("id")) {
            data.announced.add(entry.getUUID("id"));
         }
      }

      return data;
   }
}
