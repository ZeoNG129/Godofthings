package com.godofthings.compass.natures.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 半透明按钮（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>不画原版按钮贴图，只按状态（可用 / 悬停 / 禁用）填一块不同透明度的黑底再写居中文字，
 * 这样叠在群系列表上不会太抢眼。</p>
 */
@OnlyIn(Dist.CLIENT)
public class TransparentButton extends Button
{
    public TransparentButton(int x, int y, int width, int height, Component label, OnPress onPress)
    {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks)
    {
        if (!visible)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float state = 2;
        if (!active)
        {
            state = 5;
        }
        else if (isHovered)
        {
            state = 4;
        }
        final float f = state / 2 * 0.9F + 0.1F;
        final int color = (int) (255.0F * f);

        guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color / 2 << 24);
        guiGraphics.drawCenteredString(mc.font, getMessage(), getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2, 0xffffff);
    }
}
