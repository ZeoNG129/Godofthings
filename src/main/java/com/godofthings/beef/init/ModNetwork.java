package com.godofthings.beef.init;

import com.godofthings.beef.network.AeLinkPreviewPacket;
import com.godofthings.beef.network.AeLinkPreviewRequestPacket;
import com.godofthings.beef.network.BeefInvulnerabilityStatePacket;
import com.godofthings.beef.network.BeefInvulnerabilitySyncPacket;
import com.godofthings.beef.network.BeefToolLayoutRequestPacket;
import com.godofthings.beef.network.BeefToolLayoutResultPacket;
import com.godofthings.beef.network.BeefToolLayoutSyncPacket;
import com.godofthings.beef.network.BeefToolLayoutUpdatePacket;
import com.godofthings.beef.network.ConstructionWandCorePacket;
import com.godofthings.beef.network.ConstructionWandPreviewPacket;
import com.godofthings.beef.network.ConstructionWandPreviewRequestPacket;
import com.godofthings.beef.network.EnchantmentSwitchPacket;
import com.godofthings.beef.network.ForceBreakKeyPacket;
import com.godofthings.beef.network.MiningDataSyncPacket;
import com.godofthings.beef.network.ModeTogglePacket;
import com.godofthings.beef.network.RitualSatchelPlacePacket;
import com.godofthings.beef.network.StaffLinkBindPacket;
import com.godofthings.beef.network.StaffLinkConfigurePacket;
import com.godofthings.beef.network.StaffLinkCyclePacket;
import com.godofthings.beef.network.StaffLinkDetachPacket;
import com.godofthings.beef.network.StaffLinkHighlightPacket;
import com.godofthings.beef.network.StaffLinkHighlightRequestPacket;
import com.godofthings.beef.network.StaffLinkNetworkPacket;
import com.godofthings.beef.network.StaffLinkOpenPacket;
import com.godofthings.beef.network.StaffLinkRenamePacket;
import com.godofthings.beef.network.StaffLinkReorderPacket;
import com.godofthings.beef.network.StaffLinkStatusPacket;
import com.godofthings.beef.network.StaffLinkSyncPacket;
import com.godofthings.beef.network.TabKeyPressedPacket;
import com.godofthings.beef.network.TeleportKeyPacket;
import com.godofthings.beef.network.ToolTypeModeSwitchPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 造化杖的网络包注册。
 *
 * <p>逐字照抄上游 {@code com.sorrowmist.useless.init.ModNetwork} 中与工具相关的注册项；
 * 上游的机器/维度配置包（合金炉、发电机、维度配置、被动合成、AE 任务进度等）属于机器子系统，
 * 未随本次照抄带入。</p>
 *
 * <p>唯一的适配点：上游用 {@code event.registrar(MODID).versioned("13")}，
 * 而本模组既有的 9 处网络注册统一使用 {@code event.registrar("1")}，
 * 通道版本是模组级设置而非工具逻辑，故这里与之保持一致。</p>
 */
