package com.godofthings.elevator.network;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端→服务端：取消电梯的伪装。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.network.client.RemoveCamoPacket}（MIT），逐行照抄。</p>
 */
public record RemoveCamoPayload(BlockPos pos) implements CustomPacketPayload
{
    public static final Type<RemoveCamoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "elevator_remove_camo"));

    public static final StreamCodec<FriendlyByteBuf, RemoveCamoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, RemoveCamoPayload::pos,
                    RemoveCamoPayload::new);

    @Override
    public Type<RemoveCamoPayload> type()
    {
        return TYPE;
    }

    public static void handle(RemoveCamoPayload msg, IPayloadContext ctx)
    {
        ctx.enqueueWork(() ->
        {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (ElevatorNetwork.isBadClientPacket(player, msg.pos))
            {
                return;
            }
            if (player.level().getBlockEntity(msg.pos) instanceof com.godofthings.elevator.block.ElevatorBlockEntity tile)
            {
                tile.setCamoAndUpdate(null);
            }
        });
    }
}
