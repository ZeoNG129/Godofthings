package com.godofthings.omni;

import io.netty.buffer.ByteBuf;

import java.util.List;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端 → 客户端：下发服务端全局黑名单（原样移植自 {@code ServerBlacklistPayload}）。 */
public record ServerBlacklistPayload(List<String> entries) implements CustomPacketPayload {

    public static final Type<ServerBlacklistPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GodofthingsOmni.MODID, "omni_server_blacklist"));
    public static final StreamCodec<ByteBuf, ServerBlacklistPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ServerBlacklistPayload::entries,
            ServerBlacklistPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
