package com.godofthings.elevator.network;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端→服务端：设置电梯朝向。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.network.client.SetFacingPacket}（MIT），逐行照抄。</p>
 */
public record SetFacingPayload(Direction direction, BlockPos pos) implements CustomPacketPayload
{
    public static final Type<SetFacingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "elevator_set_facing"));

    public static final StreamCodec<FriendlyByteBuf, SetFacingPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Direction.STREAM_CODEC, SetFacingPayload::direction,
                    BlockPos.STREAM_CODEC, SetFacingPayload::pos,
                    SetFacingPayload::new);

    @Override
    public Type<SetFacingPayload> type()
    {
        return TYPE;
    }

    public static void handle(SetFacingPayload msg, IPayloadContext ctx)
    {
        ctx.enqueueWork(() ->
        {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (ElevatorNetwork.isBadClientPacket(player, msg.pos))
            {
                return;
            }
            Level world = player.level();
            BlockState state = world.getBlockState(msg.pos);
            if (state.getBlock() instanceof com.godofthings.elevator.ElevatorBlockBase)
            {
                world.setBlockAndUpdate(msg.pos,
                        state.setValue(com.godofthings.elevator.ElevatorBlockBase.FACING, msg.direction));
            }
        });
    }
}
