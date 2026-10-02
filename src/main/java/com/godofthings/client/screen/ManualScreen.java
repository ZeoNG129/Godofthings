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
 * <p>布局（一块自适应大小的面板，居中）：</p>
 * <pre>
 * ┌──────────────────────────────────────────────┐
 * │ 神之手册                        [ 搜索…… ] [X] │  头行 (HEADER_Y)
 * │ [入门][物品][方块][系统]                       │  页签行 (TAB_Y)
 * ├───────────────┬──────────────────────────────┤
 * │ ▣ 条目 1      │  ▣ 标题                       │  列表列 (LIST_X..LIST_X+listW)
 * │ ▣ 条目 2      │  正文……（可滚轮翻）           │  正文列 (BODY_X..)
 * │ …             │                               │
 * │ 共 N 条       │  滚轮翻页                      │
 * └───────────────┴──────────────────────────────┘
 * </pre>
 *
 * <p><b>坐标纪律</b>：面板左上角是 {@code (left, top)}，本文件里所有 y 都必须是
 * {@code top + 偏移}、x 都是 {@code left + 偏移}。曾经踩过的坑：把 {@code LIST_TOP} 这个「面板内偏移」
 * 当成绝对 Y 用，结果列表与正文画到了屏幕顶部、而页签与搜索框还在面板里 —— 看起来就是「分类跑到中间」。</p>
 *
 * <p>条目来自 {@link ManualCatalog}：物品 / 方块是<b>直接从注册表生成</b>的（漏写语言键会被
 * {@code check-lang.ps1} 挡下），系统条目是固定的一批。三种入口（快捷键 P / 神之手册物品 /
 * {@code /godofthings manual}）最终都开这个界面。</p>
 */
public class ManualScreen extends Screen
{
    // ---- 面板内偏移（全部相对 left/top）----
    private static final int PAD = 8;
    private static final int HEADER_Y = 6;
    private static final int TAB_Y = 26;
    private static final int TAB_H = 16;
    private static final int LIST_TOP = 46;
    private static final int ROW_H = 12;
    private static final int BODY_LINE_H = 10;
    private static final int BOTTOM_PAD = 18;

    // ---- 自适应后的实际尺寸/位置（init 里算）----
    private int left;
    private int top;
    private int panelW;
    private int panelH;
    private int listX0;
    private int listX1;
    private int bodyX;
    private int bodyW;

    private ManualCategory category = ManualCategory.START;
    private String query = "";
    private int listScroll;
    private int bodyScroll;
    private int selected;
    private List<ManualEntry> shown = List.of();
    private List<FormattedCharSequence> bodyLines = List.of();
    private ManualEntry lastRendered;

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
        // 面板自适应：给上下左右各留 16 像素，最大 420×250。
        // 这样在很小的 GUI 尺寸（大界面缩放）下也不会顶出屏幕 —— 之前的固定 380×220 会挤成一团。
        this.panelW = Math.max(240, Math.min(420, this.width - 32));
        this.panelH = Math.max(160, Math.min(250, this.height - 32));
        this.left = (this.width - panelW) / 2;
        this.top = (this.height - panelH) / 2;

        // 左列（条目列表）占 34%，右列（正文）占剩下的
        int listW = Math.max(100, panelW * 34 / 100);
        this.listX0 = left + PAD;
        this.listX1 = listX0 + listW;
        this.bodyX = listX1 + 10;
        this.bodyW = left + panelW - PAD - bodyX - 4;

        this.listScroll = 0;
        this.bodyScroll = 0;

        // 头行右侧：搜索框（宽度自适应）+ 关闭按钮
        int closeSize = 16;
        int searchW = Math.max(80, Math.min(180, panelW / 2));
        int searchX = left + panelW - PAD - closeSize - 4 - searchW;
        search = new EditBox(this.font, searchX, top + HEADER_Y, searchW, 16,
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
                .bounds(left + panelW - PAD - closeSize, top + HEADER_Y, closeSize, 16).build());

