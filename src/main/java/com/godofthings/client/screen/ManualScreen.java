package com.godofthings.client.screen;

import com.godofthings.manual.ManualCatalog;
import com.godofthings.manual.ManualCategory;
import com.godofthings.manual.ManualEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 神之手册：游戏内查「这东西是干嘛的」。
 *
 * <p>布局（一块 380×220 的面板）：顶部标题 + 搜索框；左列四个分类页签 + 条目列表（可滚动）；
 * 右列是选中条目的图标 + 标题 + 正文（可滚动）。鼠标滚轮按指针位置决定滚哪一列。</p>
 *
 * <p>条目来自 {@link ManualCatalog}：物品 / 方块是<b>直接从注册表生成</b>的（漏写语言键会被
 * {@code check-lang.ps1} 挡下），系统条目是固定的一批。三种入口（快捷键 P / 神之手册物品 /
 * {@code /godofthings manual}）最终都开这个界面。</p>
 */
public class ManualScreen extends Screen
{
    private static final int PANEL_W = 380;
    private static final int PANEL_H = 220;
    private static final int LIST_TOP = 46;
    private static final int ROW_H = 12;
    private static final int BODY_LINE_H = 10;

    private int left;
    private int top;

    private ManualCategory category = ManualCategory.START;
    private String query = "";
    private int listScroll;
    private int bodyScroll;
    private int selected;          // 在 filtered 列表里的下标
    private List<ManualEntry> shown = List.of();
    private List<FormattedCharSequence> bodyLines = List.of();
    private ManualEntry lastRendered; // 用来判断正文要不要重新折行

    private EditBox search;
    private Button[] tabs;

    public ManualScreen()
    {
        super(Component.translatable("gui.godofthings.manual.title"));
    }

    /** 打开手册（三种入口都走这里） */
    public static void open()
    {
        net.minecraft.client.Minecraft.getInstance().setScreen(new ManualScreen());
    }

    @Override
    protected void init()
    {
        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;
        this.listScroll = 0;
        this.bodyScroll = 0;

        // 搜索框：输入即筛（不用回车）
        search = new EditBox(this.font, left + 214, top + 6, 138, 16,
                Component.translatable("gui.godofthings.manual.search"));
        search.setMaxLength(32);
        search.setHint(Component.translatable("gui.godofthings.manual.search"));
        search.setValue(query);
        search.setResponder(text ->
        {
            query = text;
            selected = 0;
            listScroll = 0;
            rebuildList();
        });
        addRenderableWidget(search);

        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(left + PANEL_W - 22, top + 6, 16, 16).build());

        // 四个分类页签
        ManualCategory[] values = ManualCategory.values();
        tabs = new Button[values.length];
        int tabW = 62;
        for (int i = 0; i < values.length; i++)
        {
            ManualCategory cat = values[i];
            tabs[i] = addRenderableWidget(Button.builder(cat.label(), b ->
            {
                category = cat;
                selected = 0;
                listScroll = 0;
                bodyScroll = 0;
                rebuildList();
            }).bounds(left + 8 + i * (tabW + 2), top + 26, tabW, 16).build());
        }

