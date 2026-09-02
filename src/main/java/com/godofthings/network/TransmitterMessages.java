package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodTransmitterBlockEntity;
import com.godofthings.menu.GodTransmitterMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 神之传输速率调节 C2S 通道：mode 0=机器速率，1=玩家速率。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class TransmitterMessages
{
    @SubscribeEvent
    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TransmitterRatePayload.TYPE, TransmitterRatePayload.STREAM_CODEC,
                TransmitterRatePayload::handle);
    }

    public static void sendRate(int rate, int mode)
    {
        PacketDistributor.sendToServer(new TransmitterRatePayload(rate, mode));
    }

    public record TransmitterRatePayload(int rate, int mode) implements CustomPacketPayload
    {
        public static final Type<TransmitterRatePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "transmitter_rate"));
        public static final StreamCodec<ByteBuf, TransmitterRatePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, TransmitterRatePayload::rate,
                ByteBufCodecs.INT, TransmitterRatePayload::mode,
                TransmitterRatePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(TransmitterRatePayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer sender)
                        || !(sender.containerMenu instanceof GodTransmitterMenu menu))
                {
                    return;
                }
                GodTransmitterBlockEntity be = menu.getBlockEntity();
                if (msg.mode() == 0)
                {
                    be.setMachineRate(msg.rate());
                }
                else
                {
                    be.setPlayerRate(msg.rate());
                }
            });
        }
    }
}
