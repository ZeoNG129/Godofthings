package com.godofthings.client;

import com.godofthings.item.SwordModes;
import com.godofthings.network.SwordMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 神之剑功能切换面板：三个开关（斩首 / 捕捉 / 抢劫）可单独或同时启用。
 * 纯 Screen（非容器界面），按 J 键打开，点击行切换，Esc 关闭。
 */
public class SwordModeScreen extends Screen
{
    private final ItemStack sword;

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 108;
    private static final int ROW_X = 8;
    private static final int ROW_W = PANEL_W - 16;
    private static final int ROW_H = 18;
    private static final int ROW_Y_0 = 28;
    private static final int ROW_GAP = 24;

    public SwordModeScreen(ItemStack sword)
    {
        super(Component.translatable("gui.godofthings.sword_mode"));
        this.sword = sword;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        int x0 = (this.width - PANEL_W) / 2;
        int y0 = (this.height - PANEL_H) / 2;

        graphics.fill(x0, y0, x0 + PANEL_W, y0 + PANEL_H, 0xCC000000);
        graphics.fill(x0 + 1, y0 + 1, x0 + PANEL_W - 1, y0 + PANEL_H - 1, 0xCC202020);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, y0 + 8, 0xFFFFFF);

        drawRow(graphics, x0, y0 + ROW_Y_0, mouseX, mouseY,
                Component.translatable("gui.godofthings.sword_behead"), SwordModes.isBeheadEnabled(sword));
        drawRow(graphics, x0, y0 + ROW_Y_0 + ROW_GAP, mouseX, mouseY,
                Component.translatable("gui.godofthings.sword_capture"), SwordModes.isCaptureEnabled(sword));
        drawRow(graphics, x0, y0 + ROW_Y_0 + ROW_GAP * 2, mouseX, mouseY,
                Component.translatable("gui.godofthings.sword_looting"), SwordModes.isLootingEnabled(sword));
    }

    private void drawRow(GuiGraphics graphics, int x0, int y, int mouseX, int mouseY, Component name, boolean enabled)
    {
        int rowX = x0 + ROW_X;
        boolean hover = isHovering(rowX, y, ROW_W, ROW_H, mouseX, mouseY);

        int bg = enabled ? (hover ? 0xFF2E7D32 : 0xFF388E3C) : (hover ? 0xFF616161 : 0xFF424242);
        graphics.fill(rowX, y, rowX + ROW_W, y + ROW_H, bg);

        graphics.drawString(this.font, name, rowX + 6, y + 5, 0xFFFFFF, false);

        Component status = Component.translatable(enabled ? "gui.godofthings.sword_enabled" : "gui.godofthings.sword_disabled");
        int statusW = this.font.width(status);
        graphics.drawString(this.font, status, rowX + ROW_W - 6 - statusW, y + 5, enabled ? 0xA5D6A7 : 0xBBBBBB, false);
    }

    private static boolean isHovering(int x, int y, int w, int h, int mouseX, int mouseY)
    {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            int x0 = (this.width - PANEL_W) / 2;
            int y0 = (this.height - PANEL_H) / 2;
            int rowX = x0 + ROW_X;

            if (isHovering(rowX, y0 + ROW_Y_0, ROW_W, ROW_H, (int) mouseX, (int) mouseY))
            {
                toggle(SwordMessages.SwordMode.BEHEAD);
                return true;
            }
            if (isHovering(rowX, y0 + ROW_Y_0 + ROW_GAP, ROW_W, ROW_H, (int) mouseX, (int) mouseY))
            {
                toggle(SwordMessages.SwordMode.CAPTURE);
                return true;
            }
            if (isHovering(rowX, y0 + ROW_Y_0 + ROW_GAP * 2, ROW_W, ROW_H, (int) mouseX, (int) mouseY))
            {
                toggle(SwordMessages.SwordMode.LOOTING);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggle(SwordMessages.SwordMode mode)
    {
        // 客户端乐观更新本地 CUSTOM_DATA（即时反馈），服务端经 C2S 处理最终收敛
        switch (mode)
        {
            case BEHEAD -> SwordModes.setBeheadEnabled(sword, !SwordModes.isBeheadEnabled(sword));
            case CAPTURE -> SwordModes.setCaptureEnabled(sword, !SwordModes.isCaptureEnabled(sword));
            case LOOTING -> SwordModes.setLootingEnabled(sword, !SwordModes.isLootingEnabled(sword));
        }
        SwordMessages.send(mode);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    public static void show(ItemStack sword)
    {
        Minecraft.getInstance().setScreen(new SwordModeScreen(sword));
    }
}
