package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.armor.GodArmorFeatures;
import com.godofthings.armor.GodArmorState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 神之套装功能开关的网络通道（两端都要注册）。
 * <ul>
 *   <li>C2S {@code ArmorMaskPayload}：客户端把界面上算好的新位图交给服务端（服务端只保留已知 bit）。</li>
 *   <li>C2S {@code ArmorRequestPayload}：打开界面时主动拉一次权威值。</li>
 *   <li>S2C {@code ArmorSyncPayload}：服务端在登录 / 每次变更 / 收到请求时推送权威值。</li>
 * </ul>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ArmorMessages
{
    private ArmorMessages() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ArmorMaskPayload.TYPE, ArmorMaskPayload.STREAM_CODEC, ArmorMaskPayload::handle);
        registrar.playToServer(ArmorRequestPayload.TYPE, ArmorRequestPayload.STREAM_CODEC, ArmorRequestPayload::handle);
        registrar.playToClient(ArmorSyncPayload.TYPE, ArmorSyncPayload.STREAM_CODEC, ArmorSyncPayload::handle);
    }

    // ---- 客户端发送入口 ----

    /** 提交新的开关位图（界面点击后调用）。 */
    public static void sendMask(int mask)
    {
        PacketDistributor.sendToServer(new ArmorMaskPayload(GodArmorFeatures.sanitize(mask)));
    }

    /** 请求服务端推一次权威值（打开界面时调用）。 */
    public static void requestSync()
    {
        PacketDistributor.sendToServer(new ArmorRequestPayload());
    }

    /** 服务端把权威值推给某个玩家。 */
    public static void sendSync(ServerPlayer player)
    {
        PacketDistributor.sendToPlayer(player, new ArmorSyncPayload(GodArmorState.get(player)));
    }

    // ---- 包定义 ----

    public record ArmorMaskPayload(int mask) implements CustomPacketPayload
    {
        public static final Type<ArmorMaskPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "armor_mask"));

        public static final StreamCodec<ByteBuf, ArmorMaskPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ArmorMaskPayload::mask,
                ArmorMaskPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(ArmorMaskPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer player)
                {
                    GodArmorState.set(player, msg.mask());
                    sendSync(player); // 回显权威值，客户端镜像与界面立即收敛
                }
            });
        }
    }

    public record ArmorRequestPayload() implements CustomPacketPayload
    {
        public static final Type<ArmorRequestPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "armor_request"));

        public static final StreamCodec<ByteBuf, ArmorRequestPayload> STREAM_CODEC =
                StreamCodec.unit(new ArmorRequestPayload());

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(ArmorRequestPayload msg, IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (ctx.player() instanceof ServerPlayer player)
                {
                    sendSync(player);
                }
            });
        }
    }

    public record ArmorSyncPayload(int mask) implements CustomPacketPayload
    {
        public static final Type<ArmorSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "armor_sync"));

        public static final StreamCodec<ByteBuf, ArmorSyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ArmorSyncPayload::mask,
                ArmorSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(ArmorSyncPayload msg, IPayloadContext ctx)
        {
            // 只写客户端镜像，不碰玩家附件（附件是服务端权威数据）
            ctx.enqueueWork(() -> GodArmorState.setClientMask(msg.mask()));
        }
    }
}
