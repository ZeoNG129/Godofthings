package com.godofthings.aalink;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.godofthings.Godofthings;

/**
 * 无线连接器卡的网络注册（三个包，照抄上游对应包的注册方式）。
 */
public final class ConnectorCardNetwork
{
    private ConnectorCardNetwork()
    {
    }

    /** 由 Godofthings 构造器 addListener 挂到 mod 事件总线。 */
    public static void registerPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(Godofthings.MODID).versioned("1.0");

        registrar.playToServer(
                ConnectorCardUsePayload.TYPE,
                ConnectorCardUsePayload.STREAM_CODEC,
                ConnectorCardUsePayload::handle);

        registrar.playToServer(
                ConnectorCardToggleAutoPayload.TYPE,
                ConnectorCardToggleAutoPayload.STREAM_CODEC,
                ConnectorCardToggleAutoPayload::handle);

        registrar.playToServer(
                ConnectorCardLinkShortcutPayload.TYPE,
                ConnectorCardLinkShortcutPayload.STREAM_CODEC,
                ConnectorCardLinkShortcutPayload::handle);
    }
}
