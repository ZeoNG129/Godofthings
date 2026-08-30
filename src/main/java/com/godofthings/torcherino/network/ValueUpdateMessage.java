package com.godofthings.torcherino.network;

import com.godofthings.torcherino.Torcherino;
import com.godofthings.torcherino.block.entity.TorcherinoBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：更新火把的调节值。
 */
public final class ValueUpdateMessage
{
    private final BlockPos pos;
    private final int xRange, zRange, yRange, speed, redstoneMode;

    public ValueUpdateMessage(BlockPos pos, int xRange, int zRange, int yRange, int speed, int redstoneMode)
    {
        this.pos = pos;
        this.xRange = xRange;
        this.zRange = zRange;
        this.yRange = yRange;
        this.speed = speed;
        this.redstoneMode = redstoneMode;
    }

    public static void encode(ValueUpdateMessage message, FriendlyByteBuf buffer)
    {
        buffer.writeBlockPos(message.pos).writeInt(message.xRange).writeInt(message.zRange)
                .writeInt(message.yRange).writeInt(message.speed).writeInt(message.redstoneMode);
    }

    public static ValueUpdateMessage decode(FriendlyByteBuf buffer)
    {
        return new ValueUpdateMessage(buffer.readBlockPos(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                buffer.readInt(), buffer.readInt());
    }

    public static void handle(ValueUpdateMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getOriginationSide() == LogicalSide.CLIENT)
        {
            context.enqueueWork(() ->
            {
                if (context.getSender() != null
                        && context.getSender().level().getBlockEntity(message.pos) instanceof TorcherinoBlockEntity blockEntity)
                {
                    if (!blockEntity.readClientData(message.xRange, message.zRange, message.yRange, message.speed, message.redstoneMode))
                    {
                        Torcherino.LOGGER.error("Data received from {} ({}) is invalid.",
                                context.getSender().getName().getString(), context.getSender().getStringUUID());
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }
}
