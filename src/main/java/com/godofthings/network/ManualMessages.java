package com.godofthings.network;

import com.godofthings.Godofthings;
import io.netty.buffer.ByteBuf;
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
 * 神之手册的网络包：只有一条 {@code manual_open}（服务端 → 客户端）。
 *
 * <p>手册内容全在客户端，所以只需要「让客户端开界面」这一条指令 —— 给
 * {@code /godofthings manual} 用（快捷键与物品是客户端直接开的，不走网络）。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ManualMessages
{
    @SubscribeEvent
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ManualOpenPayload.TYPE, ManualOpenPayload.STREAM_CODEC, ManualOpenPayload::handle);
    }

    /** 服务端 → 客户端：让这个客户端打开神之手册 */
    public static void sendOpen(ServerPlayer player)
    {
        PacketDistributor.sendToPlayer(player, new ManualOpenPayload());
    }

    public record ManualOpenPayload() implements CustomPacketPayload
    {
        public static final Type<ManualOpenPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "manual_open"));
        public static final StreamCodec<ByteBuf, ManualOpenPayload> STREAM_CODEC =
                StreamCodec.<ByteBuf, ManualOpenPayload>unit(new ManualOpenPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(ManualOpenPayload msg, IPayloadContext ctx)
        {
            // 客户端类引用关在 lambda 体内：服务端不会执行到这里，也就不会加载它
            ctx.enqueueWork(() -> com.godofthings.client.ManualOpener.open());
        }
    }
}
