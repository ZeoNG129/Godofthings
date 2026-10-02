package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.note.GodNoteData;
import com.godofthings.note.NoteAdvancements;
import com.godofthings.note.NoteBook;
import com.godofthings.note.NoteShelf;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
 * 神之便签网络通道（4 条包）：
 * <ul>
 *   <li>{@code note_sync}（服务端→客户端）：下发整册便签（多本 + 悬浮窗设置），客户端镜像到本地供界面 / 悬浮窗使用</li>
 *   <li>{@code note_update}（客户端→服务端）：整册替换（服务端兜底后落库，再回发一份 sync）</li>
 *   <li>{@code note_request}（客户端→服务端）：进世界后拉一次数据（悬浮窗要显示）</li>
 *   <li>{@code note_open}（客户端→服务端）/ {@code note_open_screen}（服务端→客户端）：
 *       快捷键请求开界面；服务端回一条「开界面」指令，物品右键与指令走的是同一条下发路径</li>
 * </ul>
 *
 * <p>同步策略是「整册替换」而不是增量：数据量很小（上限 8 本 × 64 条 × 64 字），换来的是
 * 客户端怎么改、服务端就怎么存这种最不容易出错的形态；服务端始终先 {@code clamp()} 再落库，
 * 客户端塞垃圾数据也写不坏存档。</p>
 *
 * <p><b>协议版本</b>：v5.4.0 起载荷从「一本」变成「一册」（版本 2）；v5.6.0 起任务支持子任务、
 * 变成递归结构（版本 {@code 3}）—— 新旧两端不会静默错位（NeoForge 握手阶段就会拒掉不匹配的一侧）。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class GodNoteMessages
{
    @SubscribeEvent
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("3");
        registrar.playToClient(NoteSyncPayload.TYPE, NoteSyncPayload.STREAM_CODEC, NoteSyncPayload::handle);
        registrar.playToClient(NoteOpenScreenPayload.TYPE, NoteOpenScreenPayload.STREAM_CODEC, NoteOpenScreenPayload::handle);
        registrar.playToServer(NoteUpdatePayload.TYPE, NoteUpdatePayload.STREAM_CODEC, NoteUpdatePayload::handle);
        registrar.playToServer(NoteRequestPayload.TYPE, NoteRequestPayload.STREAM_CODEC, NoteRequestPayload::handle);
        registrar.playToServer(NoteOpenPayload.TYPE, NoteOpenPayload.STREAM_CODEC, NoteOpenPayload::handle);
    }

    /** 服务端 → 客户端：把该玩家的整册便签发下去（发副本，避免网络线程编码时主线程还在改同一份） */
    public static void sendSync(ServerPlayer player)
    {
        NoteShelf shelf = GodNoteData.get(player.server).shelf(player.getUUID()).copy();
        PacketDistributor.sendToPlayer(player, new NoteSyncPayload(shelf));
    }

    /** 服务端 → 客户端：让这个客户端打开便签界面（物品右键 / 指令 / 快捷键回执都走这里） */
    public static void sendOpenScreen(ServerPlayer player)
    {
        NoteAdvancements.award(player, NoteAdvancements.OPEN); // 「打开记事本」成就
        PacketDistributor.sendToPlayer(player, new NoteOpenScreenPayload());
    }

    /** 客户端 → 服务端：整册替换（发副本，理由同上） */
    public static void sendUpdate(NoteShelf shelf)
    {
        PacketDistributor.sendToServer(new NoteUpdatePayload(shelf.copy()));
    }

    /** 客户端 → 服务端：请求下发一次数据 */
    public static void requestSync()
    {
        PacketDistributor.sendToServer(new NoteRequestPayload());
    }

    /** 客户端 → 服务端：请求打开便签界面（快捷键） */
    public static void sendOpen()
    {
        PacketDistributor.sendToServer(new NoteOpenPayload());
    }

    public record NoteSyncPayload(NoteShelf shelf) implements CustomPacketPayload
    {
        public static final Type<NoteSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "note_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NoteSyncPayload> STREAM_CODEC =
                StreamCodec.of(NoteSyncPayload::write, NoteSyncPayload::read);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        private static void write(RegistryFriendlyByteBuf buf, NoteSyncPayload msg)
        {
            msg.shelf().write(buf);
        }

        private static NoteSyncPayload read(RegistryFriendlyByteBuf buf)
        {
            return new NoteSyncPayload(NoteShelf.read(buf));
        }

        public static void handle(NoteSyncPayload msg, IPayloadContext ctx)
        {
            // 全限定名写在 lambda 体内：服务端永远不会执行到这里，也就不会去解析客户端类
            ctx.enqueueWork(() -> com.godofthings.client.ClientNoteCache.apply(msg.shelf()));
        }
    }

    public record NoteUpdatePayload(NoteShelf shelf) implements CustomPacketPayload
    {
        public static final Type<NoteUpdatePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "note_update"));
        public static final StreamCodec<RegistryFriendlyByteBuf, NoteUpdatePayload> STREAM_CODEC =
                StreamCodec.of(NoteUpdatePayload::write, NoteUpdatePayload::read);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        private static void write(RegistryFriendlyByteBuf buf, NoteUpdatePayload msg)
        {
            msg.shelf().write(buf);
        }

        private static NoteUpdatePayload read(RegistryFriendlyByteBuf buf)
        {
            return new NoteUpdatePayload(NoteShelf.read(buf));
        }

        public static void handle(NoteUpdatePayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer serverPlayer)
                {
                    GodNoteData data = GodNoteData.get(serverPlayer.server);
                    // 先看旧状态：用来判「第一次打开悬浮窗 / 第一次开自动更名」两条行为成就
                    NoteShelf previous = data.shelf(serverPlayer.getUUID());
                    boolean hudWasOn = previous.hud().enabled;
                    boolean autoWasOn = anyAutoName(previous);

                    data.put(serverPlayer.getUUID(), msg.shelf());

                    if (!hudWasOn && msg.shelf().hud().enabled)
                    {
                        NoteAdvancements.award(serverPlayer, NoteAdvancements.HUD);
                    }
                    if (!autoWasOn && anyAutoName(msg.shelf()))
                    {
                        NoteAdvancements.award(serverPlayer, NoteAdvancements.AUTO);
                    }
                    // 回发一份规范化后的数据：客户端界面与悬浮窗都以这份为准
                    sendSync(serverPlayer);
                }
            });
        }

        private static boolean anyAutoName(NoteShelf shelf)
        {
            for (NoteBook book : shelf.books())
            {
                if (book.autoName())
                {
                    return true;
                }
            }
            return false;
        }
    }

    public record NoteRequestPayload() implements CustomPacketPayload
    {
        public static final Type<NoteRequestPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "note_request"));
        public static final StreamCodec<ByteBuf, NoteRequestPayload> STREAM_CODEC =
                StreamCodec.<ByteBuf, NoteRequestPayload>unit(new NoteRequestPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(NoteRequestPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer serverPlayer)
                {
                    sendSync(serverPlayer);
                }
            });
        }
    }

    public record NoteOpenPayload() implements CustomPacketPayload
    {
        public static final Type<NoteOpenPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "note_open"));
        public static final StreamCodec<ByteBuf, NoteOpenPayload> STREAM_CODEC =
                StreamCodec.<ByteBuf, NoteOpenPayload>unit(new NoteOpenPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(NoteOpenPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer serverPlayer)
                {
                    sendSync(serverPlayer);
                    sendOpenScreen(serverPlayer);
                }
            });
        }
    }

    public record NoteOpenScreenPayload() implements CustomPacketPayload
    {
        public static final Type<NoteOpenScreenPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "note_open_screen"));
        public static final StreamCodec<ByteBuf, NoteOpenScreenPayload> STREAM_CODEC =
                StreamCodec.<ByteBuf, NoteOpenScreenPayload>unit(new NoteOpenScreenPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(NoteOpenScreenPayload msg, IPayloadContext ctx)
        {
            // 同样把客户端类引用关在 lambda 体内，服务端不加载它
            ctx.enqueueWork(() -> com.godofthings.client.ClientNoteCache.openScreen());
        }
    }
}
