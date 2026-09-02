package com.godofthings.client;

import com.godofthings.item.SwordModes;
import com.godofthings.network.SwordMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 神之剑功能切换面板：五个开关（斩首 / 捕捉 / 抢劫 / 吸星 / 吸魂）可单独或同时启用。
 * 纯 Screen（非容器界面），按 J 键打开，点击行切换，Esc 关闭。
 */
public class SwordModeScreen extends Screen
{
    private final ItemStack sword;

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 156;
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
        drawRangeRow(graphics, x0, y0 + ROW_Y_0 + ROW_GAP * 3, mouseX, mouseY,
                Component.translatable("gui.godofthings.sword_star_absorb"),
                SwordModes.isStarAbsorbEnabled(sword), SwordModes.getStarRange(sword));
        drawRangeRow(graphics, x0, y0 + ROW_Y_0 + ROW_GAP * 4, mouseX, mouseY,
                Component.translatable("gui.godofthings.sword_soul_absorb"),
                SwordModes.isSoulAbsorbEnabled(sword), SwordModes.getSoulRange(sword));
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

    /** 带范围控件的行：名字在左，右侧是 [-] 当前值 [+]，用于吸星/吸魂半径调节。 */
    private void drawRangeRow(GuiGraphics graphics, int x0, int y, int mouseX, int mouseY,
                              Component name, boolean enabled, int range)
    {
        int rowX = x0 + ROW_X;
        boolean hover = isHovering(rowX, y, ROW_W, ROW_H, mouseX, mouseY);
        int bg = enabled ? (hover ? 0xFF2E7D32 : 0xFF388E3C) : (hover ? 0xFF616161 : 0xFF424242);
        graphics.fill(rowX, y, rowX + ROW_W, y + ROW_H, bg);

        graphics.drawString(this.font, name, rowX + 6, y + 5, 0xFFFFFF, false);

        // 范围控件（右侧）：[-] 值 [+]
        int btnW = 12, btnH = 12, btnY = y + 3;
        int plusX = rowX + ROW_W - 6 - btnW;
        String rangeText = String.valueOf(range);
        int rangeW = this.font.width(rangeText);
        int minusX = plusX - 4 - rangeW - 4 - btnW;

        boolean hoverMinus = isHovering(minusX, btnY, btnW, btnH, mouseX, mouseY);
        boolean hoverPlus = isHovering(plusX, btnY, btnW, btnH, mouseX, mouseY);
        graphics.fill(minusX, btnY, minusX + btnW, btnY + btnH, hoverMinus ? 0xFFAAAAAA : 0xFF666666);
        graphics.fill(plusX, btnY, plusX + btnW, btnY + btnH, hoverPlus ? 0xFFAAAAAA : 0xFF666666);
        graphics.drawCenteredString(this.font, "-", minusX + btnW / 2, btnY + 1, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "+", plusX + btnW / 2, btnY + 1, 0xFFFFFF);
        graphics.drawCenteredString(this.font, rangeText, (minusX + btnW + plusX) / 2, btnY + 1, 0xFFFFFF);
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
            int starY = y0 + ROW_Y_0 + ROW_GAP * 3;
            if (isHovering(rowX, starY, ROW_W, ROW_H, (int) mouseX, (int) mouseY))
            {
                int d = rangeDelta(x0, starY, SwordModes.getStarRange(sword), (int) mouseX, (int) mouseY);
                if (d != 0)
                {
                    adjustRange(SwordMessages.SwordMode.STAR_ABSORB, d);
                }
                else
                {
                    toggle(SwordMessages.SwordMode.STAR_ABSORB);
                }
                return true;
            }
            int soulY = y0 + ROW_Y_0 + ROW_GAP * 4;
            if (isHovering(rowX, soulY, ROW_W, ROW_H, (int) mouseX, (int) mouseY))
            {
                int d = rangeDelta(x0, soulY, SwordModes.getSoulRange(sword), (int) mouseX, (int) mouseY);
                if (d != 0)
                {
                    adjustRange(SwordMessages.SwordMode.SOUL_ABSORB, d);
                }
                else
                {
                    toggle(SwordMessages.SwordMode.SOUL_ABSORB);
                }
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
            case STAR_ABSORB -> SwordModes.setStarAbsorbEnabled(sword, !SwordModes.isStarAbsorbEnabled(sword));
            case SOUL_ABSORB -> SwordModes.setSoulAbsorbEnabled(sword, !SwordModes.isSoulAbsorbEnabled(sword));
        }
        SwordMessages.send(mode);
    }

    /** 判断鼠标是否落在某行的 [-] 或 [+] 按钮上，返回 -1 / +1 / 0。 */
    private int rangeDelta(int x0, int y, int range, int mouseX, int mouseY)
    {
        int rowX = x0 + ROW_X;
        int btnW = 12, btnH = 12, btnY = y + 3;
        int plusX = rowX + ROW_W - 6 - btnW;
        int rangeW = this.font.width(String.valueOf(range));
        int minusX = plusX - 4 - rangeW - 4 - btnW;
        if (isHovering(minusX, btnY, btnW, btnH, mouseX, mouseY))
        {
            return -1;
        }
        if (isHovering(plusX, btnY, btnW, btnH, mouseX, mouseY))
        {
            return 1;
        }
        return 0;
    }

    /** 客户端乐观调整本地半径（服务端经 C2S 收敛，clamp 在 SwordModes.setXxxRange 内完成）。 */
    private void adjustRange(SwordMessages.SwordMode mode, int direction)
    {
        // 通用手势：Shift=±10，Shift+Ctrl=±50，否则 ±1
        int delta = direction * GuiStep.amount();
        switch (mode)
        {
            case STAR_ABSORB -> SwordModes.setStarRange(sword, SwordModes.getStarRange(sword) + delta);
            case SOUL_ABSORB -> SwordModes.setSoulRange(sword, SwordModes.getSoulRange(sword) + delta);
            default -> { return; }
        }
        SwordMessages.sendRange(mode, delta);
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
