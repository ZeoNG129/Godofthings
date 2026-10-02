package com.godofthings.client.screen;

import com.godofthings.client.ClientNoteCache;
import com.godofthings.client.GodNoteHud;
import com.godofthings.note.NoteBook;
import com.godofthings.note.NoteHud;
import com.godofthings.note.NoteTask;
import com.godofthings.network.GodNoteMessages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 神之便签的记事本界面（快捷键 / 物品右键 / 指令三种入口最终都开这个界面）。
 *
 * <p>两页：
 * <ul>
 *   <li><b>任务页</b>：一行一条，左边方框点击勾选、点文字改名、右边 × 删除；底部输入框新增。</li>
 *   <li><b>悬浮窗页</b>：开关、位置、大小、透明度、背景、是否显示已完成，
 *       以及「拖动摆放」——切到 {@link GodNoteHudEditScreen} 直接在屏幕上拖着放。</li>
 * </ul>
 *
 * <p>数据只有一份来源：{@link ClientNoteCache}（服务端下发的镜像）。界面里每次改动都
 * 把整本便签发回服务端，服务端兜底落库后再回发一份，所以界面与悬浮窗永远同步。</p>
 */
public class GodNoteScreen extends Screen
{
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 200;

    private static final int ROWS_TOP = 34;
    private static final int ROW_H = 12;
    private static final int VISIBLE_ROWS = 9;
    private static final int ROWS_BOTTOM = ROWS_TOP + VISIBLE_ROWS * ROW_H;

    private int left;
    private int top;

    /** 当前页：0 = 任务列表，1 = 悬浮窗设置 */
    private int page;
    /** 待切换的页（-1 = 无）。按钮回调里不直接 rebuildWidgets，避免在事件分发途中改控件列表 */
    private int pendingPage = -1;
    private int scroll;

    private EditBox input;
    private Button actionButton;
    private Button clearDoneButton;
    private Button hudToggleButton;
    private Button hudSettingsButton;

    private Button enableButton;
    private Button dragButton;
    private Button xMinus;
    private Button xPlus;
    private Button yMinus;
    private Button yPlus;
    private Button scaleMinus;
    private Button scalePlus;
    private Button opacityMinus;
    private Button opacityPlus;
    private Button bgPrev;
    private Button bgNext;
    private Button doneToggle;
    private Button backButton;

    /** 正在改名的那条任务下标，-1 = 新增模式 */
    private int editingIndex = -1;

    public GodNoteScreen()
    {
        super(Component.translatable("gui.godofthings.note.title"));
    }

    private NoteBook book()
    {
        return ClientNoteCache.book();
    }

    @Override
    protected void init()
    {
        super.init();
        // 每次打开都拉一次最新数据（很小的一条包）
        GodNoteMessages.requestSync();

        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;

        // 右上角关闭
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(left + PANEL_W - 24, top + 4, 18, 16).build());

