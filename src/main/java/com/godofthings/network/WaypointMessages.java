package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.menu.WaypointListMenu;
import com.godofthings.menu.WaypointMenu;
import com.godofthings.waypoint.Waypoint;
import com.godofthings.waypoint.WaypointData;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 神之记录网络通道：
 * - WaypointListPayload（服务端→客户端）：下发点位列表刷新 UI
 * - WaypointActionPayload（客户端→服务端）：请求传送/删除/置顶某点位
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class WaypointMessages
{
    public static final byte ACTION_TELEPORT = 0;
    public static final byte ACTION_DELETE = 1;
    public static final byte ACTION_PIN = 2;
    public static final byte ACTION_REQUEST = 3;

    @SubscribeEvent
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(WaypointListPayload.TYPE, WaypointListPayload.STREAM_CODEC, WaypointListPayload::handle);
        registrar.playToServer(WaypointActionPayload.TYPE, WaypointActionPayload.STREAM_CODEC, WaypointActionPayload::handle);
        registrar.playToServer(WaypointOpenPayload.TYPE, WaypointOpenPayload.STREAM_CODEC, WaypointOpenPayload::handle);
    }

    public static void sendListTo(ServerPlayer player, List<Waypoint> list)
    {
        PacketDistributor.sendToPlayer(player, new WaypointListPayload(list));
    }

    public static void sendAction(byte action, String name)
    {
        PacketDistributor.sendToServer(new WaypointActionPayload(action, name));
    }

    /** 客户端请求服务端下发当前点位列表 */
    public static void requestList()
    {
        sendAction(ACTION_REQUEST, "");
    }

    /** 客户端请求打开传送点界面（U 键） */
    public static void sendOpen()
    {
        PacketDistributor.sendToServer(new WaypointOpenPayload());
    }

    public record WaypointListPayload(List<Waypoint> waypoints) implements CustomPacketPayload
    {
        public static final Type<WaypointListPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "waypoint_list"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WaypointListPayload> STREAM_CODEC =
                StreamCodec.of(WaypointListPayload::write, WaypointListPayload::read);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        private static void write(RegistryFriendlyByteBuf buf, WaypointListPayload msg)
        {
            buf.writeVarInt(msg.waypoints.size());
            for (Waypoint wp : msg.waypoints)
            {
                Waypoint.write(buf, wp);
            }
        }

        private static WaypointListPayload read(RegistryFriendlyByteBuf buf)
        {
            int n = buf.readVarInt();
            List<Waypoint> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
            {
                list.add(Waypoint.read(buf));
            }
            return new WaypointListPayload(list);
        }

        public static void handle(WaypointListPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.containerMenu instanceof WaypointListMenu menu)
                {
                    menu.setWaypoints(msg.waypoints());
                }
            });
        }
    }

    public record WaypointActionPayload(byte action, String name) implements CustomPacketPayload
    {
        public static final Type<WaypointActionPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "waypoint_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, WaypointActionPayload> STREAM_CODEC =
                StreamCodec.of(WaypointActionPayload::write, WaypointActionPayload::read);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        private static void write(RegistryFriendlyByteBuf buf, WaypointActionPayload msg)
        {
            buf.writeByte(msg.action);
            buf.writeUtf(msg.name);
        }

        private static WaypointActionPayload read(RegistryFriendlyByteBuf buf)
        {
            return new WaypointActionPayload(buf.readByte(), buf.readUtf());
        }

        public static void handle(WaypointActionPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer serverPlayer)
                {
                    WaypointData data = WaypointData.get(serverPlayer.server);
                    switch (msg.action())
                    {
                        case ACTION_TELEPORT -> WaypointData.teleport(serverPlayer, data.get(msg.name()));
                        case ACTION_DELETE -> data.remove(msg.name());
                        case ACTION_PIN -> data.togglePin(msg.name());
                        case ACTION_REQUEST -> { }
                        default -> { return; }
                    }
                    // 操作后回发最新列表刷新 UI
                    sendListTo(serverPlayer, data.list());
                }
            });
        }
    }

    public record WaypointOpenPayload() implements CustomPacketPayload
    {
        public static final Type<WaypointOpenPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "waypoint_open"));
        public static final StreamCodec<ByteBuf, WaypointOpenPayload> STREAM_CODEC =
                StreamCodec.<ByteBuf, WaypointOpenPayload>unit(new WaypointOpenPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(WaypointOpenPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer serverPlayer)
                {
                    serverPlayer.openMenu(new SimpleMenuProvider(
                            (id, inv, player) -> new WaypointMenu(id, inv),
                            Component.translatable("gui.godofthings.waypoint.title")));
                }
            });
        }
    }
}
