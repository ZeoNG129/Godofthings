package com.godofthings.handler;

import com.mojang.logging.LogUtils;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

public final class AdAstraCompat {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String AD_ASTRA_ID = "ad_astra";
   private static final String EVENTS_CLASS = "earth.terrarium.adastra.api.events.AdAstraEvents$OxygenTickEvent";
   private static final String ENTITY_OXYGEN_EVENTS_CLASS = "earth.terrarium.adastra.api.events.AdAstraEvents$EntityOxygenEvent";
   private static boolean enabled;

   private AdAstraCompat() {
   }

   public static void init() {
      if (ModList.get().isLoaded("ad_astra")) {
         try {
            registerOxygenTickListener();
            registerEntityOxygenListener();
            enabled = true;
            LOGGER.info("[Godofthings] Ad Astra 兼容已启用：穿戴全套神之护甲可获得无限氧气供应。");
         } catch (ReflectiveOperationException var1) {
            LOGGER.warn("[Godofthings] Ad Astra 氧气兼容注册失败（忽略）：{}", var1.getMessage());
         }
      }
   }

   private static void registerOxygenTickListener() throws ReflectiveOperationException {
      Class<?> eventClass = Class.forName("earth.terrarium.adastra.api.events.AdAstraEvents$OxygenTickEvent");
      InvocationHandler handler = (proxy, method, args) -> {
         String var3 = method.getName();
         switch (var3) {
            case "tick":
               if (args[1] instanceof Player player && GodArmorHandler.isFullSetWorn(player)) {
                  return false;
               }

               return true;
            case "toString":
               return "Godofthings-OxygenTickListener";
            case "hashCode":
               return System.identityHashCode(proxy);
            case "equals":
               return proxy == args[0];
            default:
               return false;
         }
      };
      Object listener = Proxy.newProxyInstance(eventClass.getClassLoader(), new Class[]{eventClass}, handler);
      eventClass.getMethod("register", eventClass).invoke(null, listener);
   }

   private static void registerEntityOxygenListener() throws ReflectiveOperationException {
      Class<?> eventClass = Class.forName("earth.terrarium.adastra.api.events.AdAstraEvents$EntityOxygenEvent");
      InvocationHandler handler = (proxy, method, args) -> {
         String var3 = method.getName();
         switch (var3) {
            case "hasOxygen":
               if (args[0] instanceof Player player && GodArmorHandler.isFullSetWorn(player)) {
                  return true;
               }

               return args[1];
            case "toString":
               return "Godofthings-EntityOxygenListener";
            case "hashCode":
               return System.identityHashCode(proxy);
            case "equals":
               return proxy == args[0];
            default:
               return false;
         }
      };
      Object listener = Proxy.newProxyInstance(eventClass.getClassLoader(), new Class[]{eventClass}, handler);
      eventClass.getMethod("register", eventClass).invoke(null, listener);
   }

   public static boolean isEnabled() {
      return enabled;
   }
}
