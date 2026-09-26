package com.godofthings.client.screen;

import com.godofthings.armor.GodArmorFeatures;
import com.godofthings.armor.GodArmorState;
import com.godofthings.handler.GodArmorHandler;
import com.godofthings.network.ArmorMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 神之套装功能开关界面：2 列 × 6 行开关 + 全部开启 / 全部关闭 / 返回。
 * <p>
 * 点击只改本地镜像并立即重绘（乐观更新），同时把整张位图发给服务端；
 * 服务端写完后会回推权威值，界面随之收敛。
 */
public class GodArmorConfigScreen extends Screen
{
    private static final int PANEL_W = 320;
    private static final int PANEL_H = 200;

    private static final int COL_X_0 = 8;
    private static final int COL_X_1 = 162;
    private static final int COL_W = 150;
    private static final int BTN_H = 18;
    private static final int ROW_Y_0 = 28;
    private static final int ROW_GAP = 22;

    private static final int HINT_Y = 162;
    private static final int BOTTOM_Y = 176;
    private static final int BOTTOM_W = 96;
    private static final int BOTTOM_H = 18;
    private static final int BOTTOM_X_0 = 8;
    private static final int BOTTOM_X_1 = 112;
    private static final int BOTTOM_X_2 = 216;

    private static final int BG_OUTER = 0xCC000000;
    private static final int BG_INNER = 0xCC202020;
    private static final int BTN_OFF_BG = 0xFF3A4048;
    private static final int BTN_ON_BG = 0xFF2E7D32;
    private static final int BTN_BORDER = 0xFF16181D;
    private static final int TXT_ON = 0xFFFFFFFF;
    private static final int TXT_OFF = 0xFF9AA0A8;

    public GodArmorConfigScreen()
    {
        super(Component.translatable("gui.godofthings.armor.config"));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        int x0 = (this.width - PANEL_W) / 2;
        int y0 = (this.height - PANEL_H) / 2;

        gui.fill(x0, y0, x0 + PANEL_W, y0 + PANEL_H, BG_OUTER);
        gui.fill(x0 + 1, y0 + 1, x0 + PANEL_W - 1, y0 + PANEL_H - 1, BG_INNER);

        gui.drawCenteredString(this.font, this.title, this.width / 2, y0 + 8, 0xFFFFFF);

        int mask = GodArmorState.getClientMask();
        for (int i = 0; i < GodArmorFeatures.COUNT; i++)
        {
            int col = i / 6;
            int row = i % 6;
            int bx = x0 + (col == 0 ? COL_X_0 : COL_X_1);
            int by = y0 + ROW_Y_0 + row * ROW_GAP;
            drawToggle(gui, bx, by, i, (mask & (1 << i)) != 0, mouseX, mouseY);
        }

        // 未穿齐全套时给个提示（开关仍可设置，穿上后按开关生效）
        boolean fullSet = Minecraft.getInstance().player != null
                && GodArmorHandler.isFullSetWorn(Minecraft.getInstance().player);
        Component hint = Component.translatable(fullSet
                ? "gui.godofthings.armor.config.hint"
                : "gui.godofthings.armor.config.hint_no_set");
        gui.drawCenteredString(this.font, hint, this.width / 2, y0 + HINT_Y, fullSet ? 0x808080 : 0xC8A03A);

        drawButton(gui, x0 + BOTTOM_X_0, y0 + BOTTOM_Y, BOTTOM_W, Component.translatable("gui.godofthings.armor.all_on"), mouseX, mouseY);
        drawButton(gui, x0 + BOTTOM_X_1, y0 + BOTTOM_Y, BOTTOM_W, Component.translatable("gui.godofthings.armor.all_off"), mouseX, mouseY);
        drawButton(gui, x0 + BOTTOM_X_2, y0 + BOTTOM_Y, BOTTOM_W, Component.translatable("gui.godofthings.back"), mouseX, mouseY);
    }

    private void drawToggle(GuiGraphics gui, int bx, int by, int feature, boolean on, int mouseX, int mouseY)
    {
        boolean hovered = isHovering(bx, by, COL_W, BTN_H, mouseX, mouseY);
        gui.fill(bx, by, bx + COL_W, by + BTN_H, BTN_BORDER);
        gui.fill(bx + 1, by + 1, bx + COL_W - 1, by + BTN_H - 1, on ? BTN_ON_BG : BTN_OFF_BG);

        Component label = Component.translatable(GodArmorFeatures.LANG_KEYS[feature]);
        gui.drawString(this.font, label, bx + 4, by + 5, on ? TXT_ON : TXT_OFF, false);

        Component state = Component.translatable(on
                ? "gui.godofthings.armor.on"
                : "gui.godofthings.armor.off");
        int sx = bx + COL_W - 4 - this.font.width(state);
        gui.drawString(this.font, state, sx, by + 5, on ? TXT_ON : TXT_OFF, false);

        if (hovered)
        {
            gui.fill(bx, by, bx + COL_W, by + 1, 0xFFFFFFFF);
            gui.fill(bx, by + BTN_H - 1, bx + COL_W, by + BTN_H, 0xFFFFFFFF);
        }
    }

    private void drawButton(GuiGraphics gui, int bx, int by, int w, Component label, int mouseX, int mouseY)
    {
        boolean hovered = isHovering(bx, by, w, BOTTOM_H, mouseX, mouseY);
        gui.fill(bx, by, bx + w, by + BOTTOM_H, BTN_BORDER);
        gui.fill(bx + 1, by + 1, bx + w - 1, by + BOTTOM_H - 1, hovered ? 0xFF4E5660 : BTN_OFF_BG);
        gui.drawString(this.font, label, bx + (w - this.font.width(label)) / 2, by + 5, 0xFFFFFF, false);
    }

    private static boolean isHovering(int bx, int by, int w, int h, int mouseX, int mouseY)
    {
        return mouseX >= bx && mouseX < bx + w && mouseY >= by && mouseY < by + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int x0 = (this.width - PANEL_W) / 2;
        int y0 = (this.height - PANEL_H) / 2;
        int mx = (int) mouseX;
        int my = (int) mouseY;
        int mask = GodArmorState.getClientMask();

        for (int i = 0; i < GodArmorFeatures.COUNT; i++)
        {
            int col = i / 6;
            int row = i % 6;
            int bx = x0 + (col == 0 ? COL_X_0 : COL_X_1);
            int by = y0 + ROW_Y_0 + row * ROW_GAP;
            if (isHovering(bx, by, COL_W, BTN_H, mx, my))
            {
                apply(mask ^ (1 << i)); // 乐观翻转，服务端回推后收敛
                return true;
            }
        }

        if (isHovering(x0 + BOTTOM_X_0, y0 + BOTTOM_Y, BOTTOM_W, BOTTOM_H, mx, my))
        {
            apply(GodArmorFeatures.ALL);
            return true;
        }
        if (isHovering(x0 + BOTTOM_X_1, y0 + BOTTOM_Y, BOTTOM_W, BOTTOM_H, mx, my))
        {
            apply(0);
            return true;
        }
        if (isHovering(x0 + BOTTOM_X_2, y0 + BOTTOM_Y, BOTTOM_W, BOTTOM_H, mx, my))
        {
            this.onClose();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void apply(int mask)
    {
        GodArmorState.setClientMask(mask);
        ArmorMessages.sendMask(GodArmorState.getClientMask());
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
