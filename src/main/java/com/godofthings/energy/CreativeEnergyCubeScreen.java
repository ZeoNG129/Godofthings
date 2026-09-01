package com.godofthings.energy;

import com.godofthings.Godofthings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 创造能量立方屏幕：标题 + 充能格标签 + "输出: ∞ FE/t" 提示。
 */
@OnlyIn(Dist.CLIENT)
public class CreativeEnergyCubeScreen extends AbstractContainerScreen<CreativeEnergyCubeMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "textures/gui/creative_energy_cube_gui.png");

    public CreativeEnergyCubeScreen(CreativeEnergyCubeMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        gui.drawString(this.font, this.title, 8, 6, 0x404040, false);
        // 方案C 极简布局：仅居中槽位 + 槽位下方一行输出信息
        Component output = Component.translatable("gui.godofthings.creative_energy_cube.output");
        gui.drawString(this.font, output, (this.imageWidth - this.font.width(output)) / 2, 52, 0x1B5E20, false);
        gui.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 1.21.1：AbstractContainerScreen.render 不自动绘制槽位 tooltip（物品名称），
        // 需子类显式调用 renderTooltip（与其他界面 GodMinerScreen 等保持一致）。
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }
}
