package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodFurnaceMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GodFurnaceScreen extends AbstractContainerScreen<GodFurnaceMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_furnace.png");

    // 齿轮图标按钮（打开面配置界面），位于面板右上角
    private static final int GEAR_X = 150;
    private static final int GEAR_Y = 17;
    private static final int GEAR_SIZE = 20;

    public GodFurnaceScreen(GodFurnaceMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 172;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);  // 背景（含输入/输出槽）

        // 输入/输出行标签（画在槽位右侧空白区，高对比度）
        gui.drawString(this.font, Component.translatable("gui.godofthings.furnace.input"), x + 122, y + 22, 0xFFFFFF);
        gui.drawString(this.font, Component.translatable("gui.godofthings.furnace.output"), x + 122, y + 58, 0xFFFFFF);

        // 神之加速槽框（复用输入槽框贴图 UV 8,17，画在 x=116,y=35）+ 标签
        // 加速文字位于加速槽左边、输入槽(y=17)与输出槽(y=53)中间，避免与右上「输入」文字重合
        gui.blit(TEXTURE, x + 116, y + 35, 8, 17, 18, 18);
        gui.drawString(this.font, Component.translatable("gui.godofthings.accelerator"), x + 86, y + 40, 0xFFFFFF);

        // 齿轮图标按钮（点击打开面配置界面）
        int bx = x + GEAR_X;
        int by = y + GEAR_Y;
        int relX = mouseX - x;
        int relY = mouseY - y;
        boolean hovering = relX >= GEAR_X && relX < GEAR_X + GEAR_SIZE
                && relY >= GEAR_Y && relY < GEAR_Y + GEAR_SIZE;
        gui.fill(bx, by, bx + GEAR_SIZE, by + GEAR_SIZE, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + GEAR_SIZE - 1, by + GEAR_SIZE - 1,
                hovering ? 0xFF5A5A5A : 0xFF3A3A3A);
        gui.blit(TEXTURE, bx + 1, by + 1, 176, 16, 18, 18);  // 齿轮图标
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        if (relX >= GEAR_X && relX < GEAR_X + GEAR_SIZE && relY >= GEAR_Y && relY < GEAR_Y + GEAR_SIZE)
        {
            // 打开面配置界面（服务端在 clickMenuButton(6) 中打开新菜单）
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null)
            {
                conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, 6));
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        // 高对比度文字（面板被 renderBackground 遮罩压暗后仍清晰）
        gui.drawString(this.font, this.title, 8, 6, 0x404040, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"), 8, 72, 0x404040, false); // 物品格子顶行 84 - 12（原版标准）
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 1.21.1：AbstractContainerScreen.render 内部已调用 renderBackground(gui, mouseX, mouseY, partialTick)（含 renderBg），
        // 子类不再手动调 renderBackground。
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
        // 神之加速槽 hover：显示当前并行倍率（槽内有物品时让位给物品 tooltip，避免覆盖）
        if (isHovering(116, 35, 18, 18, mouseX, mouseY) && (this.hoveredSlot == null || !this.hoveredSlot.hasItem()))
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
