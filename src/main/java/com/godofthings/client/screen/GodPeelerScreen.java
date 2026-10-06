package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodPeelerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之去皮界面：1 输入槽 + 1 输出槽（中间箭头表示原木 → 去皮原木）。
 */
public class GodPeelerScreen extends AbstractContainerScreen<GodPeelerMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_peeler.png");

    public GodPeelerScreen(GodPeelerMenu menu, Inventory playerInventory, Component title)
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
        // 高对比度文字（面板被遮罩压暗后仍清晰）
        gui.drawString(this.font, this.title, 8, 6, 0x404040, false);
        // 输入 / 输出标签（复用神之熔炉的语言键，槽位上方）
        gui.drawString(this.font, Component.translatable("gui.godofthings.furnace.input"),
                GodPeelerMenu.INPUT_X + 3, 21, 0x404040, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.furnace.output"),
                GodPeelerMenu.OUTPUT_X + 3, 21, 0x404040, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"), 8, 72, 0x404040, false);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
