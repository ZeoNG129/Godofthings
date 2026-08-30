package com.godofthings.torchmaster.common.logic.entityblocking;

import com.godofthings.torchmaster.common.logic.entityblocking.dreadlamp.DreadLampSerializer;
import com.godofthings.torchmaster.common.logic.entityblocking.megatorch.MegatorchSerializer;
import java.util.HashMap;

public class LightSerializerRegistry {
   private static HashMap<String, ILightSerializer> registry = new HashMap<>();

   public static void registerLightSerializer(ILightSerializer serializer) {
      String lightSerializerKey = serializer.getSerializerKey();
      if (lightSerializerKey == null) {
         throw new RuntimeException("SerializerKey is null! " + serializer.getClass().getCanonicalName());
      } else if (registry.containsKey(lightSerializerKey)) {
         throw new RuntimeException("lightSerializer '" + lightSerializerKey + "' already exists");
      } else {
         registry.put(lightSerializerKey, serializer);
      }
   }

   public static ILightSerializer getLightSerializer(String lightSerializerKey) {
      return registry.get(lightSerializerKey);
   }

   static {
      registerLightSerializer(MegatorchSerializer.INSTANCE);
      registerLightSerializer(DreadLampSerializer.INSTANCE);
   }
}
