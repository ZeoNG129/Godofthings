package com.godofthings.compass.explorers.network;

import com.godofthings.Godofthings;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 探险者指南针的四条网络包注册（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>上游是在它自己的 mod 主类里用 {@code SimpleChannel#registerMessage(i, ...)} 按序号注册的；
 * 本项目<b>不建第二个 {@code @Mod}</b>，改成一个独立的 {@code @EventBusSubscriber} 类，走本模组统一的
 * {@code registrar("1")} 通道（与 {@code network} 包里其它 payload 一致）。四个包的 id 都在
 * {@code godofthings} 命名空间下，名字带 {@code explorers_compass_} 前缀以免与既有包撞名：</p>
 * <ul>
 *   <li>{@code godofthings:explorers_compass_search}（C→S，选结构开搜）</li>
 *   <li>{@code godofthings:explorers_compass_teleport}（C→S，传送过去）</li>
 *   <li>{@code godofthings:explorers_compass_clean_found}（C→S，清已找到区块缓存）</li>
 *   <li>{@code godofthings:explorers_compass_sync}（S→C，同步可搜结构表与传送开关）</li>
 * </ul>
 *
 * <p><b>已删除</b>：上游的 {@code NewWayPointPacket}（服务端通知客户端往小地图模组里加路径点），
 * 用户明确要求去掉那段外部地图联动，所以这条包连同 {@code connect.NewWayPoint} /
 * {@code client.HandleNewWay} 一起删了，注册表里也没有它的位置（其余包的 id 顺序不受影响 ——
 * NeoForge 用的是字符串 id 而不是序号）。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ExplorersCompassMessages
{
    private ExplorersCompassMessages()
    {
    }

    @SubscribeEvent
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CompassSearchPacket.TYPE, CompassSearchPacket.CODEC, CompassSearchPacket::handle);
        registrar.playToServer(TeleportPacket.TYPE, TeleportPacket.CODEC, TeleportPacket::handle);
        registrar.playToServer(CleanFoundPacket.TYPE, CleanFoundPacket.CODEC, CleanFoundPacket::handle);
        registrar.playToClient(SyncPacket.TYPE, SyncPacket.CODEC, SyncPacket::handle);
    }
}
