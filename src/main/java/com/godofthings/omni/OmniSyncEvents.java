package com.godofthings.omni;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;

/** 登录时下发服务端全局黑名单、登出时清缓存（原样移植自 {@code OmniSyncEvents}）。 */
@EventBusSubscriber(modid = GodofthingsOmni.MODID)
public class OmniSyncEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide
                && event.getEntity() instanceof ServerPlayer serverPlayer) {
            OmniNetwork.sendGlobalBlacklistToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        OmniNetwork.remove(event.getEntity().getUUID());
    }
}
