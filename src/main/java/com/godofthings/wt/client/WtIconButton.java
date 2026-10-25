package com.godofthings.wt.client;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import appeng.client.gui.widgets.ITooltip;

/**
 * 图标按钮（照抄 ae2wtlib 的 {@code IconButton} 全文，[本地改动] 图标换成本模组
 * {@link WtIcon}；背景默认走本模组 icons.png 的按钮底）。
 */
public class WtIconButton extends Button implements ITooltip
{
    private final WtIcon icon;
    private final WtIcon bg;
    private final WtIcon bg_hovered;
    private final WtIcon bg_focused;
    @Nullable
    private List<Component> tooltip;

    public WtIconButton(OnPress onPress, WtIcon icon)
    {
        this(onPress, icon, WtIcon.BUTTON_BACKGROUND, WtIcon.BUTTON_BACKGROUND_HOVERED,
                WtIcon.BUTTON_BACKGROUND_FOCUSED);
    }

    public WtIconButton(OnPress onPress, WtIcon icon, WtIcon bg, WtIcon bg_hovered, WtIcon bg_focused)
    {
        super(0, 0, 16, 16, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.icon = icon;
        this.bg = bg;
        this.bg_hovered = bg_hovered;
        this.bg_focused = bg_focused;
    }

    /**
     * 用 AE2 原生工具栏按钮底（照抄 IconButton.withAE2Background——AE2 图标与本模组
     * 贴图共用绘制管线，直接引用其坐标即可）。
     */
    public static WtIconButton withAE2Background(OnPress onPress, WtIcon icon)
    {
        return new WtIconButton(onPress, icon,
                new WtIcon(176, 128, 18, 20, ae2Texture()),
                new WtIcon(212, 128, 18, 19, ae2Texture()),
                new WtIcon(194, 128, 18, 20, ae2Texture()));
    }

    private static WtIcon.Texture ae2Texture()
    {
        return new WtIcon.Texture(appeng.client.gui.Icon.TEXTURE,
                appeng.client.gui.Icon.TEXTURE_WIDTH, appeng.client.gui.Icon.TEXTURE_HEIGHT);
    }

    public void setVisibility(boolean vis)
    {
        visible = vis;
        active = vis;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial)
    {
        if (!visible)
            return;

        var yOffset = isHovered() ? 1 : 0;
        var bg = getBG();
        var bgSizeOffset = bg.width() > 16 ? 1 : 0;

        bg.getBlitter()
                .dest(getX() - 1, getY() + yOffset, bg.width(), bg.height())
                .zOffset(2)
                .blit(guiGraphics);
        getIcon().getBlitter().dest(getX() - 1 + bgSizeOffset, getY() + bgSizeOffset + yOffset).zOffset(3)
                .blit(guiGraphics);
    }

    protected WtIcon getIcon()
    {
        return icon;
    }

    private WtIcon getBG()
    {
        return isHovered() ? bg_hovered : isFocused() ? bg_focused : bg;
    }

    @Override
    public List<Component> getTooltipMessage()
    {
        if (tooltip == null)
            return Collections.singletonList(getMessage());
        return tooltip;
    }

    @Override
    public Rect2i getTooltipArea()
    {
        return new Rect2i(
                getX(),
                getY(),
                getBG().width(),
                getBG().height());
    }

    @Override
    public boolean isTooltipAreaVisible()
    {
        return visible;
    }

    public WtIconButton withTooltip(Component message)
    {
        super.setMessage(message);
        return this;
    }

    public WtIconButton withTooltip(List<Component> tooltip)
    {
        this.tooltip = tooltip;
        return this;
    }
}
