package com.godofthings.compass.natures.client;

import com.godofthings.compass.natures.config.ConfigHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 自然罗盘的绘制工具（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>列表行的半透明底色由 {@link #getBackgroundColor(boolean, boolean)} 统一算：
 * 可用 / 悬停 / 禁用各一档透明度。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class RenderUtils
{
    private static final Minecraft MC = Minecraft.getInstance();

    private RenderUtils()
    {
    }

    public static void drawStringLeft(GuiGraphics guiGraphics, String string, Font font, int x, int y, int color)
    {
        guiGraphics.drawString(font, string, x, y, color, true);
    }

    public static void drawStringRight(GuiGraphics guiGraphics, String string, Font font, int x, int y, int color)
    {
        guiGraphics.drawString(font, string, x - font.width(string), y, color);
    }

    /** 列表行底色（active = 可用，hovered = 悬停） */
    public static int getBackgroundColor(boolean active, boolean hovered)
    {
        float state = 2;
        if (!active)
        {
            state = 5;
        }
        else if (hovered)
        {
            state = 3;
        }
        final float f = state / 2 * 0.9F + 0.1F;
        final int color = (int) (255.0F * f);
        return color / 2 << 24;
    }

    /** 按配置（左右侧 + 行偏移）在 HUD 上画一行字 */
    public static void drawConfiguredStringOnHUD(GuiGraphics guiGraphics, String string, int xOffset, int yOffset,
            int color, int relLineOffset)
    {
        yOffset += (relLineOffset + ConfigHandler.CLIENT.overlayLineOffset.get()) * 9;
        if (ConfigHandler.CLIENT.overlaySide.get() == OverlaySide.LEFT)
        {
            drawStringLeft(guiGraphics, string, MC.font, xOffset + 2, yOffset + 2, color);
        }
        else
        {
            drawStringRight(guiGraphics, string, MC.font, MC.getWindow().getGuiScaledWidth() - xOffset - 2,
                    yOffset + 2, color);
        }
    }
}
