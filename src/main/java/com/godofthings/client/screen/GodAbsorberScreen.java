package com.godofthings.client.screen;

import com.godofthings.block.entity.GodAbsorberBlockEntity;
import com.godofthings.client.GuiStep;
import com.godofthings.menu.GodAbsorberMenu;
import com.godofthings.network.AbsorberMessages;
import com.godofthings.util.NumberText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 神之吸收屏幕：功能 / 存储 / 经验三个板块（顶部按钮切换），面配置由顶部按钮打开独立界面。
 */
public class GodAbsorberScreen extends AbstractContainerScreen<GodAbsorberMenu>
{
    private static final int TAB_W = 36;
    private static final int TAB_H = 14;

    public GodAbsorberScreen(GodAbsorberMenu menu, Inventory playerInv, Component title)
    {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;
        gui.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E22);

        drawButton(gui, x + 8, y + 4, TAB_W, TAB_H, Component.translatable("gui.godofthings.slaughter.tab_function"), this.menu.getCurrentTab() == GodAbsorberMenu.TAB_FUNCTION);
        drawButton(gui, x + 46, y + 4, TAB_W, TAB_H, Component.translatable("gui.godofthings.slaughter.tab_storage"), this.menu.getCurrentTab() == GodAbsorberMenu.TAB_STORAGE);
        drawButton(gui, x + 84, y + 4, TAB_W, TAB_H, Component.translatable("gui.godofthings.slaughter.tab_experience"), this.menu.getCurrentTab() == GodAbsorberMenu.TAB_EXPERIENCE);
        drawButton(gui, x + 122, y + 4, 46, TAB_H, Component.translatable("gui.godofthings.face_config"), false);

        if (this.menu.getCurrentTab() == GodAbsorberMenu.TAB_FUNCTION) renderFunction(gui, x, y);
        else if (this.menu.getCurrentTab() == GodAbsorberMenu.TAB_EXPERIENCE) renderExperience(gui, x, y);
    }

    private void renderFunction(GuiGraphics gui, int x, int y)
    {
        drawButton(gui, x + 8, y + 24, 76, 16,
                this.menu.isEnabled() ? Component.translatable("gui.godofthings.slaughter.on") : Component.translatable("gui.godofthings.slaughter.off"),
                this.menu.isEnabled());
        drawButton(gui, x + 88, y + 24, 76, 16,
                this.menu.isAeEnabled() ? Component.literal("AE: 开") : Component.literal("AE: 关"),
                this.menu.isAeEnabled());
        gui.drawString(this.font, Component.translatable("gui.godofthings.slaughter.range", this.menu.getRange()), x + 8, y + 50, 0xFFFFFF);
        drawButton(gui, x + 96, y + 46, 18, 16, Component.literal("-"), false);
        drawButton(gui, x + 116, y + 46, 18, 16, Component.literal("+"), false);
    }

    private void renderExperience(GuiGraphics gui, int x, int y)
    {
        gui.drawString(this.font, Component.translatable("gui.godofthings.slaughter.xp_stored", this.menu.getExperienceLevel()), x + 8, y + 26, 0x55FF55);
        drawButton(gui, x + 8, y + 48, 40, 16, Component.translatable("gui.godofthings.slaughter.xp_take_1"), false);
        drawButton(gui, x + 52, y + 48, 40, 16, Component.translatable("gui.godofthings.slaughter.xp_take_10"), false);
        drawButton(gui, x + 96, y + 48, 40, 16, Component.translatable("gui.godofthings.slaughter.xp_take_100"), false);
        drawButton(gui, x + 8, y + 66, 40, 16, Component.translatable("gui.godofthings.slaughter.xp_take_all"), false);
    }

    private void drawButton(GuiGraphics gui, int bx, int by, int w, int h, Component label, boolean active)
    {
        gui.fill(bx, by, bx + w, by + h, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, active ? 0xFF57B757 : 0xFF3A4048);
        gui.drawString(this.font, label, bx + (w - this.font.width(label)) / 2, by + 3, 0xFFFFFF);
    }

    @Override
    protected void renderSlot(GuiGraphics gui, Slot slot)
    {
        if (slot.index < GodAbsorberBlockEntity.STORAGE_SLOTS)
        {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && stack.getCount() > 999)
            {
                int x = slot.x, y = slot.y;
                gui.pose().pushPose();
                gui.pose().translate(0.0F, 0.0F, 100.0F);
                gui.renderItem(stack, x, y, x + y * this.imageWidth);
                gui.renderItemDecorations(this.font, stack, x, y, NumberText.format(stack.getCount()));
                gui.pose().popPose();
                return;
            }
        }
        super.renderSlot(gui, slot);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        int tab = this.menu.getCurrentTab();

        if (inRect(relX, relY, 8, 4, TAB_W, TAB_H)) { this.menu.setCurrentTab(GodAbsorberMenu.TAB_FUNCTION); return true; }
        if (inRect(relX, relY, 46, 4, TAB_W, TAB_H)) { this.menu.setCurrentTab(GodAbsorberMenu.TAB_STORAGE); return true; }
        if (inRect(relX, relY, 84, 4, TAB_W, TAB_H)) { this.menu.setCurrentTab(GodAbsorberMenu.TAB_EXPERIENCE); return true; }
        if (inRect(relX, relY, 122, 4, 46, TAB_H)) { sendButton(1); return true; }

        if (tab == GodAbsorberMenu.TAB_FUNCTION)
        {
            if (inRect(relX, relY, 8, 24, 76, 16)) { this.menu.toggleEnabledLocal(); sendButton(0); return true; }
            if (inRect(relX, relY, 88, 24, 76, 16)) { this.menu.toggleAeEnabledLocal(); sendButton(6); return true; }
            if (inRect(relX, relY, 96, 46, 18, 16)) { adjustRange(-GuiStep.amount()); return true; }
            if (inRect(relX, relY, 116, 46, 18, 16)) { adjustRange(GuiStep.amount()); return true; }
        }
        else if (tab == GodAbsorberMenu.TAB_EXPERIENCE)
        {
            if (inRect(relX, relY, 8, 48, 40, 16)) { sendButton(2); return true; }
            if (inRect(relX, relY, 52, 48, 40, 16)) { sendButton(3); return true; }
            if (inRect(relX, relY, 96, 48, 40, 16)) { sendButton(4); return true; }
            if (inRect(relX, relY, 8, 66, 40, 16)) { sendButton(5); return true; }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void adjustRange(int delta)
    {
        int v = Math.max(0, Math.min(GodAbsorberBlockEntity.MAX_RANGE, this.menu.getRange() + delta));
        this.menu.setRangeLocal(v);
        AbsorberMessages.sendRange(delta);
    }

    private static boolean inRect(int x, int y, int bx, int by, int w, int h) { return x >= bx && x < bx + w && y >= by && y < by + h; }

    private void sendButton(int id)
    {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null) conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, id));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
