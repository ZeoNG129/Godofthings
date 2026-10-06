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

    // v5.13.0：9+9 布局，画布 176 → 212 宽。齿轮按钮与加速槽竖排在右侧空白区。
    // 齿轮图标按钮（打开面配置界面），右上角
    private static final int GEAR_X = 179;
    private static final int GEAR_Y = 9;
    private static final int GEAR_SIZE = 20;

    // 神之加速槽（菜单槽位与贴图槽框都在此坐标）
    private static final int ACCEL_X = 178;
    private static final int ACCEL_Y = 35;

    // v5.1.3：原「AE 接入开关」按钮已随 AE 功能一并删除。

    public GodFurnaceScreen(GodFurnaceMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 212;
        this.imageHeight = 172;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 背景（含 9 输入 / 9 输出槽框与右侧加速槽框；贴图 212 宽区域在 256 画布内）
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight - 6);

        // 齿轮图标按钮（点击打开面配置界面）；图标 UV 在贴图 (8,170) 备用区
        int bx = x + GEAR_X;
        int by = y + GEAR_Y;
        int relX = mouseX - x;
        int relY = mouseY - y;
        boolean hovering = relX >= GEAR_X && relX < GEAR_X + GEAR_SIZE
                && relY >= GEAR_Y && relY < GEAR_Y + GEAR_SIZE;
        gui.fill(bx, by, bx + GEAR_SIZE, by + GEAR_SIZE, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + GEAR_SIZE - 1, by + GEAR_SIZE - 1,
                hovering ? 0xFF5A5A5A : 0xFF3A3A3A);
        gui.blit(TEXTURE, bx + 1, by + 1, 8, 170, 18, 18);  // 齿轮图标（新 UV）
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
        // 标题/物品栏标签恢复与菜单槽位的原始相对位置（物品栏第一个槽 x=8）
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
