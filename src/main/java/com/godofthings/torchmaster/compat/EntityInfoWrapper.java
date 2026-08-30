package com.godofthings.torchmaster.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

class EntityInfoWrapper {
   private final ResourceLocation entityName;
   private final EntityType<?> entityType;

   EntityInfoWrapper(ResourceLocation entityName, EntityType<?> entityType) {
      this.entityName = entityName;
      this.entityType = entityType;
   }

   ResourceLocation getEntityName() {
      return this.entityName;
   }

   EntityType<?> getEntityType() {
      return this.entityType;
   }
}
