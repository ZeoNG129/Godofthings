package com.godofthings.wt.client;

/**
 * 升级槽面板背景三段贴图（照抄 ae2wtlib 的 {@code UpgradeBackground} 全文）。
 */
public record WtUpgradeBackground(WtIcon top, WtIcon middle, WtIcon bottom)
{

    private static final WtUpgradeBackground SCROLLING = new WtUpgradeBackground(
            WtIcon.UPGRADE_BACKGROUND_SCROLLING_TOP,
            WtIcon.UPGRADE_BACKGROUND_SCROLLING_MIDDLE, WtIcon.UPGRADE_BACKGROUND_SCROLLING_BOTTOM);
    private static final WtUpgradeBackground FIXED = new WtUpgradeBackground(WtIcon.UPGRADE_BACKGROUND_TOP,
            WtIcon.UPGRADE_BACKGROUND_MIDDLE, WtIcon.UPGRADE_BACKGROUND_BOTTOM);

    public static WtUpgradeBackground get(boolean scrolling)
    {
        return scrolling ? SCROLLING : FIXED;
    }
}
