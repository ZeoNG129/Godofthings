package com.godofthings.client.screen;

import com.godofthings.menu.GodTransmitterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之传输配置屏幕：复用投掷器（dispenser）贴图 + 绑定器槽 + 神之加速槽 + 三个开关按钮。
 */
public class GodTransmitterScreen extends AbstractContainerScreen<GodTransmitterMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

    private static final int BTN_W = 48;
    private static final int BTN_H = 16;
    private static final int BTN_X = 8;
    private static final int CROSS_Y = 20;
    private static final int PLAYER_Y = 40;
    private static final int MACHINE_Y = 60;

    public GodTransmitterScreen(GodTransmitterMenu menu, Inventory playerInv, Component title)
    {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 绑定器槽 + 加速槽框
        gui.blitSprite(SLOT_SPRITE, x + 80, y + 20, 18, 18);
        gui.blitSprite(SLOT_SPRITE, x + 80, y + 50, 18, 18);

        // 三个开关按钮
        drawToggle(gui, x + BTN_X, y + CROSS_Y, this.menu.isCrossDimension(),
                Component.translatable("gui.godofthings.transmitter.cross_dimension"));
        drawToggle(gui, x + BTN_X, y + PLAYER_Y, this.menu.isPlayerEnabled(),
                Component.translatable("gui.godofthings.transmitter.player"));
        drawToggle(gui, x + BTN_X, y + MACHINE_Y, this.menu.isMachineEnabled(),
                Component.translatable("gui.godofthings.transmitter.machine"));
    }

    private void drawToggle(GuiGraphics gui, int bx, int by, boolean on, Component label)
    {
        gui.fill(bx, by, bx + BTN_W, by + BTN_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + BTN_W - 1, by + BTN_H - 1, on ? 0xFF57B757 : 0xFF3A4048);
        gui.drawString(this.font, label, bx + (BTN_W - this.font.width(label)) / 2, by + 4, 0xFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        gui.drawString(this.font, this.title, 8, 6, 0x404040, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"), 8, this.inventoryLabelY, 0x404040, false);
        // 作用范围显示
        int range = this.menu.getBlockEntity().getRange();
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.range", range), 8, 80, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        if (relX >= BTN_X && relX < BTN_X + BTN_W)
        {
            if (relY >= CROSS_Y && relY < CROSS_Y + BTN_H)
            {
                this.menu.toggleCrossDimensionLocal();
                sendButton(0);
                return true;
            }
            if (relY >= PLAYER_Y && relY < PLAYER_Y + BTN_H)
            {
                this.menu.togglePlayerEnabledLocal();
                sendButton(1);
                return true;
            }
            if (relY >= MACHINE_Y && relY < MACHINE_Y + BTN_H)
            {
                this.menu.toggleMachineEnabledLocal();
                sendButton(2);
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
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }
}
