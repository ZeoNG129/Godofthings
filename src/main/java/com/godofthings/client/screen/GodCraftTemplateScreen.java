package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodCraftBlockEntity;
import com.godofthings.menu.GodCraftTemplateMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 神之合成配方模板详情界面。
 * 与主界面（GodCraftScreen）同宽同布局：左侧 46px 模板面板（8 槽，位置与主界面完全一致），
 * 右侧为工作台背景，配方预览对齐主界面的 3×3 合成格，产物对齐主界面结果槽。
 */
public class GodCraftTemplateScreen extends AbstractContainerScreen<GodCraftTemplateMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_craft.png");

    // 与主界面一致：176 工作台 + 左侧 46px 模板面板
    private static final int IMG_W = 176 + 46;
    private static final int IMG_H = 166;
    private static final int SHIFT = 46;

    // 左侧模板面板（与 GodCraftScreen 完全一致）
    private static final int TPL_X0 = 6;
    private static final int TPL_Y0 = 30;
    private static final int TPL_SPACING_X = 21;
    private static final int TPL_SPACING_Y = 21;
    private static final int TPL_SIZE = 18;

    // 配方预览：对齐主界面合成格（GodCraftMenu 槽位 30+SHIFT,17 起始）
    private static final int REC_X0 = 30 + SHIFT;
    private static final int REC_Y0 = 17;
    private static final int REC_SPACING = 18;
    private static final int REC_SIZE = 18;

    // 产物：对齐主界面结果槽（124+SHIFT,35）
    private static final int RESULT_X = 124 + SHIFT;
    private static final int RESULT_Y = 35;
    private static final int RESULT_SIZE = 18;

    // 加载按钮：位于左侧模板槽位（最底 y=111）下方；返回键与主界面按钮列对齐 BTN_X=146+SHIFT
    private static final int LOAD_X = 6;
    private static final int LOAD_Y = 116;
    private static final int LOAD_W = 34;
    private static final int LOAD_H = 14;
    private static final int BACK_X = 146 + SHIFT;
    private static final int BACK_Y = 8;
    private static final int BACK_W = 22;
    private static final int BACK_H = 14;

    private int selectedTemplate = -1;

    public GodCraftTemplateScreen(GodCraftTemplateMenu menu, Inventory playerInventory, Component title)
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

        // 左侧模板面板（与主界面一致）
        gui.fill(x, y, x + SHIFT, y + this.imageHeight, 0xFF101318);
        gui.fill(x + SHIFT - 1, y, x + SHIFT, y + this.imageHeight, 0xFF000000);

        // 原版工作台背景（右移 SHIFT）
        gui.blit(TEXTURE, x + SHIFT, y, 0, 0, 176, this.imageHeight);

        gui.drawString(this.font, Component.literal("模板"), x + 8, y + 12, 0x9AA0A8);
        gui.drawString(this.font, Component.literal("单击预览 / Shift+单击保存"), x + SHIFT + 8, y + 6, 0x9AA0A8);

        GodCraftBlockEntity be = ((GodCraftTemplateMenu) this.menu).getBlockEntity();

        // 8 个模板槽（位置与主界面左侧面板一致）
        for (int t = 0; t < GodCraftBlockEntity.TEMPLATE_COUNT; t++)
        {
            int tx = x + TPL_X0 + (t % 2) * TPL_SPACING_X;
            int ty = y + TPL_Y0 + (t / 2) * TPL_SPACING_Y;
            boolean has = be.hasTemplate(t);
            gui.fill(tx, ty, tx + TPL_SIZE, ty + TPL_SIZE, 0xFF16181D);
            gui.fill(tx + 1, ty + 1, tx + TPL_SIZE - 1, ty + TPL_SIZE - 1, has ? 0xFF2B5F2B : 0xFF2B2F38);
            if (t == this.selectedTemplate)
            {
                gui.fill(tx - 1, ty - 1, tx + TPL_SIZE + 1, ty + TPL_SIZE + 1, 0xFFFFD700);
            }
            ItemStack result = has ? be.getTemplateResult(t) : ItemStack.EMPTY;
            if (!result.isEmpty())
            {
                gui.renderItem(result, tx + 1, ty + 1);
            }
            gui.drawString(this.font, Component.literal(String.valueOf(t + 1)), tx + TPL_SIZE - 8, ty + TPL_SIZE - 8, 0xFFFFFF);
        }

        if (this.selectedTemplate >= 0 && be.hasTemplate(this.selectedTemplate))
        {
            // 配方预览：覆盖在合成格上（槽背景已由贴图绘制）
            for (int i = 0; i < 9; i++)
            {
                int rx = x + REC_X0 + 1 + (i % 3) * REC_SPACING;
                int ry = y + REC_Y0 + 1 + (i / 3) * REC_SPACING;
                ItemStack item = be.getTemplateItem(this.selectedTemplate, i);
                if (!item.isEmpty())
                {
                    gui.renderItem(item, rx, ry);
                }
            }

            // 产物：覆盖在结果槽上
            gui.drawString(this.font, Component.literal("产物"), x + RESULT_X, y + RESULT_Y - 11, 0x9AA0A8);
            ItemStack result = be.getTemplateResult(this.selectedTemplate);
            if (!result.isEmpty())
            {
                gui.renderItem(result, x + RESULT_X + 1, y + RESULT_Y + 1);
            }
            String name = result.isEmpty() ? "无" : result.getHoverName().getString();
            gui.drawString(this.font, Component.literal(name), x + RESULT_X, y + RESULT_Y + RESULT_SIZE + 2, 0xCCCCCC);
        }
        else if (this.selectedTemplate >= 0)
        {
            gui.drawString(this.font, Component.literal("模板为空"), x + REC_X0, y + REC_Y0 + REC_SPACING, 0x9AA0A8);
        }

        boolean canLoad = this.selectedTemplate >= 0 && be.hasTemplate(this.selectedTemplate);
        this.drawBtn(gui, x + LOAD_X, y + LOAD_Y, LOAD_W, LOAD_H, Component.literal("加载"), canLoad ? 0xFF2B5F2B : 0xFF4A4A4A);
        this.drawBtn(gui, x + BACK_X, y + BACK_Y, BACK_W, BACK_H, Component.literal("返"), 0xFF3F5F8F);
    }

    private void drawBtn(GuiGraphics gui, int bx, int by, int w, int h, Component label, int color)
    {
        gui.fill(bx, by, bx + w, by + h, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, color);
        gui.drawString(this.font, label, bx + (w - this.font.width(label)) / 2, by + 3, 0xFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        // 模板界面无玩家物品栏槽位，留空避免画出悬空的「物品栏」标签
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        // 模板槽：Shift+单击 = 保存（10+t），否则 = 预览（仅本地选中）
        for (int t = 0; t < GodCraftBlockEntity.TEMPLATE_COUNT; t++)
        {
            int tx = TPL_X0 + (t % 2) * TPL_SPACING_X;
            int ty = TPL_Y0 + (t / 2) * TPL_SPACING_Y;
            if (relX >= tx && relX < tx + TPL_SIZE && relY >= ty && relY < ty + TPL_SIZE)
            {
                if (Screen.hasShiftDown())
                {
                    this.sendButton(10 + t);
                }
                else
                {
                    this.selectedTemplate = t;
                }
                return true;
            }
        }

        // 加载按钮（20+选中模板）
        if (relX >= LOAD_X && relX < LOAD_X + LOAD_W && relY >= LOAD_Y && relY < LOAD_Y + LOAD_H)
        {
            if (this.selectedTemplate >= 0)
            {
                this.sendButton(20 + this.selectedTemplate);
            }
            return true;
        }
        // 返回按钮（30）
        if (relX >= BACK_X && relX < BACK_X + BACK_W && relY >= BACK_Y && relY < BACK_Y + BACK_H)
        {
            this.sendButton(30);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendButton(int buttonId)
    {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null)
        {
            conn.send(new ServerboundContainerButtonClickPacket(((GodCraftTemplateMenu) this.menu).containerId, buttonId));
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 1.21.1：AbstractContainerScreen.render 内部已调用 renderBackground（含 renderBg），子类不再手动调。
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
