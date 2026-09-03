package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodResourceMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之资源界面：全部中文。
 * - 9 个输入槽（3×3，放树苗/作物/矿石块）
 * - 每 20 tick 自动生产一轮，向下自动输出
 */
public class GodResourceScreen extends AbstractContainerScreen<GodResourceMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_resource.png");

    // AE 接入开关按钮（右上角空位）
    private static final int AE_X = 150;
    private static final int AE_Y = 6;
    private static final int AE_SIZE = 20;

    public GodResourceScreen(GodResourceMenu menu, Inventory playerInventory, Component title)
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

        // 神之加速槽框（复用输入槽框贴图 UV 79,35，画在 x=43,y=35）
        gui.blit(TEXTURE, x + 43, y + 35, 79, 35, 18, 18);

        // AE 接入开关按钮
        int ax = x + AE_X;
        int ay = y + AE_Y;
        boolean aeOn = this.menu.isAeEnabled();
        gui.fill(ax, ay, ax + AE_SIZE, ay + AE_SIZE, 0xFF16181D);
        gui.fill(ax + 1, ay + 1, ax + AE_SIZE - 1, ay + AE_SIZE - 1, aeOn ? 0xFF57B757 : 0xFF3A4048);
        Component aeLabel = Component.literal("AE");
        gui.drawString(this.font, aeLabel, ax + (AE_SIZE - this.font.width(aeLabel)) / 2, ay + 6, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        if (relX >= AE_X && relX < AE_X + AE_SIZE && relY >= AE_Y && relY < AE_Y + AE_SIZE)
        {
            // AE 接入开关（服务端在 clickMenuButton(10) 中切换）
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null)
            {
                conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, 10));
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        // 高对比度文字（面板被遮罩压暗后仍清晰）
        gui.drawString(this.font, this.title, 8, 6, 0x404040, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"), 8, 72, 0x404040, false); // 物品格子顶行 84 - 12（原版标准）
        // 神之加速标签（加速槽 x=43 上方）
        gui.drawString(this.font, Component.translatable("gui.godofthings.accelerator"), 43, 24, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 1.21.1：AbstractContainerScreen.render 内部已调用 renderBackground(gui, mouseX, mouseY, partialTick)（含 renderBg），
        // 子类不再手动调 renderBackground。
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
        // 神之加速槽 hover：显示当前并行倍率（槽内有物品时让位给物品 tooltip，避免覆盖）
        if (isHovering(43, 35, 18, 18, mouseX, mouseY) && (this.hoveredSlot == null || !this.hoveredSlot.hasItem()))
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
