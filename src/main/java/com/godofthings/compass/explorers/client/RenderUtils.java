package com.godofthings.compass.explorers.client;

import com.godofthings.compass.explorers.config.ConfigHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 探险者指南针的绘制工具（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
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
        guiGraphics.drawString(font, string, x - font.width(string), y, color, true);
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
