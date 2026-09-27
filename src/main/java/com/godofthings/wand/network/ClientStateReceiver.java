package com.godofthings.wand.network;

/**
 * 客户端状态接收（照抄自万象担架 {@code network/ClientStateReceiver}，已裁掉"万象方块"相关部分）。
 * <p>只保留手杖用得到的范围加速历史同步；原文件里 {@code OmniversalMyriadScreen} 那套
 * 属于万象担架自己的机器界面，本项目未引入。
 */
public final class ClientStateReceiver
{
    private ClientStateReceiver() {}

    /** 范围加速历史同步（客户端） */
    public static void handleRangeHistory(RangeNetwork.HistoryStatePayload payload)
    {
        com.godofthings.wand.client.RangeAccelerationHistoryState.accept(payload);
    }
}