        rebuildList();
    }

    private void rebuildList()
    {
        shown = ManualCatalog.filtered(category, query);
        if (selected >= shown.size())
        {
            selected = 0;
        }
        bodyScroll = 0;
        lastRendered = null;
    }

    private ManualEntry selectedEntry()
    {
        return shown.isEmpty() ? null : shown.get(Mth.clamp(selected, 0, shown.size() - 1));
    }

    private void rebuildBodyLines()
    {
        ManualEntry entry = selectedEntry();
        int width = PANEL_W - 152;
        bodyLines = entry == null ? List.of() : this.font.split(entry.body(), width);
        lastRendered = entry;
    }

    // ------------------------------------------------------------------ 渲染

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        gui.fill(0, 0, this.width, this.height, 0x66000000);
        gui.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, 0xFF0B0E12);
        gui.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF1A1F26);
        gui.fill(left, top, left + PANEL_W, top + 1, 0xFFE8C86A);
        gui.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFFE8C86A);

        gui.drawString(this.font, Component.translatable("gui.godofthings.manual.title"),
                left + 10, top + 11, 0xFFE8C86A, false);

        // 页签：当前分类高亮（按钮本身画字，这里只加一条下划线）
        int active = category.ordinal();
        if (tabs != null && active < tabs.length)
        {
            gui.fill(tabs[active].getX(), tabs[active].getY() + 16, tabs[active].getX() + 62,
                    tabs[active].getY() + 17, 0xFFE8C86A);
        }

        drawList(gui, mouseX, mouseY);
        drawBody(gui, mouseX, mouseY);

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 底色自己画
    }

    private void drawList(GuiGraphics gui, int mouseX, int mouseY)
    {
        int x0 = left + 8;
        int x1 = left + 128;
        gui.fill(x0, LIST_TOP - 2, x1, top + PANEL_H - 8, 0xFF11151A);

        if (shown.isEmpty())
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.manual.no_match"),
                    x0 + 4, LIST_TOP + 2, 0xFF7E8794, false);
            return;
        }

        int rows = (top + PANEL_H - 8 - (LIST_TOP - 2)) / ROW_H;
        int maxScroll = Math.max(0, shown.size() - rows);
        listScroll = Mth.clamp(listScroll, 0, maxScroll);

        for (int i = 0; i < rows; i++)
        {
            int index = listScroll + i;
            if (index >= shown.size())
            {
                break;
            }
            ManualEntry entry = shown.get(index);
            int rowY = LIST_TOP + i * ROW_H;
            boolean hovered = mouseX >= x0 && mouseX <= x1 && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (index == selected)
            {
                gui.fill(x0 + 1, rowY - 1, x1 - 1, rowY + ROW_H - 2, 0x50E8C86A);
            }
            else if (hovered)
            {
                gui.fill(x0 + 1, rowY - 1, x1 - 1, rowY + ROW_H - 2, 0x30FFFFFF);
            }
            gui.renderItem(entry.icon(), x0 + 2, rowY - 2);
            String title = this.font.plainSubstrByWidth(entry.title().getString(), 96);
            gui.drawString(this.font, title, x0 + 18, rowY + 1, 0xFFF2F4F8, false);
        }

        if (maxScroll > 0)
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.manual.count_hint",
                            shown.size(), listScroll + 1, Math.min(shown.size(), listScroll + rows)),
                    x0 + 2, top + PANEL_H - 18, 0xFF6F7883, false);
        }
        else
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.manual.count", shown.size()),
                    x0 + 2, top + PANEL_H - 18, 0xFF6F7883, false);
        }
    }

    private void drawBody(GuiGraphics gui, int mouseX, int mouseY)
    {
        ManualEntry entry = selectedEntry();
        int x = left + 138;
        int y = LIST_TOP - 2;
        int width = PANEL_W - 152;
        gui.fill(x - 4, y - 2, x + width + 6, top + PANEL_H - 8, 0xFF141920);

        if (entry == null)
        {
            return;
        }
        if (lastRendered != entry)
        {
            rebuildBodyLines();
        }

        // 图标 + 标题
        gui.renderItem(entry.icon(), x, y);
        gui.drawString(this.font, this.font.plainSubstrByWidth(entry.title().getString(), width - 24),
                x + 20, y + 4, 0xFFE8C86A, false);

        int textTop = y + 22;
        int textBottom = top + PANEL_H - 12;
        int visible = Math.max(1, (textBottom - textTop) / BODY_LINE_H);
        int maxScroll = Math.max(0, bodyLines.size() - visible);
        bodyScroll = Mth.clamp(bodyScroll, 0, maxScroll);

        int lineY = textTop;
        for (int i = bodyScroll; i < bodyLines.size() && lineY + BODY_LINE_H <= textBottom + 2; i++)
        {
            gui.drawString(this.font, bodyLines.get(i), x, lineY, 0xFFD5DAE1, false);
            lineY += BODY_LINE_H;
        }

        if (maxScroll > 0)
        {
            // 右侧细滚动条
            int trackTop = textTop;
            int trackH = textBottom - textTop;
            int barH = Math.max(8, trackH * visible / bodyLines.size());
            int barY = trackTop + (trackH - barH) * bodyScroll / maxScroll;
            gui.fill(x + width + 2, trackTop, x + width + 4, textBottom, 0x30FFFFFF);
            gui.fill(x + width + 2, barY, x + width + 4, barY + barH, 0x90E8C86A);
            gui.drawString(this.font, Component.translatable("gui.godofthings.manual.scroll_hint"),
                    x, top + PANEL_H - 18, 0xFF6F7883, false);
        }
    }

    // ------------------------------------------------------------------ 交互

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int x0 = left + 8;
        int x1 = left + 128;
        if (button == 0 && mouseX >= x0 && mouseX <= x1 && mouseY >= LIST_TOP - 2 && mouseY < top + PANEL_H - 8)
        {
            int row = (int) (mouseY - LIST_TOP) / ROW_H;
            int index = listScroll + row;
            if (index >= 0 && index < shown.size())
            {
                selected = index;
                bodyScroll = 0;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        int dir = (int) Math.signum(scrollY);
        if (mouseX >= left + 128)
        {
            bodyScroll = Math.max(0, bodyScroll - dir);
        }
        else
        {
            int rows = (top + PANEL_H - 8 - (LIST_TOP - 2)) / ROW_H;
            int maxScroll = Math.max(0, shown.size() - rows);
            listScroll = Mth.clamp(listScroll - dir, 0, maxScroll);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE)
        {
            onClose();
            return true;
        }
        // 上下键翻条目
        if (keyCode == GLFW.GLFW_KEY_DOWN && !shown.isEmpty())
        {
            selected = Math.min(selected + 1, shown.size() - 1);
            bodyScroll = 0;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP && !shown.isEmpty())
        {
            selected = Math.max(selected - 1, 0);
            bodyScroll = 0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
