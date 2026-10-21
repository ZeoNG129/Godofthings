package com.godofthings.compass.natures.client;

/**
 * HUD 信息显示在哪一侧（配置项 {@code overlaySide} 的枚举值）。
 *
 * <p>注意：本类虽然放在 {@code client} 包里，但<b>不 import 任何 {@code net.minecraft.client.*}</b> ——
 * 它被 {@code ConfigHandler}（服务端也会加载的 COMMON spec 类）引用，所以必须保证服务端能安全加载。</p>
 */
public enum OverlaySide
{
    LEFT, RIGHT;

    public static OverlaySide fromString(String str)
    {
        if (str.equals("RIGHT"))
        {
            return RIGHT;
        }
        return LEFT;
    }
}
