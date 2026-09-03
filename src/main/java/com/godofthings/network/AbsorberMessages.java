package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodAbsorberBlockEntity;
import com.godofthings.menu.GodAbsorberMenu;
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
 * 神之吸收网络通道：C2S 范围调节（delta 支持 Shift/Ctrl 步进手势）。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class AbsorberMessages
{
    @SubscribeEvent
    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event)
    {
        event.registrar("1").playToServer(AbsorberRangePayload.TYPE, AbsorberRangePayload.STREAM_CODEC,
                AbsorberRangePayload::handle);
    }

    public static void sendRange(int delta)
    {
        PacketDistributor.sendToServer(new AbsorberRangePayload(delta));
    }

    public record AbsorberRangePayload(int delta) implements CustomPacketPayload
    {
        public static final Type<AbsorberRangePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "absorber_range"));
        public static final StreamCodec<ByteBuf, AbsorberRangePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, AbsorberRangePayload::delta, AbsorberRangePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(AbsorberRangePayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer sender)
                        || !(sender.containerMenu instanceof GodAbsorberMenu menu))
                {
                    return;
                }
                GodAbsorberBlockEntity be = menu.getBlockEntity();
                be.setRange(be.getRange() + msg.delta());
            });
        }
    }
}
