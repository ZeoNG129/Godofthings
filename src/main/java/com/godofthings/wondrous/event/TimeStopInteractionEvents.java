package com.godofthings.wondrous.event;
import com.godofthings.Godofthings;

import com.godofthings.wondrous.UselessStretcherMod;
import com.godofthings.wondrous.content.entity.TimeStopManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Prevents frozen players from mutating blocks or entities during a time stop. */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class TimeStopInteractionEvents {
    private TimeStopInteractionEvents() { }

    @SubscribeEvent
    public static void rightBlock(PlayerInteractEvent.RightClickBlock event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void rightItem(PlayerInteractEvent.RightClickItem event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void entity(PlayerInteractEvent.EntityInteract event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void entitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void leftBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (blocked(event.getEntity())) event.setCanceled(true);
    }

    private static boolean blocked(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer serverPlayer
                && TimeStopManager.isPaused(serverPlayer.serverLevel())
                && !TimeStopManager.mayPlayerTickDuringPause(serverPlayer);
    }
}
