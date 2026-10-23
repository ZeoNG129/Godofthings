package com.godofthings.doll.entity;

import net.minecraft.world.entity.Entity;

public final class TwoILifecycleGuard implements AutoCloseable {
   private static final ThreadLocal<Entity> CURRENT = new ThreadLocal<>();
   private final Entity previous = CURRENT.get();

   private TwoILifecycleGuard(Entity entity) {
      CURRENT.set(entity);
   }

   public static TwoILifecycleGuard allow(Entity entity) {
      return new TwoILifecycleGuard(entity);
   }

   public static boolean allows(Entity entity) {
      return CURRENT.get() == entity;
   }

   public static boolean blocks(Object entity) {
      if (entity instanceof TwoIDollEntity doll && !doll.level().isClientSide && doll.isAddedToLevel() && !doll.isRemoved() && CURRENT.get() != doll) {
         return true;
      }

      return false;
   }

   @Override
   public void close() {
      if (this.previous == null) {
         CURRENT.remove();
      } else {
         CURRENT.set(this.previous);
      }
   }
}
