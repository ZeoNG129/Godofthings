package com.godofthings.belt.common;

import com.mojang.blaze3d.systems.RenderSystem;
import com.godofthings.Godofthings;
import com.godofthings.belt.BeltRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BeltScreen extends AbstractContainerScreen<BeltContainer>
{
    /**
     * The ResourceLocation containing the chest GUI texture.
     */
    private static final ResourceLocation GUI_TEXTURE = BeltRegistration.location("textures/gui/belt.png");

    public BeltScreen(BeltContainer container, Inventory playerInventory, Component title)
    {
        super(container, playerInventory, title);
        this.imageWidth = 176;
        int beltRows = Math.max(1, (this.getMenu().beltSlots + 7) / 8);
        this.imageHeight = 133 + (beltRows - 1) * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
    {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        int extra = this.imageHeight - 133;
        if (extra <= 0)
        {
            graphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
        }
        else
        {
            // 16 格 = 2 排：上半（标题 + 腰带格）原样绘制，中间补一条面板底色，
            // 下半（玩家背包区，贴图 v=44 起）整体下移 extra 像素，避免底图被拉伸变形。
            graphics.blit(GUI_TEXTURE, i, j, 0, 0, this.imageWidth, 44);
            graphics.fill(i, j + 44, i + this.imageWidth, j + 44 + extra, 0xFFC6C6C6);
            graphics.blit(GUI_TEXTURE, i, j + 44 + extra, 0, 44, this.imageWidth, 133 - 44);
        }

        int slots = this.getMenu().beltSlots;
        int cols = Math.min(8, Math.max(1, slots));
        int rows = (slots + cols - 1) / cols;
        int width = cols * 18;
        int x = 7 + ((8 - cols) * 18) / 2;
        for (int r = 0; r < rows; r++)
        {
            int rowSlots = Math.min(cols, slots - r * cols);
            // 槽条贴图固定在底图 v=133 处，逐排复制；16 格 = 2 排 × 8
            graphics.blit(GUI_TEXTURE, i + x, j + 19 + r * 18, 0, 133, rowSlots * 18, 18);
        }
    }
}