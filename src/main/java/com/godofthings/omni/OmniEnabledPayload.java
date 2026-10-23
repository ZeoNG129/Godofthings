package com.godofthings.omni;

import io.netty.buffer.ByteBuf;

import java.util.List;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 → 服务端：玩家切换全能升级总开关（原样移植自 Omni Upgrades 的 {@code OmniEnabledPayload}）。 */
public record OmniEnabledPayload(boolean enabled) implements CustomPacketPayload {

    public static final Type<OmniEnabledPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GodofthingsOmni.MODID, "omni_enabled_toggle"));
    public static final StreamCodec<ByteBuf, OmniEnabledPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OmniEnabledPayload::enabled, OmniEnabledPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
