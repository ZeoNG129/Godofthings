package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodMinerBlockEntity;
import com.godofthings.menu.GodMinerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之矿机界面：全部中文。
 * - 开始/停止按钮
 * - 半径调整按钮（-100/-10/-1/+1/+10/+100，范围 1-1600）
 * - 无储存槽：产物直接进内置无限储存，六面默认全部自动输出
 * - 状态显示（当前深度、挖掘速度、储液、内置储存）
 */
public class GodMinerScreen extends AbstractContainerScreen<GodMinerMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_miner.png");

    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

    private static final String[] RADIUS_LABELS = { "-100", "-10", "-1", "+1", "+10", "+100" };
    private static final int[] RADIUS_DELTAS = { -100, -10, -1, 1, 10, 100 };

    private static final int START_X = 52;
    private static final int START_Y = 48;
    private static final int START_W = 72;
    private static final int START_H = 20;

    private static final int[] RADIUS_BTN_X = { 8, 36, 64, 92, 120, 148 };
    private static final int RADIUS_BTN_Y = 74;
    private static final int RADIUS_BTN_W = 26;
    private static final int RADIUS_BTN_H = 16;

    // 时运3 / 精准采集切换按钮（位于标题下方空白）
    private static final int FORTUNE_X = 8;
    private static final int SILK_X = 36;
    private static final int ENCHANT_BTN_Y = 20;
    private static final int ENCHANT_BTN_W = 24;
    private static final int ENCHANT_BTN_H = 16;

    public GodMinerScreen(GodMinerMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 234;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 神之加速槽框（标准物品槽背景）+ 标签
        gui.blitSprite(SLOT_SPRITE, x + 148, y + 30, 18, 18);
        gui.drawString(this.font, Component.translatable("gui.godofthings.accelerator"), x + 140, y + 18, 0xFFFFFF);

        GodMinerBlockEntity be = this.menu.getBlockEntity();

        // 时运3 / 精准采集切换按钮（标题下方，写「运」「精」）
        boolean fortune = be.getFortuneLevel() == 3;
        boolean silk = be.isSilkTouch();
        int fx = x + FORTUNE_X;
        int fy = y + ENCHANT_BTN_Y;
        gui.fill(fx, fy, fx + ENCHANT_BTN_W, fy + ENCHANT_BTN_H, 0xFF16181D);
        gui.fill(fx + 1, fy + 1, fx + ENCHANT_BTN_W - 1, fy + ENCHANT_BTN_H - 1, fortune ? 0xFF2E7D32 : 0xFF4A4A4A);
        Component fortuneLabel = Component.translatable("gui.godofthings.miner.fortune");
        gui.drawString(this.font, fortuneLabel, fx + (ENCHANT_BTN_W - this.font.width(fortuneLabel)) / 2, fy + 4, 0xFFFFFF);

        int sx = x + SILK_X;
        int sy = y + ENCHANT_BTN_Y;
        gui.fill(sx, sy, sx + ENCHANT_BTN_W, sy + ENCHANT_BTN_H, 0xFF16181D);
        gui.fill(sx + 1, sy + 1, sx + ENCHANT_BTN_W - 1, sy + ENCHANT_BTN_H - 1, silk ? 0xFF2E7D32 : 0xFF4A4A4A);
        Component silkLabel = Component.translatable("gui.godofthings.miner.silk_touch");
        gui.drawString(this.font, silkLabel, sx + (ENCHANT_BTN_W - this.font.width(silkLabel)) / 2, sy + 4, 0xFFFFFF);

        // 开始/停止按钮
        int bx = x + START_X;
        int by = y + START_Y;
        boolean running = be.isRunning();
        int startColor = running ? 0xFF8A3A2A : 0xFF2E5D3A; // 运行中=红棕，停止=绿
        gui.fill(bx, by, bx + START_W, by + START_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + START_W - 1, by + START_H - 1, startColor);
        Component startLabel = running
                ? Component.translatable("gui.godofthings.miner.stop")
                : Component.translatable("gui.godofthings.miner.start");
        gui.drawString(this.font, startLabel, bx + (START_W - this.font.width(startLabel)) / 2, by + 6, 0xFFFFFF);

        // 半径按钮
        for (int i = 0; i < 6; i++)
        {
            int rx = x + RADIUS_BTN_X[i];
            int ry = y + RADIUS_BTN_Y;
            gui.fill(rx, ry, rx + RADIUS_BTN_W, ry + RADIUS_BTN_H, 0xFF16181D);
            gui.fill(rx + 1, ry + 1, rx + RADIUS_BTN_W - 1, ry + RADIUS_BTN_H - 1, 0xFF4A4A4A);
            Component label = Component.literal(RADIUS_LABELS[i]);
            gui.drawString(this.font, label, rx + (RADIUS_BTN_W - this.font.width(label)) / 2, ry + 4, 0xFFFFFF);
        }

        // 半径数值
        gui.drawString(this.font, Component.translatable("gui.godofthings.miner.radius", be.getRadius()), x + 8, y + 96, 0xFFFFFF);

        // 深度
        gui.drawString(this.font, Component.translatable("gui.godofthings.miner.depth", be.getCurrentY()), x + 8, y + 108, 0xFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        // 高对比度文字
        gui.drawString(this.font, this.title, 8, 6, 0x404040, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"), 8, 144, 0x404040, false); // 物品格子顶行 156 - 12（原版标准）
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        GodMinerBlockEntity be = this.menu.getBlockEntity();

        if (relX >= START_X && relX < START_X + START_W && relY >= START_Y && relY < START_Y + START_H)
        {
            be.setRunning(!be.isRunning()); // 乐观更新，按钮立即反馈
            sendButton(0);
            return true;
        }

        if (relX >= FORTUNE_X && relX < FORTUNE_X + ENCHANT_BTN_W && relY >= ENCHANT_BTN_Y && relY < ENCHANT_BTN_Y + ENCHANT_BTN_H)
        {
            be.toggleFortune(); // 乐观更新
            sendButton(7);
            return true;
        }
        if (relX >= SILK_X && relX < SILK_X + ENCHANT_BTN_W && relY >= ENCHANT_BTN_Y && relY < ENCHANT_BTN_Y + ENCHANT_BTN_H)
        {
            be.toggleSilkTouch(); // 乐观更新
            sendButton(8);
            return true;
        }

        for (int i = 0; i < 6; i++)
        {
            if (relX >= RADIUS_BTN_X[i] && relX < RADIUS_BTN_X[i] + RADIUS_BTN_W
                    && relY >= RADIUS_BTN_Y && relY < RADIUS_BTN_Y + RADIUS_BTN_H)
            {
                be.setRadius(be.getRadius() + RADIUS_DELTAS[i]); // 乐观更新
                sendButton(i + 1);
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
        // 1.21.1：AbstractContainerScreen.render 内部已调用 renderBackground(gui, mouseX, mouseY, partialTick)（含 renderBg），
        // 子类不再手动调 renderBackground。
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
        // 神之加速槽 hover：显示挖列加速说明（槽内有物品时让位给物品 tooltip，避免覆盖）
        if (isHovering(148, 30, 18, 18, mouseX, mouseY) && (this.hoveredSlot == null || !this.hoveredSlot.hasItem()))
        {
            gui.renderTooltip(this.font, Component.translatable("tooltip.godofthings.miner_accelerator"), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
