package com.godofthings.wondrous.network;

import com.godofthings.Godofthings;
import com.godofthings.wondrous.content.apotheosis.ApotheosisStaffSettings;
import com.godofthings.wondrous.content.apotheosis.ApotheosisTableData;
import com.godofthings.wondrous.content.item.WondrousStaffItem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * APOC 附魔台设置的小型应答通道（照抄上游 ApotheosisNetwork）。
 * 台位标记坐标只在请求或新绑定时下发。
 */
public final class ApotheosisNetwork
{
    public static final int MAX_MARKERS = 4096;

    private ApotheosisNetwork()
    {
    }

    private static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, path);
    }

    public record RequestPayload(boolean offhand) implements CustomPacketPayload
    {
        public static final Type<RequestPayload> TYPE = new Type<>(id("apotheosis_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestPayload> CODEC = StreamCodec.of(
                (buf, value) -> buf.writeBoolean(value.offhand()), buf -> new RequestPayload(buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public record SettingsPayload(boolean offhand, boolean selecting, String tier,
                                  ApotheosisTableData.Boost boost) implements CustomPacketPayload
    {
        public static final Type<SettingsPayload> TYPE = new Type<>(id("apotheosis_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeBoolean(value.offhand());
                    buf.writeBoolean(value.selecting());
                    buf.writeUtf(value.tier(), 32);
                    buf.writeVarInt(value.boost().eterna());
                    buf.writeVarInt(value.boost().quanta());
                    buf.writeVarInt(value.boost().arcana());
                    buf.writeVarInt(value.boost().clues());
                }, buf -> new SettingsPayload(buf.readBoolean(), buf.readBoolean(), buf.readUtf(32),
                        new ApotheosisTableData.Boost(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                                buf.readVarInt()).normalized()));

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public record MarkersPayload(ResourceLocation dimension, List<BlockPos> tables) implements CustomPacketPayload
    {
        public static final Type<MarkersPayload> TYPE = new Type<>(id("apotheosis_markers"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MarkersPayload> CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeResourceLocation(value.dimension());
                    int count = Math.min(MAX_MARKERS, value.tables().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) buf.writeBlockPos(value.tables().get(i));
                }, buf -> {
                    ResourceLocation dimension = buf.readResourceLocation();
                    int count = buf.readVarInt();
                    if (count < 0 || count > MAX_MARKERS) throw new IllegalArgumentException("Invalid table marker count");
                    List<BlockPos> tables = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) tables.add(buf.readBlockPos());
                    return new MarkersPayload(dimension, List.copyOf(tables));
                });

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }

    public static void register(PayloadRegistrar registrar)
    {
        registrar.playToServer(RequestPayload.TYPE, RequestPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (!(player.getItemInHand(hand).getItem() instanceof WondrousStaffItem)) return;
            sendSettings(player, hand);
            sendMarkers(player);
        }));
        registrar.playToClient(SettingsPayload.TYPE, SettingsPayload.CODEC, (payload, context) -> context.enqueueWork(() ->
                com.godofthings.wondrous.client.ApotheosisClientState.accept(payload)));
        registrar.playToClient(MarkersPayload.TYPE, MarkersPayload.CODEC, (payload, context) -> context.enqueueWork(() ->
                com.godofthings.wondrous.client.ApotheosisClientState.accept(payload)));
    }

    public static void requestState(InteractionHand hand)
    {
        PacketDistributor.sendToServer(new RequestPayload(hand == InteractionHand.OFF_HAND));
    }

    public static void setApotheosisTier(String tier)
    {
        WondrousNetwork.sendApotheosisTier(tier);
    }

    public static void sendSettings(ServerPlayer player, InteractionHand hand)
    {
        var staff = player.getItemInHand(hand);
        PacketDistributor.sendToPlayer(player, new SettingsPayload(hand == InteractionHand.OFF_HAND,
                ApotheosisStaffSettings.selectionMode(staff), ApotheosisStaffSettings.currentTier(player),
                ApotheosisStaffSettings.boost(staff)));
    }

    public static void sendMarkers(ServerPlayer player)
    {
        ResourceLocation dimension = player.level().dimension().location();
        var tables = ApotheosisTableData.get(player.getServer()).ownedTables(player.getUUID(), dimension, MAX_MARKERS);
        PacketDistributor.sendToPlayer(player, new MarkersPayload(dimension, tables));
    }
}
