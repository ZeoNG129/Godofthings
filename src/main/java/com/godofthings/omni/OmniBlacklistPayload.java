package com.godofthings.omni;

import io.netty.buffer.ByteBuf;

import java.util.List;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;

/** 客户端 → 服务端：同步玩家的个人黑名单（原样移植自 {@code OmniBlacklistPayload}，String 用 UTF 编码）。 */
public record OmniBlacklistPayload(List<String> entries) implements CustomPacketPayload {

    public static final Type<OmniBlacklistPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GodofthingsOmni.MODID, "omni_blacklist"));
    public static final StreamCodec<ByteBuf, OmniBlacklistPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OmniBlacklistPayload::entries,
            OmniBlacklistPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
