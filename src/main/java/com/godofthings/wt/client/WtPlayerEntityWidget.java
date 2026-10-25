package com.godofthings.wt.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * 玩家实体预览部件（照抄 ae2wtlib 的 {@code PlayerEntityWidget} 全文）。
 */
public class WtPlayerEntityWidget extends AbstractWidget
{
    private final LivingEntity entity;

    public WtPlayerEntityWidget(LivingEntity entity)
    {
        super(0, 0, 0, 0, Component.empty());
        this.entity = entity;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float f)
    {
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, getX(), getY(), getX() + 75 - 26 - 3,
                getY() + 78 - 8, 30, 0.0625F,
                mouseX,
                mouseY, entity);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput)
    {
    }
}
