package com.godofthings.wondrous.event;
import com.godofthings.Godofthings;

import com.godofthings.wondrous.UselessStretcherMod;
import com.godofthings.wondrous.content.entity.StaffPhaseManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** The second phase switch deliberately controls whether ordinary interaction is suppressed. */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class StaffPhaseInteractionEvents {
    private StaffPhaseInteractionEvents() {}

    @SubscribeEvent public static void rightBlock(PlayerInteractEvent.RightClickBlock event) {
        if (StaffPhaseManager.blocksInteraction(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent public static void rightItem(PlayerInteractEvent.RightClickItem event) {
        if (StaffPhaseManager.blocksInteraction(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent public static void entity(PlayerInteractEvent.EntityInteract event) {
        if (StaffPhaseManager.blocksInteraction(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent public static void entitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (StaffPhaseManager.blocksInteraction(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent public static void leftBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (StaffPhaseManager.blocksInteraction(event.getEntity())) event.setCanceled(true);
    }
}
