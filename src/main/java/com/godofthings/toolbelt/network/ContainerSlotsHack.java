package com.godofthings.toolbelt.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求把皮带栏容器完整数据推给客户端（打开自定义物品栏后同步用）。
 */
public class ContainerSlotsHack
{
    public ContainerSlotsHack()
    {
    }

    public static void encode(ContainerSlotsHack msg, FriendlyByteBuf buf)
    {
    }

    public static ContainerSlotsHack decode(FriendlyByteBuf buf)
    {
        return new ContainerSlotsHack();
    }

    public static void handle(ContainerSlotsHack msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null)
            {
                ctx.getSender().containerMenu.sendAllDataToRemote();
            }
        });
        ctx.setPacketHandled(true);
    }
}
