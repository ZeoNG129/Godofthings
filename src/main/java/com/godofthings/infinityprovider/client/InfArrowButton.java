package com.godofthings.infinityprovider.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** 翻页箭头按钮（移植自上游 Up/DownArrowButtonWidget，只在悬停时画贴图与提示）。 */
public class InfArrowButton extends Button {

    public enum Direction {
        UP("textures/gui/prev_button_hover.png", "gui.godofthings.infprovider.prev_page"),
        DOWN("textures/gui/next_button_hover.png", "gui.godofthings.infprovider.next_page");

        final ResourceLocation texture;
        final String tooltipKey;

        Direction(String texture, String tooltipKey) {
            this.texture = ResourceLocation.fromNamespaceAndPath("godofthings", texture);
            this.tooltipKey = tooltipKey;
        }
    }

    private final Direction direction;

    public InfArrowButton(int x, int y, Direction direction, OnPress onPress) {
        super(x, y, 15, 22, Component.empty(), onPress, DEFAULT_NARRATION);
        this.direction = direction;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.isHovered()) {
            guiGraphics.blit(this.direction.texture, this.getX(), this.getY(), 0.0F, 0.0F, 15, 22, 15, 22);
            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                    Component.translatable(this.direction.tooltipKey), mouseX, mouseY);
        }
    }
}
