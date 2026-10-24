package com.godofthings.mept.network;

import com.godofthings.Godofthings;
import com.godofthings.mept.MeptRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端请求撤销（照抄上游 UndoPayload）。
 * 带玩家注视的方块坐标用于距离校验。
 */
public record UndoPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<UndoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_undo"));

    public static final StreamCodec<FriendlyByteBuf, UndoPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBlockPos(payload.pos),
            buf -> new UndoPayload(buf.readBlockPos())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UndoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // 绝不信任客户端坐标：必须离玩家足够近，
                // 否则「必须身处现场」的距离检查会被远程绕过。
                if (!player.blockPosition().closerThan(payload.pos, 10)) {
                    return;
                }
                // 带坐标执行撤销
                MeptRegistry.undoHistory().undo(player, player.level(), payload.pos);
            }
        });
    }
}
