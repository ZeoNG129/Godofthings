package com.godofthings.compass.explorers.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 半透明按钮（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>不画原版按钮贴图，只按状态（可用 / 悬停 / 禁用）填一块不同透明度的黑底再画一圈边框，
 * 这样叠在结构列表上不会太抢眼。状态值（2 普通 / 4 悬停 / 5 禁用）与原版一致，边框亮度反过来取。</p>
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
        drawBorder(guiGraphics, getX(), getY(), getWidth(), getHeight(), state);
        guiGraphics.drawCenteredString(mc.font, getMessage(), getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2, 0xffffff);
    }

    /** 边框亮度与底色反着来（禁用时亮、普通时暗），和上游一致 */
    private void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, float state)
    {
        final int borderState = state == 2 ? 5 : 2;
        final int borderColor = 0x33333333 * borderState;
        // 上边框
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        // 下边框
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        // 左边框
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        // 右边框
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }
}
