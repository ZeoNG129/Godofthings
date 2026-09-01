package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.item.GodSwordItem;
import com.godofthings.item.SwordModes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 神之剑的功能开关网络通道（C2S）。切换面板点击开关时发送，服务端更新神之剑 CUSTOM_DATA 与抢夺附魔。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class SwordMessages
{
    @SubscribeEvent
    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SwordModePayload.TYPE, SwordModePayload.STREAM_CODEC, SwordModePayload::handle);
    }

    public enum SwordMode
    {
        BEHEAD,
        CAPTURE,
        LOOTING
    }

    public static void send(SwordMode mode)
    {
        PacketDistributor.sendToServer(new SwordModePayload(mode));
    }

    public record SwordModePayload(SwordMode mode) implements CustomPacketPayload
    {
        public static final Type<SwordModePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "sword_mode"));
        public static final StreamCodec<ByteBuf, SwordModePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT.map(i -> SwordMode.values()[i], SwordMode::ordinal), SwordModePayload::mode,
                SwordModePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(SwordModePayload msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer sender))
                {
                    return;
                }
                ItemStack sword = findSword(sender);
                if (sword == null)
                {
                    return;
                }
                switch (msg.mode())
                {
                    case BEHEAD -> SwordModes.setBeheadEnabled(sword, !SwordModes.isBeheadEnabled(sword));
                    case CAPTURE -> SwordModes.setCaptureEnabled(sword, !SwordModes.isCaptureEnabled(sword));
                    case LOOTING ->
                    {
                        boolean enabled = !SwordModes.isLootingEnabled(sword);
                        SwordModes.setLootingEnabled(sword, enabled);
                        GodSwordItem.applyLooting(sword, sender.serverLevel(), enabled);
                    }
                }
            });
        }
    }

    private static ItemStack findSword(ServerPlayer player)
    {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GodSwordItem)
        {
            return stack;
        }
        stack = player.getOffhandItem();
        if (stack.getItem() instanceof GodSwordItem)
        {
            return stack;
        }
        return null;
    }
}
