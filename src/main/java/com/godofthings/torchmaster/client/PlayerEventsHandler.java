package com.godofthings.torchmaster.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "godofthings",
   value = {Dist.CLIENT}
)
public class PlayerEventsHandler {
   @SubscribeEvent
   public static void onPlayerJoinWorldEvent(PlayerLoggedInEvent event) {
      EntityBlockingVolumeRenderer.clearAll();
   }

   @SubscribeEvent
   public static void onPlayerJoinWorldEvent(PlayerChangedDimensionEvent event) {
      EntityBlockingVolumeRenderer.clearAll();
   }
}
