package com.godofthings.doll.event;

import com.godofthings.doll.item.TwoIDollAcquisition;
import com.godofthings.doll.item.TwoIDollItem;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.init.DollComponents;
import java.util.UUID;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre;

@EventBusSubscriber(
   modid = "godofthings"
)
public final class TwoIDollAcquisitionEvents {
   private TwoIDollAcquisitionEvents() {
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void beforeItemPickup(Pre event) {
      ItemStack stack = event.getItemEntity().getItem();
      if (stack.is((Item)DollRegistry.TWO_I_DOLL_ITEM.get())) {
         TwoIDollItem.ensureIdentity(stack);
      }
   }

   @SubscribeEvent
   public static void onItemPickup(Post event) {
      if (event.getPlayer() instanceof ServerPlayer player) {
         if (event.getOriginalStack().is((Item)DollRegistry.TWO_I_DOLL_ITEM.get())) {
            ItemStack original = event.getOriginalStack();
            UUID id = (UUID)original.get((DataComponentType)DollComponents.TWO_I_DOLL_ID.get());

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
               ItemStack received = player.getInventory().getItem(i);
               if (received.is((Item)DollRegistry.TWO_I_DOLL_ITEM.get())
                  && (
                     id == null
                        ? ItemStack.isSameItemSameComponents(original, received)
                        : id.equals(received.get((DataComponentType)DollComponents.TWO_I_DOLL_ID.get()))
                  )) {
                  TwoIDollItem.claimOrCheckOwner(received, player);
                  TwoIDollAcquisition.notifyAcquired(player, received);
                  break;
               }
            }
         }
      }
   }
}
