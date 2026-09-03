package com.godofthings.client.screen;

import com.godofthings.block.entity.FaceMode;
import com.godofthings.menu.GodAbsorberConfigMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之吸收面配置界面：3×2 方向磁贴 + 返回按钮。
 */
public class GodAbsorberConfigScreen extends AbstractContainerScreen<GodAbsorberConfigMenu>
{
    private static final int[] TILE_X = { 114, 10, 62, 62, 10, 114 };
    private static final int[] TILE_Y = { 30, 30, 30, 60, 60, 60 };
    private static final int TILE_W = 52;
    private static final int TILE_H = 26;
    private static final int BACK_X = 58;
    private static final int BACK_Y = 116;
    private static final int BACK_W = 60;
    private static final int BACK_H = 20;
    private static final String[] DIR_KEYS = { "gui.godofthings.dir.down", "gui.godofthings.dir.up", "gui.godofthings.dir.north", "gui.godofthings.dir.south", "gui.godofthings.dir.west", "gui.godofthings.dir.east" };
    private static final String[] MODE_KEYS = { "gui.godofthings.mode.none", "gui.godofthings.mode.input", "gui.godofthings.mode.output", "gui.godofthings.mode.both" };

    public GodAbsorberConfigScreen(GodAbsorberConfigMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 146;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        gui.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E22);
        gui.drawString(this.font, Component.translatable("gui.godofthings.face_config"), x + 8, y + 6, 0xFFFFFF);
        gui.drawString(this.font, Component.translatable("gui.godofthings.face_config.hint"), x + 8, y + 96, 0x555555);
        for (int d = 0; d < 6; d++) drawDirectionTile(gui, x + TILE_X[d], y + TILE_Y[d], d);
        int bx = x + BACK_X, by = y + BACK_Y;
        gui.fill(bx, by, bx + BACK_W, by + BACK_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + BACK_W - 1, by + BACK_H - 1, 0xFF3A4048);
        Component back = Component.translatable("gui.godofthings.back");
        gui.drawString(this.font, back, bx + (BACK_W - this.font.width(back)) / 2, by + 6, 0xFFFFFF);
    }

    private void drawDirectionTile(GuiGraphics gui, int bx, int by, int dir)
    {
        int mode = this.menu.getBlockEntity().getFaceMode(Direction.values()[dir]);
        int stripe = switch (mode) { case 1 -> 0xFF3F7F3F; case 2 -> 0xFF9A5A20; case 3 -> 0xFFB8860B; default -> 0xFF555555; };
        int textColor = switch (mode) { case 1 -> 0xFF57B757; case 2 -> 0xFFC87A3A; case 3 -> 0xFFE0B030; default -> 0xFF9AA0A8; };
        gui.fill(bx, by, bx + TILE_W, by + TILE_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + TILE_W - 1, by + TILE_H - 1, 0xFF2B2F38);
        gui.fill(bx + 1, by + 1, bx + 5, by + TILE_H - 1, stripe);
        Component direction = Component.translatable(DIR_KEYS[dir]);
        Component modeName = Component.translatable(MODE_KEYS[FaceMode.fromId(mode).getId()]);
        gui.drawString(this.font, direction, bx + (TILE_W - this.font.width(direction)) / 2, by + 4, 0xFFFFFF);
        gui.drawString(this.font, modeName, bx + (TILE_W - this.font.width(modeName)) / 2, by + 15, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        for (int d = 0; d < 6; d++)
        {
            if (relX >= TILE_X[d] && relX < TILE_X[d] + TILE_W && relY >= TILE_Y[d] && relY < TILE_Y[d] + TILE_H)
            {
                this.menu.getBlockEntity().cycleFaceMode(Direction.values()[d]);
                send(d);
                return true;
            }
        }
        if (relX >= BACK_X && relX < BACK_X + BACK_W && relY >= BACK_Y && relY < BACK_Y + BACK_H)
        {
            send(6);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void send(int id)
    {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null) conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, id));
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