        if (page == 0)
        {
            initTaskPage();
        }
        else
        {
            initHudPage();
        }
        refresh();
    }

    private void initTaskPage()
    {
        // 从悬浮窗页切回来时同理，先清掉上一页的控件引用
        enableButton = null;
        dragButton = null;
        xMinus = null;
        xPlus = null;
        yMinus = null;
        yPlus = null;
        scaleMinus = null;
        scalePlus = null;
        opacityMinus = null;
        opacityPlus = null;
        bgPrev = null;
        bgNext = null;
        doneToggle = null;
        backButton = null;

        input = new EditBox(this.font, left + 8, top + 148, 180, 18,
                Component.translatable("gui.godofthings.note.input"));
        input.setMaxLength(NoteTask.MAX_TEXT);
        input.setHint(Component.translatable("gui.godofthings.note.input"));
        addRenderableWidget(input);

        actionButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.add"), b -> submitInput())
                .bounds(left + 192, top + 148, 100, 18).build());

        clearDoneButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.clear_done"), b ->
                {
                    book().clearDone();
                    push();
                    refresh();
                }).bounds(left + 8, top + 172, 88, 18).build());

        hudToggleButton = addRenderableWidget(Button.builder(
                Component.empty(), b ->
                {
                    book().hud().enabled = !book().hud().enabled;
                    push();
                    refresh();
                }).bounds(left + 100, top + 172, 92, 18).build());

        hudSettingsButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.hud_settings"), b -> pendingPage = 1)
                .bounds(left + 196, top + 172, 96, 18).build());
    }

    private void initHudPage()
    {
        // 换页后原来的任务页控件已经不在控件列表里，这里清掉引用，避免 keyPressed/refresh 摸到旧对象
        input = null;
        actionButton = null;
        clearDoneButton = null;
        hudToggleButton = null;
        hudSettingsButton = null;
        editingIndex = -1;

        enableButton = addRenderableWidget(Button.builder(Component.empty(), b ->
        {
            book().hud().enabled = !book().hud().enabled;
            push();
            refresh();
        }).bounds(left + 8, top + 34, 140, 18).build());

        dragButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.drag"), b ->
                        this.minecraft.setScreen(new GodNoteHudEditScreen()))
                .bounds(left + 152, top + 34, 140, 18).build());

        // 位置 X / 位置 Y / 大小 / 透明度：[-] [+] + 数值
        xMinus = addRenderableWidget(stepButton(left + 96, top + 60, "-", () -> stepX(-1)));
        xPlus = addRenderableWidget(stepButton(left + 120, top + 60, "+", () -> stepX(1)));
        yMinus = addRenderableWidget(stepButton(left + 96, top + 84, "-", () -> stepY(-1)));
        yPlus = addRenderableWidget(stepButton(left + 120, top + 84, "+", () -> stepY(1)));
        scaleMinus = addRenderableWidget(stepButton(left + 96, top + 108, "-", () -> stepScale(-1)));
        scalePlus = addRenderableWidget(stepButton(left + 120, top + 108, "+", () -> stepScale(1)));
        opacityMinus = addRenderableWidget(stepButton(left + 96, top + 132, "-", () -> stepOpacity(-1)));
        opacityPlus = addRenderableWidget(stepButton(left + 120, top + 132, "+", () -> stepOpacity(1)));

        bgPrev = addRenderableWidget(stepButton(left + 96, top + 156, "<", () -> cycleBackground(-1)));
        bgNext = addRenderableWidget(stepButton(left + 120, top + 156, ">", () -> cycleBackground(1)));

        doneToggle = addRenderableWidget(Button.builder(Component.empty(), b ->
        {
            book().hud().showDone = !book().hud().showDone;
            push();
            refresh();
        }).bounds(left + 96, top + 180, 84, 18).build());

        backButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.back"), b -> pendingPage = 0)
                .bounds(left + 188, top + 180, 104, 18).build());
    }

    private Button stepButton(int x, int y, String label, Runnable action)
    {
        return Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, 20, 18).build();
    }

    // ------------------------------------------------------------------ 数据改动

    /** 把整本便签发回服务端（发副本，避免网络线程编码时主线程还在改同一份） */
    private void push()
    {
        GodNoteMessages.sendUpdate(book().copy());
    }

    private void submitInput()
    {
        if (input == null)
        {
            return;
        }
        String text = input.getValue();
        if (editingIndex >= 0)
        {
            book().setText(editingIndex, text);
            editingIndex = -1;
        }
        else
        {
            book().add(text);
        }
        input.setValue("");
        push();
        refresh();
    }

    private void startEditing(int index)
    {
        List<NoteTask> tasks = book().tasks();
        if (index < 0 || index >= tasks.size() || input == null)
        {
            return;
        }
        editingIndex = index;
        input.setValue(tasks.get(index).text);
        input.setFocused(true);
        refresh();
    }

    private void refresh()
    {
        if (actionButton != null)
        {
            actionButton.setMessage(Component.translatable(editingIndex >= 0
                    ? "gui.godofthings.note.save_edit"
                    : "gui.godofthings.note.add"));
        }
        NoteHud hud = book().hud();
        if (hudToggleButton != null)
        {
            hudToggleButton.setMessage(toggleText("gui.godofthings.note.hud", hud.enabled));
        }
        if (enableButton != null)
        {
            enableButton.setMessage(toggleText("gui.godofthings.note.hud", hud.enabled));
        }
        if (doneToggle != null)
        {
            doneToggle.setMessage(Component.translatable(hud.showDone
                    ? "gui.godofthings.note.on"
                    : "gui.godofthings.note.off"));
        }
        if (input != null)
        {
            input.setHint(Component.translatable(editingIndex >= 0
                    ? "gui.godofthings.note.input_edit"
                    : "gui.godofthings.note.input"));
        }
    }

    private Component toggleText(String key, boolean on)
    {
        return Component.translatable(key, Component.translatable(on
                ? "gui.godofthings.note.on"
                : "gui.godofthings.note.off"));
    }

    private void stepX(int delta)
    {
        NoteHud hud = book().hud();
        hud.x = Mth.clamp(hud.x + delta * NoteHud.POS_STEP, 0.0F, 1.0F);
        push();
    }

    private void stepY(int delta)
    {
        NoteHud hud = book().hud();
        hud.y = Mth.clamp(hud.y + delta * NoteHud.POS_STEP, 0.0F, 1.0F);
        push();
    }

    private void stepScale(int delta)
    {
        NoteHud hud = book().hud();
        hud.scale = Mth.clamp(hud.scale + delta * NoteHud.SCALE_STEP, NoteHud.MIN_SCALE, NoteHud.MAX_SCALE);
        push();
    }

    private void stepOpacity(int delta)
    {
        NoteHud hud = book().hud();
        hud.opacity = Mth.clamp(hud.opacity + delta * 0.1F, NoteHud.MIN_OPACITY, NoteHud.MAX_OPACITY);
        push();
    }

    private void cycleBackground(int delta)
    {
        NoteHud hud = book().hud();
        hud.background = Math.floorMod(hud.background + delta, NoteHud.BG_COUNT);
        push();
        refresh();
    }

    // ------------------------------------------------------------------ 渲染

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 换页放在这里做：按钮回调期间改控件列表会让事件分发中途断掉
        if (pendingPage >= 0)
        {
            page = pendingPage;
            pendingPage = -1;
            rebuildWidgets();
        }
        gui.fill(0, 0, this.width, this.height, 0x66000000);
        drawPanel(gui, mouseX, mouseY);
        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 自己画底色（见 render），这里留空避免叠两层
    }

    private void drawPanel(GuiGraphics gui, int mouseX, int mouseY)
    {
        gui.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, 0xFF0B0E12);
        gui.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF1A1F26);
        gui.fill(left, top, left + PANEL_W, top + 1, 0xFFE8C86A);
        gui.fill(left, top + PANEL_H - 1, left + PANEL_W, top + PANEL_H, 0xFFE8C86A);

        gui.drawString(this.font, Component.translatable("gui.godofthings.note.title"),
                left + 10, top + 8, 0xFFE8C86A, false);

        if (page == 0)
        {
            drawTaskPage(gui, mouseX, mouseY);
        }
        else
        {
            drawHudPage(gui);
        }
    }

    private void drawTaskPage(GuiGraphics gui, int mouseX, int mouseY)
    {
        NoteBook book = book();
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.counter",
                        book.doneCount(), book.tasks().size()),
                left + 10, top + 20, 0xFF9AA0A8, false);

        gui.fill(left + 8, top + ROWS_TOP - 3, left + PANEL_W - 8, top + ROWS_BOTTOM, 0xFF11151A);

        List<NoteTask> tasks = book.tasks();
        int maxScroll = Math.max(0, tasks.size() - VISIBLE_ROWS);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        if (tasks.isEmpty())
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.note.empty"),
                    left + 14, top + ROWS_TOP + 2, 0xFF7E8794, false);
        }

        for (int i = 0; i < VISIBLE_ROWS; i++)
        {
            int index = scroll + i;
            if (index >= tasks.size())
            {
                break;
            }
            NoteTask task = tasks.get(index);
            int rowY = top + ROWS_TOP + i * ROW_H;
            boolean hovered = isRowHovered(mouseX, mouseY, rowY);
            boolean editing = index == editingIndex;

            if (hovered || editing)
            {
                gui.fill(left + 9, rowY - 1, left + PANEL_W - 9, rowY + ROW_H - 2, 0x30FFFFFF);
            }
            drawCheckbox(gui, left + 12, rowY + 2, task.done);
            int textColor = task.done ? 0xFF9AA0A8 : 0xFFF2F4F8;
            String text = this.font.plainSubstrByWidth(task.text, PANEL_W - 60);
            gui.drawString(this.font, text, left + 26, rowY + 2, textColor, false);
            if (task.done)
            {
                gui.fill(left + 26, rowY + 6, left + 26 + this.font.width(text), rowY + 7, 0x80FFFFFF);
            }
            gui.drawString(this.font, "x", left + PANEL_W - 20, rowY + 2, 0xFFFF8080, false);
        }

        if (maxScroll > 0)
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.note.scroll_hint",
                            scroll + 1, Math.min(tasks.size(), scroll + VISIBLE_ROWS), tasks.size()),
                    left + PANEL_W - 118, top + 20, 0xFF6F7883, false);
        }
    }

    private void drawHudPage(GuiGraphics gui)
    {
        NoteHud hud = book().hud();
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.hud_hint"),
                left + 10, top + 20, 0xFF9AA0A8, false);

        rowLabel(gui, top + 60, "gui.godofthings.note.pos_x", hud.x, true);
        rowLabel(gui, top + 84, "gui.godofthings.note.pos_y", hud.y, true);
        rowLabel(gui, top + 108, "gui.godofthings.note.scale", hud.scale, false);
        rowLabel(gui, top + 132, "gui.godofthings.note.opacity", hud.opacity, false);

        gui.drawString(this.font, Component.translatable("gui.godofthings.note.background"),
                left + 10, top + 161, 0xFFF2F4F8, false);
        gui.drawString(this.font, Component.translatable(NoteHud.backgroundKey(hud.background)),
                left + 148, top + 161, 0xFFE8C86A, false);

        gui.drawString(this.font, Component.translatable("gui.godofthings.note.show_done_label"),
                left + 10, top + 185, 0xFFF2F4F8, false);
    }

    private void rowLabel(GuiGraphics gui, int rowY, String labelKey, float value, boolean percent)
    {
        gui.drawString(this.font, Component.translatable(labelKey), left + 10, rowY + 5, 0xFFF2F4F8, false);
        String valueText = percent
                ? Math.round(value * 100.0F) + "%"
                : String.format("%.2fx", value);
        gui.drawString(this.font, valueText, left + 148, rowY + 5, 0xFFE8C86A, false);
    }

    private void drawCheckbox(GuiGraphics gui, int x, int y, boolean done)
    {
        gui.fill(x, y, x + 9, y + 9, 0xFFBFC6D0);
        gui.fill(x + 1, y + 1, x + 8, y + 8, done ? 0xFF2E8B3D : 0xFF232A33);
        if (done)
        {
            gui.fill(x + 2, y + 4, x + 3, y + 5, 0xFFFFFFFF);
            gui.fill(x + 3, y + 5, x + 4, y + 6, 0xFFFFFFFF);
            gui.fill(x + 4, y + 3, x + 5, y + 4, 0xFFFFFFFF);
            gui.fill(x + 5, y + 2, x + 6, y + 3, 0xFFFFFFFF);
        }
    }

    private boolean isRowHovered(int mouseX, int mouseY, int rowY)
    {
        return mouseX >= left + 9 && mouseX <= left + PANEL_W - 9
                && mouseY >= rowY - 1 && mouseY < rowY + ROW_H - 2;
    }

    // ------------------------------------------------------------------ 交互

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (page == 0 && button == 0)
        {
            for (int i = 0; i < VISIBLE_ROWS; i++)
            {
                int rowY = top + ROWS_TOP + i * ROW_H;
                if (!isRowHovered((int) mouseX, (int) mouseY, rowY))
                {
                    continue;
                }
                int index = scroll + i;
                if (index >= book().tasks().size())
                {
                    break;
                }
                if (mouseX <= left + 24)
                {
                    book().toggle(index);
                    push();
                }
                else if (mouseX >= left + PANEL_W - 24)
                {
                    book().remove(index);
                    if (editingIndex == index)
                    {
                        editingIndex = -1;
                    }
                    push();
                    refresh();
                }
                else
                {
                    startEditing(index);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (page == 0)
        {
            scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (input != null && input.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER))
        {
            submitInput();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && editingIndex >= 0)
        {
            // 正在改名时按 Esc 只取消改名，不关界面
            editingIndex = -1;
            if (input != null)
            {
                input.setValue("");
            }
            refresh();
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
