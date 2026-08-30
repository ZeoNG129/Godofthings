package com.godofthings.torchmaster.compat;

import com.godofthings.torchmaster.common.EntityFilterRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;

public class VanillaCompat {
   public static void registerTorchEntities(EntityFilterRegistry registry) {
      ForgeRegistries.ENTITY_TYPES
         .getKeys()
         .stream()
         .map(rl -> new EntityInfoWrapper(rl, (EntityType<?>)ForgeRegistries.ENTITY_TYPES.getValue(rl)))
         .filter(e -> e.getEntityType() != null)
         .filter(e -> !e.getEntityType().getCategory().isFriendly())
         .forEach(e -> registry.registerEntity(e.getEntityName()));
   }

   public static void registerDreadLampEntities(EntityFilterRegistry registry) {
      ForgeRegistries.ENTITY_TYPES
         .getKeys()
         .stream()
         .map(rl -> new EntityInfoWrapper(rl, (EntityType<?>)ForgeRegistries.ENTITY_TYPES.getValue(rl)))
         .filter(e -> e.getEntityType() != null)
         .filter(e -> {
            MobCategory cat = e.getEntityType().getCategory();
            return cat != MobCategory.MISC && cat.isFriendly();
         })
         .forEach(e -> registry.registerEntity(e.getEntityName()));
   }
}
