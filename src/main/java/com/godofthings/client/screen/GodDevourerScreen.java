package com.godofthings.client.screen;

import com.godofthings.menu.GodDevourerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之吞噬屏幕：复用原版大箱子（generic_54）9×6 布局。
 */
public class GodDevourerScreen extends AbstractContainerScreen<GodDevourerMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    // AE 接入开关按钮，位于面板右上角空位（标题行右侧，避开 9×6 槽位区）
    private static final int AE_X = 152;
    private static final int AE_Y = 3;
    private static final int AE_SIZE = 14;

    public GodDevourerScreen(GodDevourerMenu menu, Inventory playerInv, Component title)
    {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // AE 接入开关按钮（绿色=开 / 灰色=关）
        int ax = x + AE_X;
        int ay = y + AE_Y;
        boolean aeOn = this.menu.isAeEnabled();
        gui.fill(ax, ay, ax + AE_SIZE, ay + AE_SIZE, 0xFF16181D);
        gui.fill(ax + 1, ay + 1, ax + AE_SIZE - 1, ay + AE_SIZE - 1, aeOn ? 0xFF57B757 : 0xFF3A4048);
        Component aeLabel = Component.literal("AE");
        gui.drawString(this.font, aeLabel, ax + (AE_SIZE - this.font.width(aeLabel)) / 2, ay + 3, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        if (relX >= AE_X && relX < AE_X + AE_SIZE && relY >= AE_Y && relY < AE_Y + AE_SIZE)
        {
            // AE 接入开关（服务端在 clickMenuButton(7) 中切换）
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null)
            {
                conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, 7));
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }
}
