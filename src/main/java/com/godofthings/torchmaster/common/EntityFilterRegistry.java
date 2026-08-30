package com.godofthings.torchmaster.common;

import com.godofthings.torchmaster.Torchmaster;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public class EntityFilterRegistry {
   private Set<ResourceLocation> registry = new HashSet<>();

   public boolean containsEntity(ResourceLocation entityName) {
      return this.registry.contains(entityName);
   }

   public void registerEntity(ResourceLocation entityName) {
      this.registry.add(entityName);
   }

   public void applyListOverrides(String[] overrides) {
      for (String override : overrides) {
         if (override.length() >= 4) {
            char prefix = override.charAt(0);
            ResourceLocation rl = ResourceLocation.tryParse(override.substring(1));
            if (rl != null) {
               switch (prefix) {
                  case '+':
                     if (!this.containsEntity(rl)) {
                        if (!ForgeRegistries.ENTITY_TYPES.containsKey(rl)) {
                           Torchmaster.Log.warn("  The entity '{}' does not exist, skipping", rl);
                        } else {
                           this.registerEntity(rl);
                           Torchmaster.Log.info("  Added '{}' to the block list", rl);
                        }
                     }
                     break;
                  case '-':
                     if (this.registry.removeIf(rrl -> rrl.equals(rl))) {
                        Torchmaster.Log.info("  Removed '{}' from the block list", rl);
                     }
                     break;
                  default:
                     Torchmaster.Log.warn("  Invalid block list prefix: '{}', only + and - are valid prefixes", prefix);
               }
            }
         }
      }
   }

   public ResourceLocation[] getRegisteredEntities() {
      return this.registry.toArray(new ResourceLocation[0]);
   }
}
