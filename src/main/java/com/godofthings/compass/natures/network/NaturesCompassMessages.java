package com.godofthings.compass.natures.network;

import com.godofthings.Godofthings;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 自然罗盘的四条网络包注册（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>上游是在它自己的 mod 主类里注册的；本项目<b>不建第二个 {@code @Mod}</b>，改成一个独立的
 * {@code @EventBusSubscriber} 类，走本模组统一的 {@code registrar("1")} 通道（与 {@code network} 包里
 * 其它 payload 一致）。四个包的 id 都在 {@code godofthings} 命名空间下，名字带 {@code natures_compass_} 前缀
 * 以免与既有包撞名：</p>
 * <ul>
 *   <li>{@code godofthings:natures_compass_search}（C→S，选群系开搜）</li>
 *   <li>{@code godofthings:natures_compass_search_for_next}（C→S，搜下一个）</li>
 *   <li>{@code godofthings:natures_compass_teleport}（C→S，传送过去）</li>
 *   <li>{@code godofthings:natures_compass_sync}（S→C，同步可搜群系与配置）</li>
 * </ul>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NaturesCompassMessages
{
    private NaturesCompassMessages()
    {
    }

    @SubscribeEvent
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SearchPacket.TYPE, SearchPacket.CODEC, SearchPacket::handle);
        registrar.playToServer(SearchForNextPacket.TYPE, SearchForNextPacket.CODEC, SearchForNextPacket::handle);
        registrar.playToServer(TeleportPacket.TYPE, TeleportPacket.CODEC, TeleportPacket::handle);
        registrar.playToClient(SyncPacket.TYPE, SyncPacket.CODEC, SyncPacket::handle);
    }
}
