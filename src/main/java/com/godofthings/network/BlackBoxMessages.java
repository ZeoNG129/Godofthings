package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.item.BlackBoxData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 神之黑盒网络通道：蹲下滚轮 → 服务端翻转黑盒开关。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class BlackBoxMessages
{
    @SubscribeEvent
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(BlackBoxTogglePayload.TYPE, BlackBoxTogglePayload.STREAM_CODEC, BlackBoxTogglePayload::handle);
    }

    /** 客户端请求翻转黑盒开关。 */
    public static void sendToggle()
    {
        PacketDistributor.sendToServer(new BlackBoxTogglePayload());
    }

    public record BlackBoxTogglePayload() implements CustomPacketPayload
    {
        public static final Type<BlackBoxTogglePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "black_box_toggle"));
        public static final StreamCodec<ByteBuf, BlackBoxTogglePayload> STREAM_CODEC =
                StreamCodec.<ByteBuf, BlackBoxTogglePayload>unit(new BlackBoxTogglePayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(BlackBoxTogglePayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer serverPlayer)
                {
                    ItemStack box = BlackBoxData.findAnyBox(serverPlayer);
                    if (!box.isEmpty())
                    {
                        BlackBoxData.setEnabled(box, !BlackBoxData.isEnabled(box));
                    }
                }
            });
        }
    }
}
