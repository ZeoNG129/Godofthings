package com.godofthings.client.screen;

import com.godofthings.block.entity.GodMinerBlockEntity;
import com.godofthings.godgrid.AbstractGodGridScreen;
import com.godofthings.godgrid.GodGridLayout;
import com.godofthings.menu.GodMinerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之矿机界面：网格面板（17 输入占位 + 17 输出 + 加速槽、进度箭头、三个页签）由
 * {@link AbstractGodGridScreen} 统一绘制，本类只在面板下方补矿机自己的控件：
 * 开始/停止、半径调整（-100/-10/-1/+1/+10/+100）、时运 3 / 精准采集切换、半径与深度读数。
 *
 * <p>AE 开关与面配置已并入基类右上/左侧页签（不再单独画按钮）。</p>
 */
public class GodMinerScreen extends AbstractGodGridScreen<GodMinerMenu>
{
    /** 矿机控件区：紧贴共用面板（{@link GodGridLayout#IMAGE_W}×{@link GodGridLayout#IMAGE_H}）下方 */
    private static final int EXTRA_Y = GodGridLayout.IMAGE_H + 2;
    private static final int EXTRA_H = 46;

    private static final int RUN_X = 8;
    private static final int RUN_W = 68;
    private static final int RUN_H = 18;

    private static final int FORTUNE_X = 80;
    private static final int SILK_X = 108;
    private static final int ENCHANT_W = 24;
    private static final int ENCHANT_H = 18;

    private static final int RADIUS_TEXT_X = 140;
    private static final int DEPTH_TEXT_X = 250;

    private static final String[] RADIUS_LABELS = { "-100", "-10", "-1", "+1", "+10", "+100" };
    private static final int[] RADIUS_DELTAS = { -100, -10, -1, 1, 10, 100 };
    private static final int[] RADIUS_BTN_X = { 8, 36, 64, 92, 120, 148 };
    private static final int RADIUS_BTN_Y = EXTRA_Y + 22;
    private static final int RADIUS_BTN_W = 26;
    private static final int RADIUS_BTN_H = 18;

    private static final int COLOR_BTN_BORDER = 0xFF16181D;
    private static final int COLOR_BTN_IDLE = 0xFF4A4A4A;

    public GodMinerScreen(GodMinerMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        // 共用面板下多留一条控件区
        this.imageHeight = GodGridLayout.IMAGE_H + EXTRA_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        // 面板 / 槽框 / 箭头 / 三个页签由基类画
        super.renderBg(gui, partialTick, mouseX, mouseY);

        GodMinerBlockEntity be = this.menu.getMiner();
        int x = this.leftPos;
        int y = this.topPos;

        // 开始 / 停止
        boolean running = be.isRunning();
        drawButton(gui, x + RUN_X, y + EXTRA_Y, RUN_W, RUN_H,
                running ? 0xFF8A3A2A : 0xFF2E5D3A, // 运行中=红棕，停止=绿
                running ? Component.translatable("gui.godofthings.miner.stop")
                        : Component.translatable("gui.godofthings.miner.start"));

        // 时运 3 / 精准采集（互斥，绿色=开）
        drawButton(gui, x + FORTUNE_X, y + EXTRA_Y, ENCHANT_W, ENCHANT_H,
                be.getFortuneLevel() == 3 ? 0xFF2E7D32 : COLOR_BTN_IDLE,
                Component.translatable("gui.godofthings.miner.fortune"));
        drawButton(gui, x + SILK_X, y + EXTRA_Y, ENCHANT_W, ENCHANT_H,
                be.isSilkTouch() ? 0xFF2E7D32 : COLOR_BTN_IDLE,
                Component.translatable("gui.godofthings.miner.silk_touch"));

        // 半径按钮
        for (int i = 0; i < 6; i++)
        {
            drawButton(gui, x + RADIUS_BTN_X[i], y + RADIUS_BTN_Y, RADIUS_BTN_W, RADIUS_BTN_H,
                    COLOR_BTN_IDLE, Component.literal(RADIUS_LABELS[i]));
        }

        // 读数（半径 / 当前深度）
        gui.drawString(this.font, Component.translatable("gui.godofthings.miner.radius", be.getRadius()),
                x + RADIUS_TEXT_X, y + EXTRA_Y + 5, GodGridLayout.COLOR_TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.miner.depth", be.getCurrentY()),
                x + DEPTH_TEXT_X, y + EXTRA_Y + 5, GodGridLayout.COLOR_TEXT, false);
    }

    private void drawButton(GuiGraphics gui, int bx, int by, int w, int h, int fill, Component label)
    {
        gui.fill(bx, by, bx + w, by + h, COLOR_BTN_BORDER);
        gui.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, fill);
        gui.drawString(this.font, label, bx + (w - this.font.width(label)) / 2, by + (h - 8) / 2,
                0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        GodMinerBlockEntity be = this.menu.getMiner();
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        if (hit(relX, relY, RUN_X, EXTRA_Y, RUN_W, RUN_H))
        {
            be.setRunning(!be.isRunning()); // 乐观更新，按钮立即反馈
            sendButton(GodMinerMenu.BTN_RUN);
            return true;
        }
        if (hit(relX, relY, FORTUNE_X, EXTRA_Y, ENCHANT_W, ENCHANT_H))
        {
            be.toggleFortune(); // 乐观更新
            sendButton(GodMinerMenu.BTN_FORTUNE);
            return true;
        }
        if (hit(relX, relY, SILK_X, EXTRA_Y, ENCHANT_W, ENCHANT_H))
        {
            be.toggleSilkTouch(); // 乐观更新
            sendButton(GodMinerMenu.BTN_SILK_TOUCH);
            return true;
        }
        for (int i = 0; i < 6; i++)
        {
            if (hit(relX, relY, RADIUS_BTN_X[i], RADIUS_BTN_Y, RADIUS_BTN_W, RADIUS_BTN_H))
            {
                be.setRadius(be.getRadius() + RADIUS_DELTAS[i]); // 乐观更新
                sendButton(GodMinerMenu.BTN_RADIUS_BASE + i);
                return true;
            }
        }
        // 其余交给基类（面配置 / AE / 排序三个页签与槽位点击）
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean hit(int relX, int relY, int bx, int by, int w, int h)
    {
        return relX >= bx && relX < bx + w && relY >= by && relY < by + h;
    }

    private void sendButton(int buttonId)
    {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null)
        {
            conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, buttonId));
        }
    }
}
