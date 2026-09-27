package com.godofthings.wand.event;

import com.godofthings.wand.UselessStretcherMod;
import com.godofthings.wand.content.acceleration.AccelerationExecutionBudget;
import com.godofthings.wand.content.range.RangeAccelerationSavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Drives all enabled world-owned ranges once per server tick. */
@EventBusSubscriber(modid = UselessStretcherMod.MODID)
public final class RangeAccelerationTicker {
    private RangeAccelerationTicker() {
    }

    @SubscribeEvent
    public static void onServerTickStart(ServerTickEvent.Pre event) {
        AccelerationExecutionBudget.beginTick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTickEnd(ServerTickEvent.Post event) {
        RangeAccelerationSavedData.get(event.getServer()).tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        AccelerationExecutionBudget.removeServer(event.getServer());
    }
}
