package com.godofthings.doll.client;

import com.godofthings.doll.network.DollNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

@EventBusSubscriber(
   modid = "godofthings",
   value = {Dist.CLIENT}
)
public final class TwoIClientInteractionLease {
   private static ClientPacketListener connection;
   private static ClientLevel level;
   private static int entityId = -1;
   private static int ticks;

   private TwoIClientInteractionLease() {
   }

   @SubscribeEvent
   public static void tick(Post event) {
      Minecraft client = Minecraft.getInstance();
      if (client.getConnection() != connection || client.level != level) {
         connection = client.getConnection();
         level = client.level;
         entityId = -1;
         ticks = 0;
      }

      if (connection != null && level != null && client.player != null) {
         int current = TwoIInteractionScreen.entityFor(client.screen);
         if (current != entityId) {
            if (entityId >= 0) {
               DollNetwork.sendInteraction(entityId, false);
            }

            entityId = current;
            ticks = 0;
         }

         if (entityId >= 0 && ticks++ % 20 == 0) {
            DollNetwork.sendInteraction(entityId, true);
         }
      }
   }
}
