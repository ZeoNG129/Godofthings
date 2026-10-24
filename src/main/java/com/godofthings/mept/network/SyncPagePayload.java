package com.godofthings.mept.network;

import com.godofthings.Godofthings;
import com.godofthings.mept.WandMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 -> 服务端同步页码（照抄上游 SyncPagePayload）。
 */
public record SyncPagePayload(int page) implements CustomPacketPayload {
    public static final Type<SyncPagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_sync_page"));

    public static final StreamCodec<FriendlyByteBuf, SyncPagePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SyncPagePayload::page,
                    SyncPagePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // 把页码同步给服务端菜单
                if (player.containerMenu instanceof WandMenu menu) {
                    menu.setCurrentPage(payload.page);
                }
            }
        });
    }
}
