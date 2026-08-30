package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.FaceMode;
import com.godofthings.menu.GodFurnaceConfigMenu;
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
 * 面配置界面：3x2 网格的方向磁贴 + 返回按钮，深色主题，中文。
 * 每个面可循环：无 → 输入 → 输出 → 输入和输出
 * 方向索引 = Direction.get3DDataValue()：DOWN(0) UP(1) NORTH(2) SOUTH(3) WEST(4) EAST(5)
 */
public class GodFurnaceConfigScreen extends AbstractContainerScreen<GodFurnaceConfigMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_furnace_config.png");

    // 3x2 网格：上 北 下 / 西 南 东
    private static final int[] TILE_X = { 114, 10, 62, 62, 10, 114 };
    private static final int[] TILE_Y = { 30, 30, 30, 60, 60, 60 };
    private static final int TILE_W = 52;
    private static final int TILE_H = 26;

    private static final int BACK_X = 58;
    private static final int BACK_Y = 116;
    private static final int BACK_W = 60;
    private static final int BACK_H = 20;

    // 方向/模式名称通过语言文件本地化（默认中文）
    private static final String[] DIR_KEYS = {
            "gui.godofthings.dir.down", "gui.godofthings.dir.up", "gui.godofthings.dir.north",
            "gui.godofthings.dir.south", "gui.godofthings.dir.west", "gui.godofthings.dir.east" };
    private static final String[] MODE_KEYS = {
            "gui.godofthings.mode.none", "gui.godofthings.mode.input",
            "gui.godofthings.mode.output", "gui.godofthings.mode.both" };

    public GodFurnaceConfigScreen(GodFurnaceConfigMenu menu, Inventory playerInventory, Component title)
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

        // 标题与提示（本地化）
        gui.drawString(this.font, Component.translatable("gui.godofthings.face_config"), x + 8, y + 6, 0x404040);
        gui.drawString(this.font, Component.translatable("gui.godofthings.face_config.hint"), x + 8, y + 96, 0x555555);

        // 方向磁贴
        for (int d = 0; d < 6; d++)
        {
            drawDirectionTile(gui, x + TILE_X[d], y + TILE_Y[d], d);
        }

        // 返回按钮
        int bx = x + BACK_X;
        int by = y + BACK_Y;
        gui.fill(bx, by, bx + BACK_W, by + BACK_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + BACK_W - 1, by + BACK_H - 1, 0xFF3A4048);
        Component back = Component.translatable("gui.godofthings.back");
        gui.drawString(this.font, back, bx + (BACK_W - this.font.width(back)) / 2, by + 6, 0xFFFFFF);
    }

    private void drawDirectionTile(GuiGraphics gui, int bx, int by, int dir)
    {
        int mode = this.menu.getBlockEntity().getFaceMode(Direction.values()[dir]);
        int stripeColor = switch (mode)
        {
            case 1 -> 0xFF3F7F3F; // 输入：绿
            case 2 -> 0xFF9A5A20; // 输出：棕
            case 3 -> 0xFFB8860B; // 输入和输出：金
            default -> 0xFF555555;
        };
        int textColor = switch (mode)
        {
            case 1 -> 0xFF57B757;
            case 2 -> 0xFFC87A3A;
            case 3 -> 0xFFE0B030;
            default -> 0xFF9AA0A8;
        };

        gui.fill(bx, by, bx + TILE_W, by + TILE_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + TILE_W - 1, by + TILE_H - 1, 0xFF2B2F38);
        gui.fill(bx + 1, by + 1, bx + 5, by + TILE_H - 1, stripeColor); // 左侧模式色条

        FaceMode faceMode = FaceMode.fromId(mode);
        // 第一行：方向名；第二行：模式名（本地化）
        Component direction = Component.translatable(DIR_KEYS[dir]);
        Component modeName = Component.translatable(MODE_KEYS[faceMode.getId()]);
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
            int bx = TILE_X[d];
            int by = TILE_Y[d];
            if (relX >= bx && relX < bx + TILE_W && relY >= by && relY < by + TILE_H)
            {
                this.menu.getBlockEntity().cycleFaceMode(Direction.values()[d]);
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
        // 标题与提示已在 renderBg 中绘制，这里留空避免画出「物品栏」标签
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
