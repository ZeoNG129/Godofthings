package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.FaceMode;
import com.godofthings.menu.GodCraftConfigMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之合成面配置界面：6 个方向磁贴循环切换 + 返回。
 */
public class GodCraftConfigScreen extends AbstractContainerScreen<GodCraftConfigMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_furnace_config.png");

    private static final int[] TILE_X = { 114, 10, 62, 62, 10, 114 };
    private static final int[] TILE_Y = { 30, 30, 30, 60, 60, 60 };
    private static final int TILE_W = 52;
    private static final int TILE_H = 26;
    private static final int BACK_X = 58;
    private static final int BACK_Y = 116;
    private static final int BACK_W = 60;
    private static final int BACK_H = 20;

    private static final String[] DIR_NAMES = { "下", "上", "北", "南", "西", "东" };
    private static final String[] MODE_NAMES = { "无", "输入", "输出", "输入和输出" };

    public GodCraftConfigScreen(GodCraftConfigMenu menu, Inventory playerInventory, Component title)
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

        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        gui.drawString(this.font, Component.literal("面配置"), x + 8, y + 6, 0xFFFFFF);
        gui.drawString(this.font, Component.literal("点击切换：无/输入/输出/输入和输出"), x + 8, y + 96, 0x9AA0A8);

        for (int d = 0; d < 6; d++)
        {
            drawDirectionTile(gui, x + TILE_X[d], y + TILE_Y[d], d);
        }

        int bx = x + BACK_X;
        int by = y + BACK_Y;
        gui.fill(bx, by, bx + BACK_W, by + BACK_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + BACK_W - 1, by + BACK_H - 1, 0xFF3A4048);
        Component back = Component.literal("返回");
        gui.drawString(this.font, back, bx + (BACK_W - this.font.width(back)) / 2, by + 6, 0xFFFFFF);
    }

    private void drawDirectionTile(GuiGraphics gui, int bx, int by, int dir)
    {
        int mode = this.menu.getBlockEntity().getFaceMode(Direction.values()[dir]);
        int textColor = switch (mode)
        {
            case 1 -> 0xFF57B757;
            case 2 -> 0xFFC87A3A;
            case 3 -> 0xFFE0B030;
            default -> 0xFF9AA0A8;
        };
        gui.fill(bx, by, bx + TILE_W, by + TILE_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + TILE_W - 1, by + TILE_H - 1, 0xFF2B2F38);
        Component direction = Component.literal(DIR_NAMES[dir]);
        Component modeName = Component.literal(MODE_NAMES[FaceMode.fromId(mode).getId()]);
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
                ClientPacketListener conn = Minecraft.getInstance().getConnection();
                if (conn != null)
                {
                    conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, d));
                }
                return true;
            }
        }
        if (relX >= BACK_X && relX < BACK_X + BACK_W && relY >= BACK_Y && relY < BACK_Y + BACK_H)
        {
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null)
            {
                conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, 6));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
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
