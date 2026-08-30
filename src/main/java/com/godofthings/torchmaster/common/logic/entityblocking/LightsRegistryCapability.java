package com.godofthings.torchmaster.common.logic.entityblocking;

import com.godofthings.torchmaster.Torchmaster;
import com.godofthings.torchmaster.common.ModCaps;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class LightsRegistryCapability implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
   private ITEBLightRegistry container = new LightsRegistryCapability.RegistryContainer();
   private LazyOptional optional = LazyOptional.of(() -> this.container);

   @Nonnull
   public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
      return cap == ModCaps.TEB_REGISTRY ? this.optional : LazyOptional.empty();
   }

   public CompoundTag serializeNBT() {
      return (CompoundTag)this.container.serializeNBT();
   }

   public void deserializeNBT(CompoundTag nbt) {
      this.container.deserializeNBT(nbt);
   }

   private static class RegistryContainer implements ITEBLightRegistry {
      private HashMap<String, IEntityBlockingLight> lights = new HashMap<>();
      private int tickCounter;

      public CompoundTag serializeNBT() {
         CompoundTag nbt = new CompoundTag();
         ILightSerializer serializer = null;
         String cachedSerializerKey = null;

         for (Entry<String, IEntityBlockingLight> lightEntry : this.lights.entrySet()) {
            IEntityBlockingLight light = lightEntry.getValue();
            String lightKey = lightEntry.getKey();
            String serializerKey = light.getLightSerializerKey();
            if (serializerKey == null) {
               Torchmaster.Log.error("Unable to serialize light '{}', the serializer was null", lightKey);
            } else {
               if (!serializerKey.equals(cachedSerializerKey)) {
                  serializer = LightSerializerRegistry.getLightSerializer(serializerKey);
                  cachedSerializerKey = serializerKey;
               }

               if (serializer == null) {
                  Torchmaster.Log.error("Unable to serialize light '{}', the serializer '{}' was not found", lightKey, serializerKey);
               } else {
                  try {
                     CompoundTag lightNbt = serializer.serializeLight(lightKey, light);
                     if (lightNbt == null) {
                        Torchmaster.Log.error("Unable to serialize light '{}', the serializer '{}' returned null", lightKey, serializerKey);
                     } else {
                        lightNbt.putString("lightSerializerKey", serializerKey);
                        nbt.put(lightKey, lightNbt);
                     }
                  } catch (Exception var10) {
                     Torchmaster.Log.error("The serializer '{}' threw an error during serialization!", serializerKey);
                     Torchmaster.Log.error("Error", var10);
                  }
               }
            }
         }

         return nbt;
      }

      public void deserializeNBT(CompoundTag nbt) {
         this.lights.clear();
         ILightSerializer serializer = null;
         String cachedSerializerKey = null;

         for (String lightKey : nbt.getAllKeys()) {
            CompoundTag lightNbt = nbt.getCompound(lightKey);
            String serializerKey = lightNbt.getString("lightSerializerKey");
            if (!serializerKey.equals(cachedSerializerKey)) {
               serializer = LightSerializerRegistry.getLightSerializer(serializerKey);
               cachedSerializerKey = serializerKey;
            }

            if (serializer == null) {
               Torchmaster.Log.error("Unable to deserialize the light '{}', the serializer '{}' was not found", lightKey, serializerKey);
            } else {
               try {
                  IEntityBlockingLight light = serializer.deserializeLight(lightKey, lightNbt);
                  if (light == null) {
                     Torchmaster.Log.error("Unable to deserialize the light '{}', the serializer returned null", lightKey);
                  } else {
                     this.lights.put(lightKey, light);
                  }
               } catch (Exception var9) {
                  Torchmaster.Log.error("The serializer '{}' threw an error during deserialization!", serializerKey);
                  Torchmaster.Log.error("Error", var9);
               }
            }
         }
      }

      @Override
      public boolean shouldBlockEntity(Entity entity, BlockPos pos) {
         for (Entry<String, IEntityBlockingLight> lightEntry : this.lights.entrySet()) {
            IEntityBlockingLight light = lightEntry.getValue();
            if (light.shouldBlockEntity(entity, pos)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public boolean shouldBlockVillageSiege(BlockPos pos) {
         for (Entry<String, IEntityBlockingLight> lightEntry : this.lights.entrySet()) {
            IEntityBlockingLight light = lightEntry.getValue();
            if (light.shouldBlockVillageSiege(pos)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public void registerLight(String lightKey, IEntityBlockingLight light) {
         if (lightKey == null) {
            throw new IllegalArgumentException("lightKey must not be null");
         } else if (light == null) {
            throw new IllegalArgumentException("light must not be null");
         } else {
            this.lights.put(lightKey, light);
         }
      }

      @Override
      public void unregisterLight(String lightKey) {
         this.lights.remove(lightKey);
      }

      @Nullable
      @Override
      public IEntityBlockingLight getLight(String lightKey) {
         return this.lights.get(lightKey);
      }

      @Override
      public void onGlobalTick(Level level) {
         if (this.tickCounter++ >= 200) {
            this.tickCounter = 0;
            this.lights.entrySet().removeIf(l -> l.getValue().cleanupCheck(level));
         }
      }
   }
}
