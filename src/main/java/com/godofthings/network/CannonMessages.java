package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.item.GodCannonItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 神之炮网络通道：客户端「左键持续发射」请求 → 服务端执行贯穿光束。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CannonMessages
{
    @SubscribeEvent
    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CannonBeamPayload.TYPE, CannonBeamPayload.STREAM_CODEC, CannonBeamPayload::handle);
    }

    public static void sendBeam()
    {
        PacketDistributor.sendToServer(new CannonBeamPayload());
    }

    public record CannonBeamPayload() implements CustomPacketPayload
    {
        public static final Type<CannonBeamPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "cannon_beam"));
        public static final StreamCodec<ByteBuf, CannonBeamPayload> STREAM_CODEC =
                StreamCodec.<ByteBuf, CannonBeamPayload>unit(new CannonBeamPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(CannonBeamPayload msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer player)
                {
                    GodCannonItem.fireBeam(player);
                }
            });
        }
    }
}
