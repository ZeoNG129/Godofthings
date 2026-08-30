package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodChangeMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之更改界面：调整时间（早/午/晚）与天气（晴/雨/雷暴）。
 * 深色面板 + 金色边框，两个区块各 3 个按钮。
 */
public class GodChangeScreen extends AbstractContainerScreen<GodChangeMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_change.png");

    private static final int IMG_W = 176;
    private static final int IMG_H = 120;

    // 区块布局
    private static final int SECTION_TOP = 16;
    private static final int SECTION_H = 40;
    private static final int SECTION_GAP = 8;
    private static final int TIME_SECTION_Y = SECTION_TOP;
    private static final int WEATHER_SECTION_Y = TIME_SECTION_Y + SECTION_H + SECTION_GAP;

    // 按钮：3 个横排
    private static final int BTN_Y = 30;
    private static final int BTN_W = 50;
    private static final int BTN_H = 20;
    private static final int[] BTN_X = { 8, 63, 118 };
    private static final int[] TIME_BTN_Y = { BTN_Y, BTN_Y, BTN_Y };
    private static final int[] WEATHER_BTN_Y = { BTN_Y, BTN_Y, BTN_Y };

    public GodChangeScreen(GodChangeMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = IMG_W;
        this.imageHeight = IMG_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 两个区块标题
        gui.drawString(this.font, Component.translatable("gui.godofthings.god_change.time_section"),
                x + 8, y + 20, 0xFFE0B030);
        gui.drawString(this.font, Component.translatable("gui.godofthings.god_change.weather_section"),
                x + 8, y + 68, 0xFFE0B030);

        // 时间按钮（早上/中午/晚上）
        for (int i = 0; i < 3; i++)
        {
            drawButton(gui, x, y, BTN_X[i], TIME_SECTION_Y + BTN_Y, BTN_W, BTN_H,
                    Component.translatable("gui.godofthings.god_change.time_" + i),
                    0xFF3F7F3F);
        }
        // 天气按钮（晴朗/下雨/雷暴）
        for (int i = 0; i < 3; i++)
        {
            drawButton(gui, x, y, BTN_X[i], WEATHER_SECTION_Y + BTN_Y, BTN_W, BTN_H,
                    Component.translatable("gui.godofthings.god_change.weather_" + i),
                    0xFF3F5F8F);
        }
    }

    /** 画一个带边框的立体按钮 */
    private void drawButton(GuiGraphics gui, int ox, int oy, int bx, int by, int w, int h,
                            Component label, int baseColor)
    {
        int gx = ox + bx;
        int gy = oy + by;
        // 外边框（深色）
        gui.fill(gx, gy, gx + w, gy + h, 0xFF16181D);
        // 内部主体
        gui.fill(gx + 1, gy + 1, gx + w - 1, gy + h - 1, baseColor);
        // 顶部高光
        gui.fill(gx + 1, gy + 1, gx + w - 1, gy + 4, lighten(baseColor));
        // 底部阴影
        gui.fill(gx + 1, gy + h - 4, gx + w - 1, gy + h - 1, darken(baseColor));
        // 文字
        gui.drawString(this.font, label, gx + (w - this.font.width(label)) / 2, gy + (h - 8) / 2, 0xFFFFFF);
    }

    private static int lighten(int color)
    {
        int r = ((color >> 16) & 0xFF), g = ((color >> 8) & 0xFF), b = (color & 0xFF);
        return 0xFF000000 | (Math.min(255, r + 30) << 16) | (Math.min(255, g + 30) << 8) | Math.min(255, b + 30);
    }

    private static int darken(int color)
    {
        int r = ((color >> 16) & 0xFF), g = ((color >> 8) & 0xFF), b = (color & 0xFF);
        return 0xFF000000 | (Math.max(0, r - 40) << 16) | (Math.max(0, g - 40) << 8) | Math.max(0, b - 40);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        gui.drawString(this.font, this.title, 8, 6, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        // 时间按钮
        for (int i = 0; i < 3; i++)
        {
            int bx = BTN_X[i], by = TIME_SECTION_Y + BTN_Y;
            if (relX >= bx && relX < bx + BTN_W && relY >= by && relY < by + BTN_H)
            {
                sendButton(1 + i);
                return true;
            }
        }
        // 天气按钮
        for (int i = 0; i < 3; i++)
        {
            int bx = BTN_X[i], by = WEATHER_SECTION_Y + BTN_Y;
            if (relX >= bx && relX < bx + BTN_W && relY >= by && relY < by + BTN_H)
            {
                sendButton(4 + i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendButton(int buttonId)
    {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null)
        {
            conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, buttonId));
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
