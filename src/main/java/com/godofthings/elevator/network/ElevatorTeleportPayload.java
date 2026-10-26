package com.godofthings.elevator.network;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端→服务端：请求从 {@code from} 传送到 {@code to}。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.network.TeleportPacket}（MIT），逐行照抄。</p>
 */
public record ElevatorTeleportPayload(BlockPos from, BlockPos to) implements CustomPacketPayload
{
    public static final Type<ElevatorTeleportPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "elevator_teleport"));

    public static final StreamCodec<FriendlyByteBuf, ElevatorTeleportPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ElevatorTeleportPayload::from,
                    BlockPos.STREAM_CODEC, ElevatorTeleportPayload::to,
                    ElevatorTeleportPayload::new);

    @Override
    public Type<ElevatorTeleportPayload> type()
    {
        return TYPE;
    }
}
