package com.godofthings.wt.client;

import net.minecraft.resources.ResourceLocation;

import appeng.client.gui.style.Blitter;
import appeng.core.AppEng;

/**
 * 终端界面图标（照抄 ae2wtlib 的 {@code Icon}，[本地改动] 贴图命名空间与路径改为
 * {@code godofthings:textures/wutfree/guis/icons.png}；AE2 原生图标直接复用）。
 */
public record WtIcon(int x, int y, int width, int height, Texture texture)
{
    public static final Texture TEXTURE = new Texture(AppEng.makeId("textures/wutfree/guis/icons.png"), 128, 128);

    // 本模组 icons.png 里的图标（贴图即上游 wtlib 的 icons.png 原样拷贝）
    public static final WtIcon BUTTON_BACKGROUND = new WtIcon(63, 0, 16, 17);
    public static final WtIcon BUTTON_BACKGROUND_HOVERED = new WtIcon(95, 1, 16, 16);
    public static final WtIcon BUTTON_BACKGROUND_FOCUSED = new WtIcon(79, 0, 16, 17);

    public static final WtIcon MAGNET = new WtIcon(0, 0);
    public static final WtIcon TRASH = new WtIcon(0, 32);

    public static final WtIcon PATTERN_ACCESS = new WtIcon(16, 0);
    public static final WtIcon PATTERN_ENCODING = new WtIcon(16, 16);
    public static final WtIcon CRAFTING = new WtIcon(16, 32);

    public static final WtIcon NO = new WtIcon(32, 0);
    public static final WtIcon YES = new WtIcon(32, 16);
    public static final WtIcon UP = new WtIcon(32, 32);
    public static final WtIcon DOWN = new WtIcon(32, 48);
    public static final WtIcon SWITCH = new WtIcon(32, 64);

    /** AE2 原生「终端设置」图标（照抄 Icon.TERMINAL_SETTINGS，贴图走 AE2 的 state.png）。 */
    public static final WtIcon TERMINAL_SETTINGS_AE2 = new WtIcon(32, 65, 16, 15, ae2Texture());

    public static final WtIcon EMPTY_ARMOR_SLOT_HELMET = new WtIcon(112, 0);
    public static final WtIcon EMPTY_ARMOR_SLOT_CHESTPLATE = new WtIcon(112, 16);
    public static final WtIcon EMPTY_ARMOR_SLOT_LEGGINGS = new WtIcon(112, 32);
    public static final WtIcon EMPTY_ARMOR_SLOT_BOOTS = new WtIcon(112, 48);
    public static final WtIcon EMPTY_ARMOR_SLOT_SHIELD = new WtIcon(112, 64);

    public static final WtIcon UPGRADE_BACKGROUND_TOP = new WtIcon(77, 62, 23, 23);
    public static final WtIcon UPGRADE_BACKGROUND_MIDDLE = new WtIcon(77, 85, 23, 18);
    public static final WtIcon UPGRADE_BACKGROUND_BOTTOM = new WtIcon(77, 103, 23, 25);
    public static final WtIcon UPGRADE_BACKGROUND_SCROLLING_TOP = new WtIcon(48, 62, 29, 23);
    public static final WtIcon UPGRADE_BACKGROUND_SCROLLING_MIDDLE = new WtIcon(48, 85, 29, 18);
    public static final WtIcon UPGRADE_BACKGROUND_SCROLLING_BOTTOM = new WtIcon(48, 103, 29, 25);

    public WtIcon(int x, int y)
    {
        this(x, y, 16, 16);
    }

    public WtIcon(int x, int y, int width, int height)
    {
        this(x, y, width, height, TEXTURE);
    }

    private static WtIcon.Texture ae2Texture()
    {
        return new WtIcon.Texture(appeng.client.gui.Icon.TEXTURE,
                appeng.client.gui.Icon.TEXTURE_WIDTH, appeng.client.gui.Icon.TEXTURE_HEIGHT);
    }

    public Blitter getBlitter()
    {
        return Blitter.texture(texture.location(), texture.width(), texture.height())
                .src(x, y, width, height);
    }

    public record Texture(ResourceLocation location, int width, int height)
    {
    }
}
