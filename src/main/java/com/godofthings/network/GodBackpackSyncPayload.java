package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.backpack.GodBackpackMenu;
import com.godofthings.backpack.GodBackpackSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 神之背包：服务端 → 客户端的设置同步包（全量推一份设置 + 滚动行数）。
 *
 * <p>服务端每次改动设置（排序方式 / 两个忽略开关 / 保留搜索词 / 记忆格 / 忽略整理格 / 搜索词）
 * 或滚动，都用它推给客户端；客户端菜单存成副本，界面的按钮状态、记忆格影子与角标、
 * 不匹配格子的判定都读这份副本（{@link GodBackpackMenu#settings()}）。</p>
 *
 * <p>槽位内容本身不走这个包（那是原版容器同步的事），这里只管「设置」这类不在槽位里的状态。</p>
 */
public record GodBackpackSyncPayload(int containerId, GodBackpackSettings settings, int scroll)
        implements CustomPacketPayload
{
    public static final Type<GodBackpackSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "god_backpack_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GodBackpackSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) ->
            {
                buf.writeVarInt(payload.containerId());
                GodBackpackSettings.STREAM_CODEC.encode(buf, payload.settings());
                buf.writeVarInt(payload.scroll());
            },
            buf -> new GodBackpackSyncPayload(buf.readVarInt(),
                    GodBackpackSettings.STREAM_CODEC.decode(buf), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(GodBackpackSyncPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            // 只认当前开着的那个背包界面（containerId 对不上 = 迟到的包，丢掉）
            if (context.player().containerMenu instanceof GodBackpackMenu menu
                    && menu.containerId == payload.containerId())
            {
                menu.applySync(payload.settings(), payload.scroll());
            }
        });
    }
}