        // 页签行：四个等宽页签铺满面板宽度
        ManualCategory[] values = ManualCategory.values();
        int tabW = Math.max(40, (panelW - PAD * 2 - (values.length - 1) * 2) / values.length);
        tabs = new Button[values.length];
        for (int i = 0; i < values.length; i++)
        {
            ManualCategory cat = values[i];
            Button button = Button.builder(cat.label(), b ->
            {
                category = cat;
                selected = 0;
                listScroll = 0;
                bodyScroll = 0;
                rebuildList();
            }).bounds(left + PAD + i * (tabW + 2), top + TAB_Y, tabW, TAB_H).build();
            tabs[i] = button;
            addRenderableWidget(button);
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

    /** 列表第一条所在的行 Y（面板内偏移 + top） */
    private int listTop()
    {
        return top + LIST_TOP;
    }

    private int listBottom()
    {
        return top + panelH - BOTTOM_PAD;
    }

    private int visibleRows()
    {
        return Math.max(1, (listBottom() - listTop()) / ROW_H);
    }

    private int bodyTop()
    {
        return top + LIST_TOP;
    }

    private int bodyBottom()
    {
        return top + panelH - BOTTOM_PAD;
    }

    private void rebuildBodyLines()
    {
        ManualEntry entry = selectedEntry();
        bodyLines = entry == null ? List.of() : this.font.split(entry.body(), bodyW - 14);
        lastRendered = entry;
    }

    // ------------------------------------------------------------------ 渲染

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        gui.fill(0, 0, this.width, this.height, 0x66000000);

        // 面板
        gui.fill(left - 1, top - 1, left + panelW + 1, top + panelH + 1, 0xFF0B0E12);
        gui.fill(left, top, left + panelW, top + panelH, 0xFF1A1F26);
        gui.fill(left, top, left + panelW, top + 1, 0xFFE8C86A);
        gui.fill(left, top + panelH - 1, left + panelW, top + panelH, 0xFFE8C86A);

        gui.drawString(this.font, Component.translatable("gui.godofthings.manual.title"),
                left + PAD, top + HEADER_Y + 4, 0xFFE8C86A, false);

        // 当前页签下面画一条金线（按钮自己画字）
        if (tabs != null && category.ordinal() < tabs.length)
        {
            Button active = tabs[category.ordinal()];
            gui.fill(active.getX(), active.getY() + TAB_H, active.getX() + active.getWidth(),
                    active.getY() + TAB_H + 1, 0xFFE8C86A);
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
        int top_ = listTop();
        int bottom = listBottom();
        gui.fill(listX0, top_ - 2, listX1, bottom, 0xFF11151A);

        if (shown.isEmpty())
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.manual.no_match"),
                    listX0 + 4, top_ + 2, 0xFF7E8794, false);
            return;
        }

        int rows = visibleRows();
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
            int rowY = top_ + i * ROW_H;
            boolean hovered = mouseX >= listX0 && mouseX <= listX1 && mouseY >= rowY && mouseY < rowY + ROW_H;
            if (index == selected)
            {
                gui.fill(listX0 + 1, rowY - 1, listX1 - 1, rowY + ROW_H - 2, 0x50E8C86A);
            }
            else if (hovered)
            {
                gui.fill(listX0 + 1, rowY - 1, listX1 - 1, rowY + ROW_H - 2, 0x30FFFFFF);
            }
            gui.renderItem(entry.icon(), listX0 + 2, rowY - 2);
            String title = this.font.plainSubstrByWidth(entry.title().getString(), listX1 - listX0 - 22);
            gui.drawString(this.font, title, listX0 + 18, rowY + 1, 0xFFF2F4F8, false);
        }

        Component hint = maxScroll > 0
                ? Component.translatable("gui.godofthings.manual.count_hint", shown.size(),
                        listScroll + 1, Math.min(shown.size(), listScroll + rows))
                : Component.translatable("gui.godofthings.manual.count", shown.size());
        gui.drawString(this.font, hint, listX0 + 2, top + panelH - 14, 0xFF6F7883, false);
    }

    private void drawBody(GuiGraphics gui, int mouseX, int mouseY)
    {
        int top_ = bodyTop();
        int bottom = bodyBottom();
        gui.fill(bodyX - 5, top_ - 2, bodyX + bodyW + 6, bottom, 0xFF141920);

        ManualEntry entry = selectedEntry();
        if (entry == null)
        {
            return;
        }
        if (lastRendered != entry)
        {
            rebuildBodyLines();
        }

        // 图标 + 标题（标题超宽就截断，绝不画出面板）
        gui.renderItem(entry.icon(), bodyX, top_);
        gui.drawString(this.font, this.font.plainSubstrByWidth(entry.title().getString(), bodyW - 26),
                bodyX + 20, top_ + 4, 0xFFE8C86A, false);

        int textTop = top_ + 22;
        int visible = Math.max(1, (bottom - textTop) / BODY_LINE_H);
        int maxScroll = Math.max(0, bodyLines.size() - visible);
        bodyScroll = Mth.clamp(bodyScroll, 0, maxScroll);

        int lineY = textTop;
        for (int i = bodyScroll; i < bodyLines.size() && lineY + BODY_LINE_H <= bottom + 1; i++)
        {
            gui.drawString(this.font, bodyLines.get(i), bodyX, lineY, 0xFFD5DAE1, false);
            lineY += BODY_LINE_H;
        }

        if (maxScroll > 0)
        {
            int trackH = bottom - textTop;
            int barH = Math.max(8, trackH * visible / bodyLines.size());
            int barY = textTop + (trackH - barH) * bodyScroll / maxScroll;
            gui.fill(bodyX + bodyW + 2, textTop, bodyX + bodyW + 4, bottom, 0x30FFFFFF);
            gui.fill(bodyX + bodyW + 2, barY, bodyX + bodyW + 4, barY + barH, 0x90E8C86A);
        }
    }

    // ------------------------------------------------------------------ 交互

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && mouseX >= listX0 && mouseX <= listX1
                && mouseY >= listTop() - 2 && mouseY < listBottom())
        {
            int row = (int) (mouseY - listTop()) / ROW_H;
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
        if (mouseX >= listX1)
        {
            bodyScroll = Math.max(0, bodyScroll - dir);
        }
        else
        {
            int maxScroll = Math.max(0, shown.size() - visibleRows());
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
