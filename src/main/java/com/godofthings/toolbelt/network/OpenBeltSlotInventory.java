package com.godofthings.toolbelt.network;

import com.godofthings.toolbelt.ToolBeltConfig;
import com.godofthings.toolbelt.common.Screens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求打开自定义皮带栏物品栏。
 */
public class OpenBeltSlotInventory
{
    public OpenBeltSlotInventory()
    {
    }

    public static void encode(OpenBeltSlotInventory msg, FriendlyByteBuf buf)
    {
    }

    public static OpenBeltSlotInventory decode(FriendlyByteBuf buf)
    {
        return new OpenBeltSlotInventory();
    }

    public static void handle(OpenBeltSlotInventory msg, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ToolBeltConfig.customBeltSlotEnabled)
            {
                ServerPlayer sender = ctx.getSender();
                if (sender != null)
                {
                    Screens.openSlotScreen(sender);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
