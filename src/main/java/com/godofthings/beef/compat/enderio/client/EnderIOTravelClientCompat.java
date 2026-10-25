package com.godofthings.beef.compat.enderio.client;

/**
 * Ender IO 旅行锚客户端兼容的本地空壳（[本地改动]）：开发环境无 enderio 依赖，空实现。
 */
public final class EnderIOTravelClientCompat
{
    /** 空壳：不需要抑制 enderio 的旅行键。 */
    public static boolean suppressTravelKey()
    {
        return false;
    }

    private EnderIOTravelClientCompat()
    {
    }
}
