package com.godofthings.client.screen;

import com.godofthings.menu.GodBlackBoxMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之黑盒配置屏幕：复用原版投掷器（dispenser）贴图的 3×3 槽位区 + 左侧开关按钮。
 */
public class GodBlackBoxScreen extends AbstractContainerScreen<GodBlackBoxMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

    private static final int SWITCH_X = 8;
    private static final int SWITCH_Y = 17;
    private static final int SWITCH_W = 48;
    private static final int SWITCH_H = 20;

    public GodBlackBoxScreen(GodBlackBoxMenu menu, Inventory playerInv, Component title)
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

        // 开关按钮
        boolean enabled = this.menu.isEnabled();
        int bx = x + SWITCH_X;
        int by = y + SWITCH_Y;
        gui.fill(bx, by, bx + SWITCH_W, by + SWITCH_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + SWITCH_W - 1, by + SWITCH_H - 1, enabled ? 0xFF57B757 : 0xFF3A4048);
        Component label = Component.translatable(enabled
                ? "gui.godofthings.black_box.enabled"
                : "gui.godofthings.black_box.disabled");
        gui.drawString(this.font, label, bx + (SWITCH_W - this.font.width(label)) / 2, by + 6, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        if (relX >= SWITCH_X && relX < SWITCH_X + SWITCH_W && relY >= SWITCH_Y && relY < SWITCH_Y + SWITCH_H)
        {
            // 乐观更新本地开关，再发包让服务端切换
            this.menu.toggleEnabledLocal();
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null)
            {
                conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, 0));
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
