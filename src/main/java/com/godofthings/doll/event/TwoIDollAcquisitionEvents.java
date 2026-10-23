package com.godofthings.doll.event;

import com.godofthings.Godofthings;


import com.godofthings.doll.item.TwoIDollAcquisition;
import com.godofthings.doll.init.DollRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/** Announces completed item pickups after the stack entered the player inventory. */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class TwoIDollAcquisitionEvents {
    private TwoIDollAcquisitionEvents() { }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void beforeItemPickup(ItemEntityPickupEvent.Pre event) {
        var stack = event.getItemEntity().getItem();
        if (stack.is(DollRegistry.TWO_I_DOLL.get()))
            com.godofthings.doll.item.TwoIDollItem.ensureIdentity(stack);
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!event.getOriginalStack().is(DollRegistry.TWO_I_DOLL.get())) return;
        var original = event.getOriginalStack();
        var id = original.get(com.godofthings.doll.init.DollComponents.TWO_I_DOLL_ID.get());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var received = player.getInventory().getItem(i);
            if (!received.is(DollRegistry.TWO_I_DOLL.get())) continue;
            if (id == null ? !net.minecraft.world.item.ItemStack.isSameItemSameComponents(original, received)
                    : !id.equals(received.get(com.godofthings.doll.init.DollComponents.TWO_I_DOLL_ID.get()))) continue;
            com.godofthings.doll.item.TwoIDollItem.claimOrCheckOwner(received, player);
            TwoIDollAcquisition.notifyAcquired(player, received);
            break;
        }
    }
}
