package com.godofthings.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * 通用数值增减鼠标手势：点击 [+] / [-] 按钮时，
 * 按住 Shift 一次 ±10，按住 Shift+Ctrl 一次 ±50，否则 ±1。
 * 本 mod 所有「增加/减少数值」的控件统一复用此步进。
 */
public final class GuiStep
{
    private GuiStep()
    {
    }

    /** 返回当前修饰键对应的单次步进量。 */
    public static int amount()
    {
        if (Screen.hasShiftDown() && Screen.hasControlDown())
        {
            return 50;
        }
        if (Screen.hasShiftDown())
        {
            return 10;
        }
        return 1;
    }
}
