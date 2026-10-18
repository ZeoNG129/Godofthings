package com.godofthings.godgrid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;

/**
 * 六台神之机器共用的网格界面 —— 几何一比一照抄 PBG「无限通用机械离心机工厂」，
 * 全部用 {@link GuiGraphics#fill} 绘制（不 blit 贴图）。
 *
 * <p>与原版一致的部分：面板 362×218、输入行 y=17、进度箭头 y=33、输出三排 y=57/77/97、
 * 物品栏 y=135 居中。改动的部分：删掉流体罐 / 多流体页签 / PB 升级页签 / Mek 升级槽，
 * 原流体罐位置改成 1 格加速槽，左右两侧只留「面配置」「AE」「排序」三个按钮。</p>
 */
public abstract class AbstractGodGridScreen<T extends AbstractGodGridMenu> extends AbstractContainerScreen<T>
{
    protected AbstractGodGridScreen(T menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = GodGridLayout.IMAGE_W;
        this.imageHeight = GodGridLayout.IMAGE_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;

        // 面板（原版是 Mekanism 的 176 底图横向拉伸，这里直接画）
        gui.fill(x, y, x + this.imageWidth, y + this.imageHeight, GodGridLayout.COLOR_PANEL);
        gui.fill(x, y, x + this.imageWidth, y + 1, GodGridLayout.COLOR_PANEL_LIGHT);
        gui.fill(x, y, x + 1, y + this.imageHeight, GodGridLayout.COLOR_PANEL_LIGHT);
        gui.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, GodGridLayout.COLOR_PANEL_DARK);
        gui.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, GodGridLayout.COLOR_PANEL_DARK);

        for (int i = 0; i < GodGridLayout.INPUT_COUNT; i++)
        {
            drawSlot(gui, x + GodGridLayout.inputX(i), y + GodGridLayout.INPUT_Y,
                    GodGridLayout.COLOR_INPUT_BORDER);
        }
        for (int i = 0; i < GodGridLayout.COLS; i++)
        {
            drawArrow(gui, x + GodGridLayout.columnX(i) + (GodGridLayout.SLOT - 8) / 2,
                    y + GodGridLayout.ARROW_Y);
        }
        for (int j = 0; j < GodGridLayout.OUTPUT_COUNT; j++)
        {
            drawSlot(gui, x + GodGridLayout.outputX(j), y + GodGridLayout.outputY(j),
                    GodGridLayout.COLOR_OUTPUT_BORDER);
        }
        // 原版流体罐位置（x=7,y=84）→ 1 格加速槽
        drawSlot(gui, x + GodGridLayout.accelX(), y + GodGridLayout.ACCEL_Y,
                GodGridLayout.COLOR_ACCEL_BORDER);

        // 左侧页签：面配置（只有真正有面配置的机器才画）
        if (this.menu.host.gridSupportsFaceConfig())
        {
            drawTab(gui, x + GodGridLayout.TAB_LEFT_X, y + GodGridLayout.TAB_FACE_Y,
                    Component.translatable("gui.godofthings.grid.face"), isHovered(mouseX, mouseY,
                            x + GodGridLayout.TAB_LEFT_X, y + GodGridLayout.TAB_FACE_Y), 0xFF6B6B6B);
        }
        // 右侧页签：AE / 排序
        drawTab(gui, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_AE_Y,
                Component.literal("AE"), isHovered(mouseX, mouseY,
                        x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_AE_Y),
                this.menu.isAeEnabled() ? 0xFF2F8F3F : 0xFF3A4048);
        drawTab(gui, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_SORT_Y,
                Component.translatable(GodGridSorting.modeLabelKey(this.menu.sortMode())),
                isHovered(mouseX, mouseY, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_SORT_Y),
                0xFF2F5F8F);
    }

    private void drawSlot(GuiGraphics gui, int sx, int sy, int border)
    {
        gui.fill(sx, sy, sx + GodGridLayout.SLOT, sy + GodGridLayout.SLOT, border);
        gui.fill(sx + 1, sy + 1, sx + GodGridLayout.SLOT - 1, sy + GodGridLayout.SLOT - 1,
                GodGridLayout.COLOR_SLOT_INNER);
    }

    /** 原版是 ProgressType.DOWN 的箭头贴图，这里用 4 段矩形近似同尺寸（8×20） */
    private void drawArrow(GuiGraphics gui, int ax, int ay)
    {
        int color = GodGridLayout.COLOR_ARROW;
        gui.fill(ax + 3, ay, ax + 5, ay + 12, color);
        gui.fill(ax + 2, ay + 12, ax + 6, ay + 14, color);
        gui.fill(ax + 1, ay + 14, ax + 7, ay + 16, color);
        gui.fill(ax, ay + 16, ax + 8, ay + 18, color);
    }

    private void drawTab(GuiGraphics gui, int bx, int by, Component label, boolean hovered, int fillColor)
    {
        int size = GodGridLayout.TAB_SIZE;
        // Mekanism 页签观感：深底 + 左上高光 + 右下阴影
        gui.fill(bx, by, bx + size, by + size, 0xFF1B1B1B);
        gui.fill(bx + 1, by + 1, bx + size - 1, by + size - 1,
                hovered ? GodGridLayout.COLOR_BUTTON_BG_HOVER : fillColor);
        gui.fill(bx + 1, by + 1, bx + size - 1, by + 2, 0xFF9A9A9A);
        gui.fill(bx + 1, by + 1, bx + 2, by + size - 1, 0xFF9A9A9A);
        gui.fill(bx + 1, by + size - 2, bx + size - 1, by + size - 1, 0xFF2B2B2B);
        gui.fill(bx + size - 2, by + 1, bx + size - 1, by + size - 1, 0xFF2B2B2B);
        int textWidth = this.font.width(label);
        gui.drawString(this.font, label, bx + (size - textWidth) / 2, by + (size - 8) / 2, 0xFFFFFFFF, false);
    }

    private boolean isHovered(int mouseX, int mouseY, int bx, int by)
    {
        // 没有面配置的机器（掉落机 / 怪蛋机）不画左侧页签，命中检测也一律不成立
        if (!this.menu.host.gridSupportsFaceConfig()
                && bx == this.leftPos + GodGridLayout.TAB_LEFT_X
                && by == this.topPos + GodGridLayout.TAB_FACE_Y)
        {
            return false;
        }
        int size = GodGridLayout.TAB_SIZE;
        return mouseX >= bx && mouseX < bx + size && mouseY >= by && mouseY < by + size;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        // 原版工厂界面标题居中在顶部
        gui.drawString(this.font, this.title,
                (this.imageWidth - this.font.width(this.title)) / 2, 6,
                GodGridLayout.COLOR_TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"),
                GodGridLayout.INVENTORY_X, GodGridLayout.INVENTORY_Y - 12,
                GodGridLayout.COLOR_TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int x = this.leftPos;
        int y = this.topPos;
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (isHovered(mx, my, x + GodGridLayout.TAB_LEFT_X, y + GodGridLayout.TAB_FACE_Y))
        {
            sendButton(AbstractGodGridMenu.BTN_FACE_CONFIG);
            return true;
        }
        if (isHovered(mx, my, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_AE_Y))
        {
            sendButton(AbstractGodGridMenu.BTN_AE_TOGGLE);
            return true;
        }
        if (isHovered(mx, my, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_SORT_Y))
        {
            sendButton(AbstractGodGridMenu.BTN_SORT);
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
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        int x = this.leftPos;
        int y = this.topPos;

        if (isHovered(mouseX, mouseY, x + GodGridLayout.TAB_LEFT_X, y + GodGridLayout.TAB_FACE_Y))
        {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.godofthings.grid.face.tooltip"), mouseX, mouseY);
        }
        else if (isHovered(mouseX, mouseY, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_AE_Y))
        {
            gui.renderTooltip(this.font, Component.translatable(this.menu.isAeEnabled()
                    ? "gui.godofthings.grid.ae.on" : "gui.godofthings.grid.ae.off"), mouseX, mouseY);
        }
        else if (isHovered(mouseX, mouseY, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_SORT_Y))
        {
            gui.renderTooltip(this.font, Component.translatable("gui.godofthings.grid.sort.tooltip",
                    Component.translatable(GodGridSorting.modeLabelKey(this.menu.sortMode()))), mouseX, mouseY);
        }
        else if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()
                && mouseX >= x + GodGridLayout.ACCEL_X && mouseX < x + GodGridLayout.ACCEL_X + GodGridLayout.SLOT
                && mouseY >= y + GodGridLayout.ACCEL_Y && mouseY < y + GodGridLayout.ACCEL_Y + GodGridLayout.SLOT)
        {
            gui.renderTooltip(this.font, Component.translatable("gui.godofthings.grid.accel.tooltip",
                    this.menu.host.gridOperationsPerColumn()), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
