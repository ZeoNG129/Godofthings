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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 神之传输网络通道：
 * <ul>
 *   <li>C2S 速率调节（mode 0=机器，1=玩家）。</li>
 *   <li>C2S 绑定/解绑玩家（按名字）。</li>
 *   <li>S2C 列表信息（在线玩家 + 已绑定玩家 + 已绑定机器坐标）。</li>
 * </ul>
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
        registrar.playToServer(TransmitterBindPlayerPayload.TYPE, TransmitterBindPlayerPayload.STREAM_CODEC,
                TransmitterBindPlayerPayload::handle);
        registrar.playToServer(TransmitterRequestListPayload.TYPE, TransmitterRequestListPayload.STREAM_CODEC,
                TransmitterRequestListPayload::handle);
        // TransmitterListPayload（S2C）在客户端侧 ClientTransmitterHandler 注册 handler
    }

    public static void sendRate(int rate, int mode)
    {
        PacketDistributor.sendToServer(new TransmitterRatePayload(rate, mode));
    }

    /** 客户端屏幕打开后主动请求在线玩家 / 绑定列表。 */
    public static void sendRequestList()
    {
        PacketDistributor.sendToServer(new TransmitterRequestListPayload());
    }

    public static void sendBindPlayer(String name, boolean bind)
    {
        PacketDistributor.sendToServer(new TransmitterBindPlayerPayload(name, bind));
    }

    /** 服务端把在线玩家 + 已绑定玩家 + 已绑定机器列表发给打开菜单的玩家。 */
    public static void sendList(ServerPlayer player, GodTransmitterBlockEntity be)
    {
        List<String> online = new ArrayList<>();
        for (ServerPlayer p : player.getServer().getPlayerList().getPlayers())
        {
            online.add(p.getGameProfile().getName());
        }
        List<String> bound = new ArrayList<>();
        for (UUID uuid : be.getBoundPlayers())
        {
            ServerPlayer p = player.getServer().getPlayerList().getPlayer(uuid);
            bound.add(p != null ? p.getGameProfile().getName() : uuid.toString());
        }
        List<String> machines = be.getBoundMachineTexts();
        PacketDistributor.sendToPlayer(player, new TransmitterListPayload(online, bound, machines));
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

    public record TransmitterBindPlayerPayload(String playerName, boolean bind) implements CustomPacketPayload
    {
        public static final Type<TransmitterBindPlayerPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "transmitter_bind_player"));
        public static final StreamCodec<ByteBuf, TransmitterBindPlayerPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TransmitterBindPlayerPayload::playerName,
                ByteBufCodecs.BOOL, TransmitterBindPlayerPayload::bind,
                TransmitterBindPlayerPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(TransmitterBindPlayerPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer sender)
                        || !(sender.containerMenu instanceof GodTransmitterMenu menu))
                {
                    return;
                }
                GodTransmitterBlockEntity be = menu.getBlockEntity();
                ServerPlayer target = null;
                for (ServerPlayer p : sender.getServer().getPlayerList().getPlayers())
                {
                    if (p.getGameProfile().getName().equals(msg.playerName()))
                    {
                        target = p;
                        break;
                    }
                }
                if (target == null)
                {
                    return;
                }
                if (msg.bind())
                {
                    be.bindPlayer(target.getUUID());
                }
                else
                {
                    be.unbindPlayer(target.getUUID());
                }
                sendList(sender, be);
            });
        }
    }

    public record TransmitterRequestListPayload() implements CustomPacketPayload
    {
        public static final Type<TransmitterRequestListPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "transmitter_request_list"));
        public static final StreamCodec<ByteBuf, TransmitterRequestListPayload> STREAM_CODEC =
                StreamCodec.unit(new TransmitterRequestListPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(TransmitterRequestListPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer sender)
                        || !(sender.containerMenu instanceof GodTransmitterMenu menu))
                {
                    return;
                }
                sendList(sender, menu.getBlockEntity());
            });
        }
    }

    public record TransmitterListPayload(List<String> online, List<String> boundPlayers,
                                         List<String> boundMachines) implements CustomPacketPayload
    {
        public static final Type<TransmitterListPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "transmitter_list"));
        public static final StreamCodec<ByteBuf, TransmitterListPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TransmitterListPayload::online,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TransmitterListPayload::boundPlayers,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TransmitterListPayload::boundMachines,
                TransmitterListPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }
    }
}