public class ModNetwork {
    public static void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(BeefToolLayoutRequestPacket.TYPE,
                               BeefToolLayoutRequestPacket.STREAM_CODEC,
                               BeefToolLayoutRequestPacket::handle);
        registrar.playToClient(BeefToolLayoutSyncPacket.TYPE,
                               BeefToolLayoutSyncPacket.STREAM_CODEC,
                               BeefToolLayoutSyncPacket::handle);
        registrar.playToServer(BeefToolLayoutUpdatePacket.TYPE,
                               BeefToolLayoutUpdatePacket.STREAM_CODEC,
                               BeefToolLayoutUpdatePacket::handle);
        registrar.playToClient(BeefToolLayoutResultPacket.TYPE,
                               BeefToolLayoutResultPacket.STREAM_CODEC,
                               BeefToolLayoutResultPacket::handle);
        registrar.playToServer(EnchantmentSwitchPacket.TYPE, EnchantmentSwitchPacket.STREAM_CODEC,
                               EnchantmentSwitchPacket::handle
        );
        registrar.playToServer(ToolTypeModeSwitchPacket.TYPE, ToolTypeModeSwitchPacket.STREAM_CODEC,
                               ToolTypeModeSwitchPacket::handle
        );
        registrar.playToServer(ConstructionWandCorePacket.TYPE, ConstructionWandCorePacket.STREAM_CODEC,
                               ConstructionWandCorePacket::handle
        );
        registrar.playToServer(AeLinkPreviewRequestPacket.TYPE, AeLinkPreviewRequestPacket.STREAM_CODEC,
                               AeLinkPreviewRequestPacket::handle
        );
        registrar.playToClient(AeLinkPreviewPacket.TYPE, AeLinkPreviewPacket.STREAM_CODEC,
                               AeLinkPreviewPacket::handle
        );
        registrar.playToServer(ConstructionWandPreviewRequestPacket.TYPE,
                               ConstructionWandPreviewRequestPacket.STREAM_CODEC,
                               ConstructionWandPreviewRequestPacket::handle
        );
        registrar.playToClient(ConstructionWandPreviewPacket.TYPE,
                               ConstructionWandPreviewPacket.STREAM_CODEC,
                               ConstructionWandPreviewPacket::handle
        );
        registrar.playToServer(TabKeyPressedPacket.TYPE, TabKeyPressedPacket.STREAM_CODEC,
                               TabKeyPressedPacket::handle
        );
        registrar.playToServer(TeleportKeyPacket.TYPE, TeleportKeyPacket.STREAM_CODEC,
                               TeleportKeyPacket::handle
        );
        registrar.playToServer(ModeTogglePacket.TYPE, ModeTogglePacket.STREAM_CODEC,
                               ModeTogglePacket::handle
        );
        registrar.playToServer(ForceBreakKeyPacket.TYPE, ForceBreakKeyPacket.STREAM_CODEC,
                               ForceBreakKeyPacket::handle
        );
        registrar.playToClient(MiningDataSyncPacket.TYPE, MiningDataSyncPacket.STREAM_CODEC,
                               MiningDataSyncPacket::handle
        );
        registrar.playToClient(BeefInvulnerabilitySyncPacket.TYPE, BeefInvulnerabilitySyncPacket.STREAM_CODEC,
                               BeefInvulnerabilitySyncPacket::handle
        );
        registrar.playToClient(BeefInvulnerabilityStatePacket.TYPE, BeefInvulnerabilityStatePacket.STREAM_CODEC,
                               BeefInvulnerabilityStatePacket::handle
        );
        registrar.playToServer(RitualSatchelPlacePacket.TYPE,
                               RitualSatchelPlacePacket.STREAM_CODEC,
                               RitualSatchelPlacePacket::handle);
        registrar.playToServer(StaffLinkOpenPacket.TYPE,
                               StaffLinkOpenPacket.STREAM_CODEC,
                               StaffLinkOpenPacket::handle);
        registrar.playToServer(StaffLinkBindPacket.TYPE,
                               StaffLinkBindPacket.STREAM_CODEC,
                               StaffLinkBindPacket::handle);
        registrar.playToServer(StaffLinkDetachPacket.TYPE,
                               StaffLinkDetachPacket.STREAM_CODEC,
                               StaffLinkDetachPacket::handle);
        registrar.playToServer(StaffLinkReorderPacket.TYPE,
                               StaffLinkReorderPacket.STREAM_CODEC,
                               StaffLinkReorderPacket::handle);
        registrar.playToServer(StaffLinkConfigurePacket.TYPE,
                               StaffLinkConfigurePacket.STREAM_CODEC,
                               StaffLinkConfigurePacket::handle);
        registrar.playToServer(StaffLinkRenamePacket.TYPE,
                               StaffLinkRenamePacket.STREAM_CODEC,
                               StaffLinkRenamePacket::handle);
        registrar.playToServer(StaffLinkCyclePacket.TYPE,
                               StaffLinkCyclePacket.STREAM_CODEC,
                               StaffLinkCyclePacket::handle);
        registrar.playToServer(StaffLinkNetworkPacket.TYPE,
                               StaffLinkNetworkPacket.STREAM_CODEC,
                               StaffLinkNetworkPacket::handle);
        registrar.playToClient(StaffLinkSyncPacket.TYPE,
                               StaffLinkSyncPacket.STREAM_CODEC,
                               StaffLinkSyncPacket::handle);
        registrar.playToClient(StaffLinkStatusPacket.TYPE,
                               StaffLinkStatusPacket.STREAM_CODEC,
                               StaffLinkStatusPacket::handle);
        registrar.playToServer(StaffLinkHighlightRequestPacket.TYPE,
                               StaffLinkHighlightRequestPacket.STREAM_CODEC,
                               StaffLinkHighlightRequestPacket::handle);
        registrar.playToClient(StaffLinkHighlightPacket.TYPE,
                               StaffLinkHighlightPacket.STREAM_CODEC,
                               StaffLinkHighlightPacket::handle);
    }
}
