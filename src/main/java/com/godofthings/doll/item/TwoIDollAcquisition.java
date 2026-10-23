package com.godofthings.doll.item;

import com.mojang.authlib.GameProfile;
import com.godofthings.doll.entity.TwoIDollAcquisitionData;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.init.DollComponents;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TwoIDollAcquisition {
   private TwoIDollAcquisition() {
   }

   public static void notifyAcquired(ServerPlayer player, ItemStack stack) {
      if (player != null && stack != null && stack.is((Item)DollRegistry.TWO_I_DOLL_ITEM.get())) {
         UUID dollId = TwoIDollItem.ensureIdentity(stack);
         MinecraftServer server = player.getServer();
         if (server != null && TwoIDollAcquisitionData.get(server).markIfNew(dollId)) {
            UUID ownerId = (UUID)stack.get((DataComponentType)DollComponents.TWO_I_OWNER.get());
            Component owner = resolveOwner(player.getServer(), ownerId);
            Component message = Component.translatable(
               "msg.godofthings.two_i.acquired_broadcast",
               new Object[]{Component.literal(player.getGameProfile().getName()), Component.translatable("item.godofthings.two_i_doll"), owner}
            );
            server.getPlayerList().broadcastSystemMessage(message, false);
         }
      }
   }

   private static Component resolveOwner(MinecraftServer server, UUID ownerId) {
      if (ownerId == null) {
         return Component.translatable("msg.godofthings.two_i.owner_unbound");
      } else {
         if (server != null) {
            ServerPlayer online = server.getPlayerList().getPlayer(ownerId);
            if (online != null) {
               return Component.literal(online.getGameProfile().getName());
            }

            GameProfileCache cache = server.getProfileCache();
            if (cache != null) {
               Optional<GameProfile> profile = cache.get(ownerId);
               if (profile.isPresent()) {
                  return Component.literal(profile.get().getName());
               }
            }
         }

         return Component.literal(ownerId.toString());
      }
   }
}
