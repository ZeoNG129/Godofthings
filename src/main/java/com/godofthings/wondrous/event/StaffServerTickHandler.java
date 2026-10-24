package com.godofthings.wondrous.event;
import com.godofthings.Godofthings;

import com.godofthings.wondrous.content.entity.PlayerAccelerationManager;
import com.godofthings.wondrous.content.entity.StaffPhaseManager;
import com.godofthings.wondrous.content.entity.TimeStopManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Godofthings.MODID)
public final class StaffServerTickHandler {
    private StaffServerTickHandler() { }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        TimeStopManager.tick(event.getServer());
        PlayerAccelerationManager.tick(event.getServer(), TimeStopManager.remaining(event.getServer()) > 0);
        StaffPhaseManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        StaffPhaseManager.clear();
    }
}
