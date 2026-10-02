package com.godofthings.client;

import com.godofthings.client.screen.ManualScreen;

/**
 * 打开手册的唯一客户端入口。
 *
 * <p>单独放一个类是为了把「客户端类」的引用收在一处：服务端的物品 / 指令分支都不会走到这里，
 * 也就不会去解析 {@link ManualScreen}。</p>
 */
public final class ManualOpener
{
    private ManualOpener() {}

    public static void open()
    {
        ManualScreen.open();
    }
}
