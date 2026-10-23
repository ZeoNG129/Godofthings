package com.godofthings.doll.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 玩偶界面的配色与绘制工具。
 *
 * <p>原 mod 的 {@code DollScreenStyle} 调 AE2 的 {@code BackgroundGenerator.draw(...)}
 * 来画面板背景 —— 那会把玩偶界面绑死在 AE2 的客户端类上。移植版改成纯 {@code fill} 手绘，
 * 配色沿用原来的十六进制值，观感一致但不依赖 AE2。</p>
 */
public final class DollScreenStyle
{
    /** 面板底色 */
    public static final int PANEL_COLOR = 0xFFCBD6D4;
    /** 高亮色（内凹描边） */
    public static final int HIGHLIGHT_COLOR = 0xFFF2F3F2;
    /** 槽位底色 */
    public static final int SLOT_COLOR = 0xFFADB4B4;
    /** 槽位阴影 */
    public static final int SLOT_SHADOW_COLOR = 0xFF9A9C94;
    /** 正文色 */
    public static final int TEXT_COLOR = 0xFF414A54;
    /** 次要文字 */
    public static final int MUTED_TEXT_COLOR = 0xFF87A0A5;
    /** 更淡的文字 */
    public static final int SUBTLE_TEXT_COLOR = 0xFF6D7977;
    /** 激活态（青色，与模组主题一致） */
    public static final int ACTIVE_COLOR = 0xFF516E17;
    /** 成功色 */
    public static final int SUCCESS_COLOR = 0xFF3B8C52;
    /** 警告色 */
    public static final int WARNING_COLOR = 0xFF9A6A38;

    private DollScreenStyle()
    {
    }

    /** 画一块面板：外描边 + 内底色 */
    public static void drawPanel(GuiGraphics graphics, int left, int top, int width, int height)
    {
        graphics.fill(left, top, left + width, top + height, HIGHLIGHT_COLOR);
        graphics.fill(left + 1, top + 1, left + width - 1, top + height - 1, PANEL_COLOR);
    }

    /** 画一个内凹区域（对话框、槽位底板） */
    public static void drawInset(GuiGraphics graphics, int left, int top, int right, int bottom)
    {
        graphics.fill(left, top, right, bottom, SLOT_SHADOW_COLOR);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, SLOT_COLOR);
    }
}
