package com.godofthings.beef.init;

import com.godofthings.beef.network.DimensionConfigGhostSlotPacket;
import com.godofthings.beef.network.DimensionConfigSubmitPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 本模组（beef 子系统）的网络包注册。
 *
 * <p><b>死代码清理历程</b>：</p>
 * <ol>
 *   <li>牛排工具框架已整套删除，原先为它注册的 19 个包（模式轮盘布局 4 个、附魔 / 工具形态切换、
 *       强制破坏、Tab 连锁、短距传送、连点模式切换、挖掘数据同步、建筑手杖 3 个、匠心仪式挎包、
 *       模式开关）连同各自的类一并移除；</li>
 *   <li><b>AE 无线接入点连接子系统</b>已删除（{@code AeLinkPreviewRequestPacket} /
 *       {@code AeLinkPreviewPacket} 与 {@code AeDeviceLinker} /
 *       {@code AeLinkChannelBypass} / {@code AeConnectLinkSavedData} 整个子系统），
 *       两个连接预览包随之注销；</li>
 *   <li><b>玩家保护层（无敌 / 高级隐身）</b>已删除（{@code BeefInvulnerabilityStatePacket} /
 *       {@code BeefInvulnerabilitySyncPacket} 与其客户端处理器），两个状态同步包随之注销。</li>
 * </ol>
 *
 * <p>如今只剩<b>无用维度配置界面</b>（{@link DimensionConfigGhostSlotPacket} /
 * {@link DimensionConfigSubmitPacket}）。</p>
 *
 * <p>通道版本是模组级设置而非子系统逻辑，沿用既有的 {@code event.registrar("1")}。</p>
 */
public class ModNetwork {
    public static void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        // 无用维度配置界面（照抄上游 ModNetwork 的同名两项）
        registrar.playToServer(DimensionConfigGhostSlotPacket.TYPE,
                               DimensionConfigGhostSlotPacket.STREAM_CODEC,
                               DimensionConfigGhostSlotPacket::handle);
        registrar.playToServer(DimensionConfigSubmitPacket.TYPE,
                               DimensionConfigSubmitPacket.STREAM_CODEC,
                               DimensionConfigSubmitPacket::handle);
    }
}
