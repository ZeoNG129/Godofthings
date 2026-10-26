package com.godofthings.elevator.network;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端→服务端：设置电梯是否「有方向」（显示朝向箭头）。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.network.client.SetDirectionalPacket}（MIT），逐行照抄。</p>
 */
public record SetDirectionalPayload(boolean value, BlockPos pos) implements CustomPacketPayload
{
    public static final Type<SetDirectionalPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "elevator_set_directional"));

    public static final StreamCodec<FriendlyByteBuf, SetDirectionalPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SetDirectionalPayload::value,
                    BlockPos.STREAM_CODEC, SetDirectionalPayload::pos,
                    SetDirectionalPayload::new);

    @Override
    public Type<SetDirectionalPayload> type()
    {
        return TYPE;
    }

    public static void handle(SetDirectionalPayload msg, IPayloadContext ctx)
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
                        state.setValue(com.godofthings.elevator.ElevatorBlockBase.DIRECTIONAL, msg.value));
            }
        });
    }
}
