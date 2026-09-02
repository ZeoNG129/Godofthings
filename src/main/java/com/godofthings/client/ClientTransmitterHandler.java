package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodTransmitterMenu;
import com.godofthings.network.TransmitterMessages;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 神之传输客户端侧网络处理：注册 S2C 列表 payload 并更新菜单缓存。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientTransmitterHandler
{
    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event)
    {
        event.registrar("1").playToClient(
                TransmitterMessages.TransmitterListPayload.TYPE,
                TransmitterMessages.TransmitterListPayload.STREAM_CODEC,
                ClientTransmitterHandler::handleList);
    }

    public static void handleList(TransmitterMessages.TransmitterListPayload msg, IPayloadContext ctx)
    {
        ctx.enqueueWork(() ->
        {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.containerMenu instanceof GodTransmitterMenu menu)
            {
                menu.setList(msg.online(), msg.boundPlayers(), msg.boundMachines());
            }
        });
    }
}
