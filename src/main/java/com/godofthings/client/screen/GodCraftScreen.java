package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodCraftBlockEntity;
import com.godofthings.menu.GodCraftMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 神之合成界面：原版工作台布局（3×3 合成格 + 结果槽 + 物品栏），
 * 界面整体加宽，左侧内嵌 8 个配方模板槽（显示合成产物预览）。
 * 模板槽：单击 = 加载模板到合成格，Shift+单击 = 保存当前配方到模板。
 */
public class GodCraftScreen extends AbstractContainerScreen<GodCraftMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_craft.png");

    // 界面加宽：176 + 左侧模板面板 46
    private static final int IMG_W = 176 + 46;
    private static final int IMG_H = 166;

    // 主内容右移量（与 GodCraftMenu.SHIFT 一致）
    private static final int SHIFT = 46;

    // 左侧模板面板（8 槽，2 列 × 4 行）
    private static final int TPL_X0 = 6;
    private static final int TPL_Y0 = 30;
    private static final int TPL_SPACING_X = 21;
    private static final int TPL_SPACING_Y = 21;
    private static final int TPL_SIZE = 18;

    // 按钮位置（结果槽右侧空隙区竖排，右移 SHIFT）
    private static final int BTN_X = 146 + SHIFT;
    private static final int[] BTN_Y = { 8, 24, 40, 56 };
    private static final int BTN_W = 22;
    private static final int BTN_H = 14;

    // AE 接入开关按钮（右上角空位：标题下方、结果槽左侧）
    private static final int AE_X = 150;
    private static final int AE_Y = 18;
    private static final int AE_SIZE = 20;

    public GodCraftScreen(GodCraftMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = IMG_W;
        this.imageHeight = IMG_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 左侧模板面板底色（与右侧工作台背景区分）
        gui.fill(x, y, x + SHIFT, y + this.imageHeight, 0xFF101318);
        gui.fill(x + SHIFT - 1, y, x + SHIFT, y + this.imageHeight, 0xFF000000);

        // 原版工作台背景（右移 SHIFT）
        gui.blit(TEXTURE, x + SHIFT, y, 0, 0, 176, this.imageHeight);

        gui.drawString(this.font, Component.literal("模板"), x + 8, y + 12, 0x9AA0A8);

        GodCraftBlockEntity be = this.menu.getBlockEntity();

        // 左侧模板槽：图标 = 模板合成产物预览；单击=加载、Shift+单击=保存
        for (int t = 0; t < GodCraftBlockEntity.TEMPLATE_COUNT; t++)
        {
            int tx = x + TPL_X0 + (t % 2) * TPL_SPACING_X;
            int ty = y + TPL_Y0 + (t / 2) * TPL_SPACING_Y;
            boolean has = be.hasTemplate(t);
            gui.fill(tx, ty, tx + TPL_SIZE, ty + TPL_SIZE, 0xFF16181D);
            gui.fill(tx + 1, ty + 1, tx + TPL_SIZE - 1, ty + TPL_SIZE - 1, has ? 0xFF2B5F2B : 0xFF2B2F38);
            ItemStack result = has ? be.getTemplateResult(t) : ItemStack.EMPTY;
            if (!result.isEmpty())
            {
                gui.renderItem(result, tx + 1, ty + 1);
            }
            gui.drawString(this.font, Component.literal(String.valueOf(t + 1)), tx + TPL_SIZE - 8, ty + TPL_SIZE - 8, 0xFFFFFF);
        }

        // 锁定配方后：给锁定模板的合成格画淡绿色背景（右移 SHIFT）
        if (be.isLocked())
        {
            for (int i = 0; i < 9; i++)
            {
                ItemStack lockedItem = be.getLockedItem(i);
                if (lockedItem.isEmpty())
                {
                    continue;
                }
                int gx = x + SHIFT + 30 + (i % 3) * 18;
                int gy = y + 17 + (i / 3) * 18;
                // 淡绿色半透明（0x30 = 半透明）
                gui.fill(gx, gy, gx + 16, gy + 16, 0x3018D018);
            }
        }

        // 锁定配方按钮
        boolean locked = be.isLocked();
        drawBtn(gui, x + BTN_X, y + BTN_Y[0], BTN_W, BTN_H,
                Component.literal("锁"), locked ? 0xFF3F7F3F : 0xFF4A4A4A);

        // 启动自动合成按钮
        boolean enabled = be.isEnabled();
        drawBtn(gui, x + BTN_X, y + BTN_Y[1], BTN_W, BTN_H,
                Component.literal(enabled ? "开" : "停"), enabled ? 0xFF3F7F3F : 0xFF7F3F3F);

        // 面配置按钮
        drawBtn(gui, x + BTN_X, y + BTN_Y[2], BTN_W, BTN_H, Component.literal("配"), 0xFF3F5F8F);

        // 配方模板详情按钮
        drawBtn(gui, x + BTN_X, y + BTN_Y[3], BTN_W, BTN_H, Component.literal("模"), 0xFF3F5F8F);

        // AE 接入开关按钮（绿色 = 开 / 灰 = 关）
        int ax = x + AE_X;
        int ay = y + AE_Y;
        boolean aeOn = this.menu.isAeEnabled();
        gui.fill(ax, ay, ax + AE_SIZE, ay + AE_SIZE, 0xFF16181D);
        gui.fill(ax + 1, ay + 1, ax + AE_SIZE - 1, ay + AE_SIZE - 1, aeOn ? 0xFF57B757 : 0xFF3A4048);
        Component aeLabel = Component.literal("AE");
        gui.drawString(this.font, aeLabel, ax + (AE_SIZE - this.font.width(aeLabel)) / 2, ay + 6, 0xFFFFFF);
    }

    private void drawBtn(GuiGraphics gui, int bx, int by, int w, int h, Component label, int color)
    {
        gui.fill(bx, by, bx + w, by + h, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, color);
        gui.drawString(this.font, label, bx + 4, by + 3, 0xFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        gui.drawString(this.font, Component.literal("神之合成"), 60 + SHIFT, 6, 4210752, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"), 8 + SHIFT, 74, 4210752, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        // 左侧模板槽：Shift = 保存（20+t），否则 = 加载（10+t）
        for (int t = 0; t < GodCraftBlockEntity.TEMPLATE_COUNT; t++)
        {
            int tx = TPL_X0 + (t % 2) * TPL_SPACING_X;
            int ty = TPL_Y0 + (t / 2) * TPL_SPACING_Y;
            if (relX >= tx && relX < tx + TPL_SIZE && relY >= ty && relY < ty + TPL_SIZE)
            {
                sendButton(net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 20 + t : 10 + t);
                return true;
            }
        }

        if (relX >= BTN_X && relX < BTN_X + BTN_W && relY >= BTN_Y[0] && relY < BTN_Y[0] + BTN_H)
        {
            sendButton(0); // 锁定
            return true;
        }
        if (relX >= BTN_X && relX < BTN_X + BTN_W && relY >= BTN_Y[1] && relY < BTN_Y[1] + BTN_H)
        {
            sendButton(1); // 启动/停止
            return true;
        }
        if (relX >= BTN_X && relX < BTN_X + BTN_W && relY >= BTN_Y[2] && relY < BTN_Y[2] + BTN_H)
        {
            sendButton(2); // 面配置
            return true;
        }
        if (relX >= BTN_X && relX < BTN_X + BTN_W && relY >= BTN_Y[3] && relY < BTN_Y[3] + BTN_H)
        {
            sendButton(3); // 配方模板详情
            return true;
        }
        if (relX >= AE_X && relX < AE_X + AE_SIZE && relY >= AE_Y && relY < AE_Y + AE_SIZE)
        {
            sendButton(28); // AE 接入开关
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendButton(int buttonId)
    {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null)
        {
            conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, buttonId));
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 1.21.1：AbstractContainerScreen.render 内部已调用 renderBackground(gui, mouseX, mouseY, partialTick)（含 renderBg），
        // 子类不再手动调 renderBackground。
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
