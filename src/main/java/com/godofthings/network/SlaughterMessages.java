package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodSlaughterBlockEntity;
import com.godofthings.menu.GodSlaughterMenu;
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
 * 神之砍杀网络通道：C2S 范围 / 抢夺强度调节（delta 支持 Shift/Ctrl 步进手势）。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class SlaughterMessages
{
    @SubscribeEvent
    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SlaughterRangePayload.TYPE, SlaughterRangePayload.STREAM_CODEC,
                SlaughterRangePayload::handle);
        registrar.playToServer(SlaughterLootingPayload.TYPE, SlaughterLootingPayload.STREAM_CODEC,
                SlaughterLootingPayload::handle);
    }

    public static void sendRange(int delta)
    {
        PacketDistributor.sendToServer(new SlaughterRangePayload(delta));
    }

    public static void sendLooting(int delta)
    {
        PacketDistributor.sendToServer(new SlaughterLootingPayload(delta));
    }

    public record SlaughterRangePayload(int delta) implements CustomPacketPayload
    {
        public static final Type<SlaughterRangePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "slaughter_range"));
        public static final StreamCodec<ByteBuf, SlaughterRangePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, SlaughterRangePayload::delta,
                SlaughterRangePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(SlaughterRangePayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer sender)
                        || !(sender.containerMenu instanceof GodSlaughterMenu menu))
                {
                    return;
                }
                GodSlaughterBlockEntity be = menu.getBlockEntity();
                be.setRange(be.getRange() + msg.delta());
            });
        }
    }

    public record SlaughterLootingPayload(int delta) implements CustomPacketPayload
    {
        public static final Type<SlaughterLootingPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "slaughter_looting"));
        public static final StreamCodec<ByteBuf, SlaughterLootingPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, SlaughterLootingPayload::delta,
                SlaughterLootingPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(SlaughterLootingPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer sender)
                        || !(sender.containerMenu instanceof GodSlaughterMenu menu))
                {
                    return;
                }
                GodSlaughterBlockEntity be = menu.getBlockEntity();
                be.setLooting(be.getLooting() + msg.delta());
            });
        }
    }
}
