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
        registrar.playToServer(SwordRangePayload.TYPE, SwordRangePayload.STREAM_CODEC, SwordRangePayload::handle);
    }

    public enum SwordMode
    {
        BEHEAD,
        CAPTURE,
        LOOTING,
        STAR_ABSORB,
        SOUL_ABSORB
    }

    public static void send(SwordMode mode)
    {
        PacketDistributor.sendToServer(new SwordModePayload(mode));
    }

    public static void sendRange(SwordMode mode, int delta)
    {
        PacketDistributor.sendToServer(new SwordRangePayload(mode, delta));
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
                    case STAR_ABSORB -> SwordModes.setStarAbsorbEnabled(sword, !SwordModes.isStarAbsorbEnabled(sword));
                    case SOUL_ABSORB -> SwordModes.setSoulAbsorbEnabled(sword, !SwordModes.isSoulAbsorbEnabled(sword));
                }
            });
        }
    }

    /** 吸星/吸魂半径调整（C2S）：mode 仅用 STAR_ABSORB / SOUL_ABSORB，delta 为 ±1。 */
    public record SwordRangePayload(SwordMode mode, int delta) implements CustomPacketPayload
    {
        public static final Type<SwordRangePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "sword_range"));
        public static final StreamCodec<ByteBuf, SwordRangePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT.map(i -> SwordMode.values()[i], SwordMode::ordinal), SwordRangePayload::mode,
                ByteBufCodecs.INT, SwordRangePayload::delta,
                SwordRangePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(SwordRangePayload msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx)
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
                    case STAR_ABSORB -> SwordModes.setStarRange(sword, SwordModes.getStarRange(sword) + msg.delta());
                    case SOUL_ABSORB -> SwordModes.setSoulRange(sword, SwordModes.getSoulRange(sword) + msg.delta());
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
