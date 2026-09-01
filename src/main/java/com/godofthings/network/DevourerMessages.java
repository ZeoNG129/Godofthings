package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodDevourerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 神之吞噬网络通道：背包按钮 → 服务端打开便携吞噬界面。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DevourerMessages
{
    @SubscribeEvent
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(DevourerOpenPayload.TYPE, DevourerOpenPayload.STREAM_CODEC, DevourerOpenPayload::handle);
    }

    /** 客户端请求打开便携吞噬界面（背包按钮） */
    public static void sendOpen()
    {
        PacketDistributor.sendToServer(new DevourerOpenPayload());
    }

    public record DevourerOpenPayload() implements CustomPacketPayload
    {
        public static final Type<DevourerOpenPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "devourer_open"));
        public static final StreamCodec<ByteBuf, DevourerOpenPayload> STREAM_CODEC =
                StreamCodec.<ByteBuf, DevourerOpenPayload>unit(new DevourerOpenPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(DevourerOpenPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer serverPlayer)
                {
                    serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inv, player) -> new GodDevourerMenu(id, inv),
                            Component.translatable("gui.godofthings.devourer.title")));
                }
            });
        }
    }
}
