package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodPeelerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之去皮界面（v5.15.1：与神之熔炉相同布局）——
 * 9 输入槽（上一行）→ 9 输出槽（下一行），中间向下箭头表示转化方向；右侧神之加速槽。
 */
public class GodPeelerScreen extends AbstractContainerScreen<GodPeelerMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_peeler.png");

    // 神之加速槽（菜单槽位与贴图槽框都在此坐标，同神之熔炉）
    private static final int ACCEL_X = 178;
    private static final int ACCEL_Y = 35;

    public GodPeelerScreen(GodPeelerMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 212;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // 背景（含 9 输入 / 9 输出槽框、中间转化箭头与右侧加速槽框）
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        // 标题/物品栏标签与菜单槽位的原始相对位置（物品栏第一个槽 x=8，同神之熔炉）
        gui.drawString(this.font, this.title, 8, 6, 0x404040, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"), 8, 72, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 1.21.1：AbstractContainerScreen.render 内部已调用 renderBackground(gui, mouseX, mouseY, partialTick)（含 renderBg），
        // 子类不再手动调 renderBackground。
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
        // 神之加速槽 hover：显示当前并行倍率（槽内有物品时让位给物品 tooltip，避免覆盖）
        if (isHovering(ACCEL_X, ACCEL_Y, 18, 18, mouseX, mouseY) && (this.hoveredSlot == null || !this.hoveredSlot.hasItem()))
        {
            int mult = this.menu.getBlockEntity().getParallelMultiplier();
            gui.renderTooltip(this.font, Component.translatable("tooltip.godofthings.parallel", mult), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
