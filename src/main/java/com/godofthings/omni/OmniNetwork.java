package com.godofthings.omni;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 全能升级的网络通道（原样移植自 Omni Upgrades 的 {@code OmniNetwork}）：
 * 玩家总开 / 关状态上行、个人黑名单上行、服务端全局黑名单下行。
 */
public final class OmniNetwork {

    private static final ConcurrentHashMap<UUID, Boolean> PLAYER_ENABLED = new ConcurrentHashMap<>();

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(OmniEnabledPayload.TYPE, OmniEnabledPayload.STREAM_CODEC, OmniNetwork::handleEnabledToggle);
        registrar.playToServer(OmniBlacklistPayload.TYPE, OmniBlacklistPayload.STREAM_CODEC, OmniNetwork::handlePlayerBlacklist);
        registrar.playToClient(ServerBlacklistPayload.TYPE, ServerBlacklistPayload.STREAM_CODEC, OmniNetwork::handleServerBlacklist);
    }

    private static void handleEnabledToggle(OmniEnabledPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                PLAYER_ENABLED.put(player.getUUID(), payload.enabled());
            }
        });
    }

    private static void handlePlayerBlacklist(OmniBlacklistPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                OmniBlacklist.overwritePlayer(player.getUUID(), payload.entries());
            }
        });
    }

    private static void handleServerBlacklist(ServerBlacklistPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> OmniBlacklist.setServerGlobalForClient(payload.entries()));
    }

    public static boolean isEnabled(Player player) {
        return PLAYER_ENABLED.getOrDefault(player.getUUID(), true);
    }

    public static void remove(UUID playerId) {
        PLAYER_ENABLED.remove(playerId);
        OmniBlacklist.removePlayer(playerId);
    }

    public static void sendToServer(boolean enabled) {
        PacketDistributor.sendToServer(new OmniEnabledPayload(enabled));
    }

    public static void sendBlacklistToServer(List<String> entries) {
        PacketDistributor.sendToServer(new OmniBlacklistPayload(entries));
    }

    public static void sendGlobalBlacklistToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ServerBlacklistPayload(OmniBlacklist.getGlobalRaw()));
    }

    private OmniNetwork() {
    }
}
