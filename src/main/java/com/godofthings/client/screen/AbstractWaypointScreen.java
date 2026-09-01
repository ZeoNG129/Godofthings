package com.godofthings.client.screen;

import com.godofthings.menu.WaypointListMenu;
import com.godofthings.network.WaypointMessages;
import com.godofthings.waypoint.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

/**
 * 传送点列表界面公共基类：神之记录（方块打开）与快捷键（U 键打开）共用同一套渲染与交互。
 */
public abstract class AbstractWaypointScreen<T extends AbstractContainerMenu & WaypointListMenu> extends AbstractContainerScreen<T>
{
    private static final int ROWS = 9;
    private static final int ROW_H = 22;
    private static final int LIST_Y = 18;

    private static final int BTN_H = 16;
    private static final int BTN_TP_X = 204;
    private static final int BTN_PIN_X = 230;
    private static final int BTN_DEL_X = 256;
    private static final int BTN_TP_W = 24;
    private static final int BTN_PIN_W = 24;
    private static final int BTN_DEL_W = 20;

    private static final int PAGE_Y = 218;
    private static final int PAGE_H = 16;
    private static final int PAGE_PREV_X = 8;
    private static final int PAGE_NEXT_X = 208;
    private static final int PAGE_W = 64;

    private int page = 0;

    public AbstractWaypointScreen(T menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 280;
        this.imageHeight = 236;
        // 屏幕打开后主动请求点位列表（规避 openScreen 包与列表包时序颠倒）
        WaypointMessages.requestList();
    }

    private List<Waypoint> list()
    {
        return menu.getWaypoints();
    }

    private int totalPages()
    {
        return Math.max(1, (list().size() + ROWS - 1) / ROWS);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;
        gui.fill(x, y, x + imageWidth, y + imageHeight, 0xC0101010);
        gui.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFF1E1E22);
        gui.drawString(this.font, this.title, x + 8, y + 6, 0xFFFFFF);

        List<Waypoint> all = list();
        if (all.isEmpty())
        {
            Component empty = Component.translatable("gui.godofthings.waypoint.empty");
            gui.drawString(this.font, empty, x + 8, y + LIST_Y + 40, 0xAAAAAA);
        }
        else
        {
            if (page >= totalPages())
            {
                page = totalPages() - 1;
            }
            int start = page * ROWS;
            for (int i = 0; i < ROWS; i++)
            {
                int idx = start + i;
                if (idx >= all.size())
                {
                    break;
                }
                int rowY = y + LIST_Y + i * ROW_H;
                drawRow(gui, x, rowY, all.get(idx));
            }
        }

        // 翻页
        int pages = totalPages();
        int py = y + PAGE_Y;
        gui.fill(x + PAGE_PREV_X, py, x + PAGE_PREV_X + PAGE_W, py + PAGE_H, 0xFF3A4048);
        gui.fill(x + PAGE_NEXT_X, py, x + PAGE_NEXT_X + PAGE_W, py + PAGE_H, 0xFF3A4048);
        Component prev = Component.translatable("gui.godofthings.waypoint.prev");
        Component next = Component.translatable("gui.godofthings.waypoint.next");
        gui.drawString(this.font, prev, x + PAGE_PREV_X + (PAGE_W - this.font.width(prev)) / 2, py + 4, 0xFFFFFF);
        gui.drawString(this.font, next, x + PAGE_NEXT_X + (PAGE_W - this.font.width(next)) / 2, py + 4, 0xFFFFFF);
        Component pageInfo = Component.literal((page + 1) + " / " + pages);
        gui.drawString(this.font, pageInfo, x + imageWidth / 2 - this.font.width(pageInfo) / 2, py + 4, 0xCCCCCC);
    }

    private void drawRow(GuiGraphics gui, int x, int rowY, Waypoint wp)
    {
        String label = wp.pinned ? "[顶] " + wp.name : wp.name;
        label = this.font.plainSubstrByWidth(label, 92);
        gui.drawString(this.font, label, x + 8, rowY + 5, wp.pinned ? 0xFFE0B030 : 0xFFFFFF);

        String coord = Math.round(wp.x) + " " + Math.round(wp.y) + " " + Math.round(wp.z);
        coord = this.font.plainSubstrByWidth(coord, 96);
        gui.drawString(this.font, coord, x + 102, rowY + 5, 0xAAAAAA);

        int by = rowY + 3;
        drawButton(gui, x + BTN_TP_X, by, BTN_TP_W, "gui.godofthings.waypoint.tp");
        drawButton(gui, x + BTN_PIN_X, by, BTN_PIN_W, "gui.godofthings.waypoint.pin");
        drawButton(gui, x + BTN_DEL_X, by, BTN_DEL_W, "gui.godofthings.waypoint.del");
    }

    private void drawButton(GuiGraphics gui, int bx, int by, int w, String key)
    {
        gui.fill(bx, by, bx + w, by + BTN_H, 0xFF3A4048);
        Component text = Component.translatable(key);
        gui.drawString(this.font, text, bx + (w - this.font.width(text)) / 2, by + 4, 0xFFFFFF);
    }

    private static boolean in(int v, int begin, int width)
    {
        return v >= begin && v < begin + width;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        // 翻页
        if (in(relY, PAGE_Y, PAGE_H))
        {
            if (in(relX, PAGE_PREV_X, PAGE_W))
            {
                page = Math.max(0, page - 1);
                return true;
            }
            if (in(relX, PAGE_NEXT_X, PAGE_W))
            {
                page = Math.min(totalPages() - 1, page + 1);
                return true;
            }
        }

        // 行按钮
        List<Waypoint> all = list();
        int start = page * ROWS;
        for (int i = 0; i < ROWS; i++)
        {
            int idx = start + i;
            if (idx >= all.size())
            {
                break;
            }
            int rowY = LIST_Y + i * ROW_H;
            if (in(relY, rowY, ROW_H))
            {
                Waypoint wp = all.get(idx);
                if (in(relX, BTN_TP_X, BTN_TP_W))
                {
                    WaypointMessages.sendAction(WaypointMessages.ACTION_TELEPORT, wp.name);
                    return true;
                }
                if (in(relX, BTN_PIN_X, BTN_PIN_W))
                {
                    WaypointMessages.sendAction(WaypointMessages.ACTION_PIN, wp.name);
                    return true;
                }
                if (in(relX, BTN_DEL_X, BTN_DEL_W))
                {
                    WaypointMessages.sendAction(WaypointMessages.ACTION_DELETE, wp.name);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
