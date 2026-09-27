package com.godofthings.wand.client;

import com.godofthings.wand.network.RangeNetwork;

/** 客户端持有的范围加速历史（由 ClientStateReceiver 写入，界面读取） */
public final class RangeAccelerationHistoryState
{
    private static RangeNetwork.HistoryStatePayload latest;

    private RangeAccelerationHistoryState() {}

    public static void accept(RangeNetwork.HistoryStatePayload payload)
    {
        latest = payload;
    }

    public static RangeNetwork.HistoryStatePayload latest()
    {
        return latest;
    }
}