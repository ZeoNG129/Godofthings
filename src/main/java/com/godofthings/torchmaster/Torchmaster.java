package com.godofthings.torchmaster;

import com.godofthings.torchmaster.common.EntityFilterRegistry;
import com.godofthings.torchmaster.common.ModBlocks;
import com.godofthings.torchmaster.common.network.ModMessageHandler;
import com.godofthings.torchmaster.compat.VanillaCompat;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

public final class Torchmaster {
   public static final String MOD_ID = "godofthings";
   public static final Logger Log = LogUtils.getLogger();
   public static MinecraftServer server;
   public static final EntityFilterRegistry MegaTorchFilterRegistry = new EntityFilterRegistry();
   public static final EntityFilterRegistry DreadLampFilterRegistry = new EntityFilterRegistry();

   private Torchmaster() {
   }

   public static void register(IEventBus modEventBus) {
      ModBlocks.init();
      ModMessageHandler.initialize();
      modEventBus.addListener(EventPriority.LOWEST, Torchmaster::postInit);
      MinecraftForge.EVENT_BUS.register(Torchmaster.class);
   }

   private static void postInit(FMLLoadCompleteEvent event) {
      VanillaCompat.registerTorchEntities(MegaTorchFilterRegistry);
      VanillaCompat.registerDreadLampEntities(DreadLampFilterRegistry);
      Log.info("Applying mega torch entity block list overrides...");
      MegaTorchFilterRegistry.applyListOverrides(TorchmasterConfig.GENERAL.megaTorchEntityBlockListOverrides.get().toArray(new String[0]));
      Log.info("Applying dread lamp entity block list overrides...");
      DreadLampFilterRegistry.applyListOverrides(TorchmasterConfig.GENERAL.dreadLampEntityBlockListOverrides.get().toArray(new String[0]));
   }

   public static List<RegistryObject<Item>> getItems() {
      return List.<RegistryObject<Item>>of(
            (RegistryObject<Item>) (RegistryObject<?>) ModBlocks.itemMegaTorch,
            (RegistryObject<Item>) (RegistryObject<?>) ModBlocks.itemDreadLamp);
   }

   @SubscribeEvent
   public static void onServerStarted(ServerStartedEvent event) {
      server = event.getServer();
   }

   @SubscribeEvent
   public static void onServerStopping(ServerStoppedEvent event) {
      server = null;
   }
}
