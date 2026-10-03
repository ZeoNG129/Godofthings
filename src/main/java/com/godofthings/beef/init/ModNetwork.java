package com.godofthings.beef.init;

import com.godofthings.beef.network.AeLinkPreviewPacket;
import com.godofthings.beef.network.AeLinkPreviewRequestPacket;
import com.godofthings.beef.network.BeefInvulnerabilityStatePacket;
import com.godofthings.beef.network.BeefInvulnerabilitySyncPacket;
import com.godofthings.beef.network.DimensionConfigGhostSlotPacket;
import com.godofthings.beef.network.DimensionConfigSubmitPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 本模组（beef 子系统）的网络包注册。
 *
 * <p><b>本次死代码清理</b>：牛排工具框架已整套删除，原先为它注册的 19 个包
 * （模式轮盘布局 4 个、附魔 / 工具形态切换、强制破坏、Tab 连锁、短距传送、连点模式切换、
 * 挖掘数据同步、建筑手杖 3 个、匠心仪式挎包、模式开关）连同各自的类一并移除；
 * 这里只剩三块仍然活着的：</p>
 * <ul>
 *   <li><b>AE 连接预览</b>（{@link AeLinkPreviewRequestPacket} / {@link AeLinkPreviewPacket}）；</li>
 *   <li><b>玩家保护状态同步</b>（{@link BeefInvulnerabilityStatePacket} /
 *       {@link BeefInvulnerabilitySyncPacket}）；</li>
 *   <li><b>无用维度配置界面</b>（{@link DimensionConfigGhostSlotPacket} /
 *       {@link DimensionConfigSubmitPacket}）。</li>
 * </ul>
 *
 * <p>通道版本是模组级设置而非子系统逻辑，沿用既有的 {@code event.registrar("1")}。</p>
 */
public class ModNetwork {
    public static void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(AeLinkPreviewRequestPacket.TYPE, AeLinkPreviewRequestPacket.STREAM_CODEC,
                               AeLinkPreviewRequestPacket::handle
        );
        registrar.playToClient(AeLinkPreviewPacket.TYPE, AeLinkPreviewPacket.STREAM_CODEC,
                               AeLinkPreviewPacket::handle
        );
        registrar.playToClient(BeefInvulnerabilitySyncPacket.TYPE, BeefInvulnerabilitySyncPacket.STREAM_CODEC,
                               BeefInvulnerabilitySyncPacket::handle
        );
        registrar.playToClient(BeefInvulnerabilityStatePacket.TYPE, BeefInvulnerabilityStatePacket.STREAM_CODEC,
                               BeefInvulnerabilityStatePacket::handle
        );
        // 无用维度配置界面（照抄上游 ModNetwork 的同名两项）
        registrar.playToServer(DimensionConfigGhostSlotPacket.TYPE,
                               DimensionConfigGhostSlotPacket.STREAM_CODEC,
                               DimensionConfigGhostSlotPacket::handle);
        registrar.playToServer(DimensionConfigSubmitPacket.TYPE,
                               DimensionConfigSubmitPacket.STREAM_CODEC,
                               DimensionConfigSubmitPacket::handle);
    }
}
