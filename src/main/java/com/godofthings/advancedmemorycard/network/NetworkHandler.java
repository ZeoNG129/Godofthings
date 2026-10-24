package com.godofthings.advancedmemorycard.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 高级内存卡的网络通道。
 *
 * <p>上游用 {@code @EventBusSubscriber}（游戏总线）监听 {@link RegisterPayloadHandlersEvent}
 * （mod 总线事件），总线不匹配根本收不到；并入本模组后改为普通类，
 * 由 {@code Godofthings} 构造器 {@code modEventBus.addListener(NetworkHandler::register)} 接线。</p>
 */
public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    private NetworkHandler() {
    }

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                ConfigModeSyncPacket.TYPE,
                ConfigModeSyncPacket.STREAM_CODEC,
                ConfigModeSyncPacket::handleClient
        );

        registrar.playToServer(
                OpenGuiPacket.TYPE,
                OpenGuiPacket.STREAM_CODEC,
                OpenGuiPacket::handleServer
        );

        registrar.playToServer(
                ModeSwitchPacket.TYPE,
                ModeSwitchPacket.STREAM_CODEC,
                ModeSwitchPacket::handleServer
        );
    }

    public static void sendToServer(CustomPacketPayload message) {
        PacketDistributor.sendToServer(message);
    }

    public static void sendToPlayer(CustomPacketPayload message, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, message);
        }
    }

    public static void sendToAllPlayers(CustomPacketPayload message) {
        PacketDistributor.sendToAllPlayers(message);
    }
}
