package com.godofthings.torcherino.network;

import com.godofthings.torcherino.block.entity.TorcherinoBlockEntity;
import com.godofthings.torcherino.client.screen.TorcherinoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：打开火把调节界面。
 */
public final class OpenScreenMessage
{
    private final BlockPos pos;
    private final Component title;
    private final int xRange, zRange, yRange, speed, redstoneMode;

    public OpenScreenMessage(BlockPos pos, Component title, int xRange, int zRange, int yRange, int speed, int redstoneMode)
    {
        this.pos = pos;
        this.title = title;
        this.xRange = xRange;
        this.zRange = zRange;
        this.yRange = yRange;
        this.speed = speed;
        this.redstoneMode = redstoneMode;
    }

    public static void encode(OpenScreenMessage message, FriendlyByteBuf buffer)
    {
        buffer.writeBlockPos(message.pos).writeComponent(message.title).writeInt(message.xRange)
                .writeInt(message.zRange).writeInt(message.yRange).writeInt(message.speed).writeInt(message.redstoneMode);
    }

    public static OpenScreenMessage decode(FriendlyByteBuf buffer)
    {
        return new OpenScreenMessage(buffer.readBlockPos(), buffer.readComponent(), buffer.readInt(), buffer.readInt(),
                buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    public static void handle(OpenScreenMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getOriginationSide() == LogicalSide.SERVER)
        {
            openTorcherinoScreen(message);
            context.setPacketHandled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void openTorcherinoScreen(OpenScreenMessage message)
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.submitAsync(() ->
        {
            if (minecraft.player != null
                    && minecraft.player.level().getBlockEntity(message.pos) instanceof TorcherinoBlockEntity blockEntity)
            {
                TorcherinoScreen screen = new TorcherinoScreen(message.title, message.xRange, message.zRange, message.yRange,
                        message.speed, message.redstoneMode, blockEntity.getBlockPos(), blockEntity.getTier());
                minecraft.setScreen(screen);
            }
        });
    }
}
