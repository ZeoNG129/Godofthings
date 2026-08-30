package com.godofthings.torchmaster.common.logic.entityblocking;

import com.godofthings.torchmaster.Torchmaster;
import com.godofthings.torchmaster.TorchmasterConfig;
import com.godofthings.torchmaster.common.ModCaps;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;
import net.minecraftforge.event.village.VillageSiegeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "godofthings"
)
public class EntityBlockingEventHandler {
   private static boolean isIntentionalSpawn(MobSpawnType spawnType) {
      switch (spawnType) {
         case BREEDING:
         case DISPENSER:
         case BUCKET:
         case CONVERSION:
         case SPAWN_EGG:
         case TRIGGERED:
         case COMMAND:
         case EVENT:
            return true;
         case NATURAL:
         case CHUNK_GENERATION:
         case PATROL:
         case SPAWNER:
         case STRUCTURE:
         case MOB_SUMMONED:
         case REINFORCEMENT:
         case JOCKEY:
         default:
            return false;
      }
   }

   private static boolean isNaturalSpawn(MobSpawnType spawnType) {
      switch (spawnType) {
         case BREEDING:
         case DISPENSER:
         case BUCKET:
         case CONVERSION:
         case SPAWN_EGG:
         case TRIGGERED:
         case COMMAND:
         case EVENT:
         case SPAWNER:
         case STRUCTURE:
         case MOB_SUMMONED:
         case REINFORCEMENT:
         case JOCKEY:
            return false;
         case NATURAL:
         case CHUNK_GENERATION:
         case PATROL:
         default:
            return true;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onFinalizeSpawn(FinalizeSpawn event) {
      boolean log = (Boolean)TorchmasterConfig.GENERAL.logSpawnChecks.get();
      if (log) {
         Torchmaster.Log
            .debug(
               "CheckSpawn - SpawnType: {}, EntityType: {}, Pos: {}/{}/{}",
               new Object[]{event.getSpawnType(), EntityType.getKey(event.getEntity().getType()), event.getX(), event.getY(), event.getZ()}
            );
      }

      if (!event.isSpawnCancelled()) {
         if (!isIntentionalSpawn(event.getSpawnType())) {
            if ((Boolean)TorchmasterConfig.GENERAL.aggressiveSpawnChecks.get() || event.getResult() != Result.ALLOW) {
               if (!(Boolean)TorchmasterConfig.GENERAL.blockOnlyNaturalSpawns.get() || isNaturalSpawn(event.getSpawnType())) {
                  Mob entity = event.getEntity();
                  BlockPos pos = new BlockPos((int)event.getX(), (int)event.getY(), (int)event.getZ());
                  if (shouldBlockEntity(entity, pos)) {
                     event.setResult(Result.DENY);
                     event.setSpawnCancelled(true);
                     event.setCanceled(true);
                     if (log) {
                        Torchmaster.Log.debug("Blocking spawn of {}", EntityType.getKey(event.getEntity().getType()));
                     }
                  } else if (log) {
                     Torchmaster.Log.debug("Allowed spawn of {}", EntityType.getKey(event.getEntity().getType()));
                  }
               }
            }
         }
      }
   }

   public static boolean shouldBlockEntity(Entity entity, BlockPos pos) {
      Level world = entity.getCommandSenderWorld();
      return world.getCapability(ModCaps.TEB_REGISTRY).map(reg -> reg.shouldBlockEntity(entity, pos)).orElse(false);
   }

   @SubscribeEvent(
      priority = EventPriority.NORMAL
   )
   public static void onVillageSiegeEvent(VillageSiegeEvent event) {
      if ((Boolean)TorchmasterConfig.GENERAL.blockVillageSieges.get()) {
         boolean log = (Boolean)TorchmasterConfig.GENERAL.logSpawnChecks.get();
         if (log) {
            Torchmaster.Log.debug("VillageSiegeEvent - Pos: {}", event.getAttemptedSpawnPos());
         }

         if ((Boolean)TorchmasterConfig.GENERAL.aggressiveSpawnChecks.get() || event.getResult() != Result.ALLOW) {
            Vec3 vec = event.getAttemptedSpawnPos();
            BlockPos pos = new BlockPos((int)vec.x, (int)vec.y, (int)vec.z);
            Level level = event.getLevel();
            level.getCapability(ModCaps.TEB_REGISTRY).ifPresent(reg -> {
               if (reg.shouldBlockVillageSiege(pos)) {
                  event.setResult(Result.DENY);
                  if (event.isCancelable()) {
                     event.setCanceled(true);
                  }

                  if (log) {
                     Torchmaster.Log.debug("Blocking village siege @ {}", pos);
                  }
               } else if (log) {
                  Torchmaster.Log.debug("Allowed village siege @ {}", pos);
               }
            });
         }
      }
   }

   @SubscribeEvent
   public static void onWorldAttachCapabilityEvent(AttachCapabilitiesEvent<Level> event) {
      event.addCapability(ResourceLocation.tryBuild("godofthings", "registry"), new LightsRegistryCapability());
   }

   @SubscribeEvent
   public static void onGlobalTick(ServerTickEvent event) {
      if (event.side != LogicalSide.CLIENT) {
         if (event.phase == Phase.END) {
            if (Torchmaster.server == null) {
               return;
            }

            for (ServerLevel level : Torchmaster.server.getAllLevels()) {
               level.getProfiler().push("torchmaster_" + level.dimension().registry());
               level.getCapability(ModCaps.TEB_REGISTRY).ifPresent(reg -> reg.onGlobalTick(level));
               level.getProfiler().pop();
            }
         }
      }
   }
}
