package com.godofthings.torchmaster.common.network;

import com.godofthings.torchmaster.common.network.volume.VolumeDisplayMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessageHandler {
   private static final String PROTOCOL_VERSION = "1";
   public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
      ResourceLocation.tryBuild("godofthings", "torchmaster"), () -> "1", "1"::equals, "1"::equals
   );

   public static void initialize() {
      INSTANCE.registerMessage(1, VolumeDisplayMessage.class, VolumeDisplayMessage::encode, VolumeDisplayMessage::decode, VolumeDisplayMessage::dispatch);
   }
}
