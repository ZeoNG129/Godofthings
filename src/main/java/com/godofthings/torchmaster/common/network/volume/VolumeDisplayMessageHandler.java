package com.godofthings.torchmaster.common.network.volume;

import com.godofthings.torchmaster.client.EntityBlockingVolumeRenderer;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent.Context;

public class VolumeDisplayMessageHandler {
   public static void handle(VolumeDisplayMessage msg, Supplier<Context> ctx) {
      if (msg.showVolume()) {
         EntityBlockingVolumeRenderer.showVolumeAt(msg.getPosition(), msg.getRange(), msg.getColor());
      } else {
         EntityBlockingVolumeRenderer.removeVolumeAt(msg.getPosition());
      }

      if (msg.showLocation()) {
         EntityBlockingVolumeRenderer.showLocationAt(msg.getPosition(), msg.getColor());
      } else {
         EntityBlockingVolumeRenderer.removeLocationAt(msg.getPosition());
      }
   }
}
