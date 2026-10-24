package com.godofthings.infinityprovider;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.godofthings.Godofthings;
import com.godofthings.item.GodBinderItem;

/**
 * 同心契的使用包（移植上游 WirelessConnectorUsePacket 的桥接思路）：
 * 客户端在 onItemUseFirst 里把点击目标与 Ctrl 批量状态发给服务端处理——
 * 服务端拿不到按键状态，必须走网络包（上游同理）。
 */
public record BinderUsePayload(
        net.minecraft.world.InteractionHand hand,
        net.minecraft.core.BlockPos pos,
        net.minecraft.core.Direction face,
        boolean contiguous) implements CustomPacketPayload {

    public static final Type<BinderUsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "binder_use"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BinderUsePayload> STREAM_CODEC =
            StreamCodec.ofMember(BinderUsePayload::write, BinderUsePayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static BinderUsePayload decode(RegistryFriendlyByteBuf buf) {
        return new BinderUsePayload(
                buf.readEnum(net.minecraft.world.InteractionHand.class),
                buf.readBlockPos(),
                buf.readEnum(net.minecraft.core.Direction.class),
                buf.readBoolean());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
        buf.writeBlockPos(this.pos);
        buf.writeEnum(this.face);
        buf.writeBoolean(this.contiguous);
    }

    public static void handle(BinderUsePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                GodBinderItem.handleBinderUseOnServer(player, payload.hand(), payload.pos(),
                        payload.face(), payload.contiguous());
            }
        });
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(TYPE, STREAM_CODEC, BinderUsePayload::handle);
    }
}
