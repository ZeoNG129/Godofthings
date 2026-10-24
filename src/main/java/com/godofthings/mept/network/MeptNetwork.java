package com.godofthings.mept.network;

import com.godofthings.Godofthings;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * ME 放置工具的网络注册（照抄上游 ModNetwork，包名改 godofthings 命名空间）。
 */
public final class MeptNetwork
{
    private MeptNetwork()
    {
    }

    /** 由 Godofthings 构造器 addListener 挂到 mod 事件总线（RegisterPayloadHandlersEvent）。 */
    public static void registerPayloads(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(Godofthings.MODID)
                .versioned("1.0");

        // 客户端 -> 服务端
        registrar.playToServer(
                UpdateWandConfigPayload.TYPE,
                UpdateWandConfigPayload.STREAM_CODEC,
                UpdateWandConfigPayload::handle
        );

        registrar.playToServer(
                UpdatePlacementCountPayload.TYPE,
                UpdatePlacementCountPayload.STREAM_CODEC,
                UpdatePlacementCountPayload::handle
        );

        registrar.playToServer(
                UpdateDirectionModePayload.TYPE,
                UpdateDirectionModePayload.STREAM_CODEC,
                UpdateDirectionModePayload::handle
        );

        registrar.playToServer(
                UndoPayload.TYPE,
                UndoPayload.STREAM_CODEC,
                UndoPayload::handle
        );

        registrar.playToServer(
                SyncPagePayload.TYPE,
                SyncPagePayload.STREAM_CODEC,
                SyncPagePayload::handle
        );

        registrar.playToServer(
                UpdateWandSlotPayload.TYPE,
                UpdateWandSlotPayload.STREAM_CODEC,
                UpdateWandSlotPayload::handle
        );

        registrar.playToServer(
                UpdateCableToolPayload.TYPE,
                UpdateCableToolPayload.STREAM_CODEC,
                UpdateCableToolPayload::handle
        );

        registrar.playToServer(
                OpenCableToolGuiPayload.TYPE,
                OpenCableToolGuiPayload.STREAM_CODEC,
                OpenCableToolGuiPayload::handle
        );

        registrar.playToServer(
                ClearCableToolPointsPayload.TYPE,
                ClearCableToolPointsPayload.STREAM_CODEC,
                ClearCableToolPointsPayload::handle
        );

        // 服务端 -> 客户端
        registrar.playToClient(
                SyncCableToolPointsPayload.TYPE,
                SyncCableToolPointsPayload.STREAM_CODEC,
                SyncCableToolPointsPayload::handle
        );
    }
}
