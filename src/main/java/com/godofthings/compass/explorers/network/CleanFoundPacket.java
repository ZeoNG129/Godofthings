package com.godofthings.compass.explorers.network;

import java.util.Objects;

import com.godofthings.Godofthings;
import com.godofthings.compass.explorers.worker.SearchWorkerManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：清掉该玩家「已搜到区块」的缓存（界面上的「清除地图」按钮）。
 *
 * <p>移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0。
 * 上游 {@code CleanFoundPacket} 的 {@code toBytes} 是空的；这里用 {@link StreamCodec#unit}。</p>
 */
public record CleanFoundPacket() implements CustomPacketPayload
{
    public static final Type<CleanFoundPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "explorers_compass_clean_found"));

    public static final StreamCodec<FriendlyByteBuf, CleanFoundPacket> CODEC =
            StreamCodec.unit(new CleanFoundPacket());

    public static void handle(CleanFoundPacket packet, IPayloadContext context)
    {
        if (context.flow().isServerbound())
        {
            context.enqueueWork(() ->
            {
                if (context.player() == null)
                {
                    return;
                }
                var uuid = Objects.requireNonNull(context.player()).getUUID();
                SearchWorkerManager.foundChunks.removeIf(pair -> pair.getFirst().equals(uuid));
            });
        }
    }

    @Override
    public Type<CleanFoundPacket> type()
    {
        return TYPE;
    }
}
