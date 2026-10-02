package com.godofthings.client.screen;

import com.godofthings.client.ClientNoteCache;
import com.godofthings.note.NoteBook;
import com.godofthings.note.NoteHud;
import com.godofthings.note.NoteShelf;
import com.godofthings.note.NoteTask;
import com.godofthings.network.GodNoteMessages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之便签的记事本界面（快捷键 / 物品右键 / 指令三种入口最终都开这个界面）。
 *
 * <p>两页：
 * <ul>
 *   <li><b>任务页</b>：顶部一条「便签册」栏（切换 / 新建 / 删除一本），下面是一行一条的任务列表 ——
 *       点方框勾选、<b>按住整行上下拖拽排序</b>、点文字改名、右边 × 删单条，底部输入框新增。</li>
 *   <li><b>悬浮窗页</b>：开关、位置、大小、透明度、背景、是否显示已完成，以及「拖动摆放」。</li>
 * </ul>
 *
 * <p>数据只有一份来源：{@link ClientNoteCache}（服务端下发的镜像）。界面里每次改动都把整册
 * 发回服务端，服务端兜底落库后再回发一份，所以界面与悬浮窗永远同步。</p>
 */
public class GodNoteScreen extends Screen
{
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 200;

    /** 便签册栏（切换 / 新建 / 删除） */
    private static final int BOOK_BAR_Y = 32;
    /** 任务区（7 行：第 8 行的位置让给了提示行，避免提示文字压进输入框） */
    private static final int ROWS_TOP = 52;
    private static final int ROW_H = 12;
    private static final int VISIBLE_ROWS = 7;
    private static final int ROWS_BOTTOM = ROWS_TOP + VISIBLE_ROWS * ROW_H;
    /** 任务区与输入框之间的提示行（拖拽 / 删除确认 / 子任务模式） */
    private static final int HINT_Y = ROWS_BOTTOM + 3;

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

    private Button prevBookButton;
    private Button nextBookButton;
    private Button addBookButton;
    private Button deleteBookButton;

    /** 悬浮窗标题的名字输入框 + 自动更名开关（都在悬浮窗页） */
    private EditBox nameField;
    private Button autoNameButton;
    /** 名字输入框里有还没提交的改动 */
    private boolean nameDirty;

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

    /** 输入框的三种模式：新增主任务 / 改名（主任务或子任务）/ 给某条主任务加子任务 */
    private static final int MODE_ADD = 0;
    private static final int MODE_EDIT = 1;
    private static final int MODE_SUB = 2;

    /** 输入框当前模式与目标（MODE_EDIT / MODE_SUB 时有效） */
    private int inputMode = MODE_ADD;
    private int editParent = -1;
    private int editChild = -1;
    private int subParent = -1;

    /** 正在拖拽排序的那一行（rows 下标），-1 = 没在拖 */
    private int dragRow = -1;
    private boolean dragMoved;
    /** 删除便签本需要点两次（第一次变成「确认删除」） */
    private boolean bookDeleteArmed;
    /** 删除任意一行都点两次确认：第一次点 × 后记住这一行（rows 下标） */
    private int deleteArmedRow = -1;

    /**
     * 扁平化后的一行。主任务：{@code parent = 自己的下标、child = -1}；
     * 子任务：{@code parent = 父任务下标、child = 自己在父任务里的下标}。
     * 这样勾选 / 删除 / 改名都能直接落到 {@link NoteBook} 的对应方法上。
     */
    private record RowRef(NoteTask task, int parent, int child) {}

    /** 任务列表是否处于「编辑中」状态（用于 Esc 与高亮） */
    private boolean editingTask()
    {
        return inputMode == MODE_EDIT;
    }

    public GodNoteScreen()
    {
        super(Component.translatable("gui.godofthings.note.title"));
    }

    private NoteShelf shelf()
    {
        return ClientNoteCache.shelf();
    }

    private NoteBook book()
    {
        return shelf().current();
    }

    @Override
    protected void init()
    {
        super.init();
        // 每次打开都拉一次最新数据（很小的一条包）
        GodNoteMessages.requestSync();

        this.left = (this.width - PANEL_W) / 2;
        this.top = (this.height - PANEL_H) / 2;
        this.bookDeleteArmed = false;

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
        nameField = null;
        autoNameButton = null;
        nameDirty = false;

        // ---- 便签册栏 ----
        prevBookButton = addRenderableWidget(Button.builder(Component.literal("<"), b ->
        {
            bookDeleteArmed = false;
            shelf().select(shelf().selected() - 1);
            scroll = 0;
            push();
            refresh();
        }).bounds(left + 8, top + BOOK_BAR_Y, 16, 16).build());

        nextBookButton = addRenderableWidget(Button.builder(Component.literal(">"), b ->
        {
            bookDeleteArmed = false;
            shelf().select(shelf().selected() + 1);
            scroll = 0;
            push();
            refresh();
        }).bounds(left + 148, top + BOOK_BAR_Y, 16, 16).build());

        addBookButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.book_new"), b ->
                {
                    bookDeleteArmed = false;
                    if (shelf().addBook("") >= 0)
                    {
                        scroll = 0;
                        push();
                        refresh();
                    }
                }).bounds(left + 170, top + BOOK_BAR_Y, 58, 16).build());

        deleteBookButton = addRenderableWidget(Button.builder(Component.empty(), b ->
        {
            if (!bookDeleteArmed)
            {
                bookDeleteArmed = true;
                refresh();
                return;
            }
            bookDeleteArmed = false;
            if (shelf().removeBook(shelf().selected()))
            {
                scroll = 0;
                push();
            }
            refresh();
        }).bounds(left + 232, top + BOOK_BAR_Y, 60, 16).build());

        // ---- 任务输入 / 操作 ----
        input = new EditBox(this.font, left + 8, top + 152, 180, 18,
                Component.translatable("gui.godofthings.note.input"));
        input.setMaxLength(NoteTask.MAX_TEXT);
        input.setHint(Component.translatable("gui.godofthings.note.input"));
        addRenderableWidget(input);

        actionButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.add"), b -> submitInput())
                .bounds(left + 192, top + 152, 100, 18).build());

        clearDoneButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.clear_done"), b ->
                {
                    bookDeleteArmed = false;
                    book().clearDone();
                    push();
                    refresh();
                }).bounds(left + 8, top + 174, 88, 18).build());

        hudToggleButton = addRenderableWidget(Button.builder(
                Component.empty(), b ->
                {
                    bookDeleteArmed = false;
                    shelf().hud().enabled = !shelf().hud().enabled;
                    push();
                    refresh();
                }).bounds(left + 100, top + 174, 92, 18).build());

        hudSettingsButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.hud_settings"), b -> pendingPage = 1)
                .bounds(left + 196, top + 174, 96, 18).build());
    }

    private void initHudPage()
    {
        // 换页后原来的任务页控件已经不在控件列表里，这里清掉引用，避免 keyPressed/refresh 摸到旧对象
        input = null;
        actionButton = null;
        clearDoneButton = null;
        hudToggleButton = null;
        hudSettingsButton = null;
        prevBookButton = null;
        nextBookButton = null;
        addBookButton = null;
        deleteBookButton = null;
        inputMode = MODE_ADD;
        editParent = editChild = subParent = -1;
        dragRow = -1;
        deleteArmedRow = -1;
        nameDirty = false;

        // ① 名称（当前这一本的悬浮窗标题）
        nameField = new EditBox(this.font, left + 80, top + 30, 212, 18,
                Component.translatable("gui.godofthings.note.name"));
        nameField.setMaxLength(NoteBook.MAX_NAME);
        nameField.setHint(Component.translatable("gui.godofthings.note.name_hint"));
        nameField.setValue(book().name());
        addRenderableWidget(nameField);

        // ② 自动更名：开启后名字固定为当日日期（MM.dd），服务端登录 / 定时都会刷
        autoNameButton = addRenderableWidget(Button.builder(Component.empty(), b ->
        {
            boolean on = !book().autoName();
            book().setAutoName(on);
            if (on)
            {
                // 本地先按当日日期填一下，等回包这一小会儿显示也一致（服务端会以它的日期为准再回发）
                book().applyAutoName();
                if (nameField != null)
                {
                    nameField.setValue(book().name());
                }
            }
            nameDirty = false;
            push();
            refresh();
        }).bounds(left + 80, top + 54, 60, 18).build());

        // ③ 悬浮窗总开关 + 拖动摆放
        enableButton = addRenderableWidget(Button.builder(Component.empty(), b ->
        {
            shelf().hud().enabled = !shelf().hud().enabled;
            push();
            refresh();
        }).bounds(left + 80, top + 78, 60, 18).build());

        dragButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.drag"), b ->
                {
                    commitName();
                    this.minecraft.setScreen(new GodNoteHudEditScreen());
                })
                .bounds(left + 148, top + 78, 144, 18).build());

        // ④ 位置：X / Y 各一个 [-] [+] 与百分比（坐标按英文标签宽度留的余量）
        xMinus = addRenderableWidget(stepButton(left + 66, top + 102, "-", () -> stepX(-1)));
        xPlus = addRenderableWidget(stepButton(left + 88, top + 102, "+", () -> stepX(1)));
        yMinus = addRenderableWidget(stepButton(left + 204, top + 102, "-", () -> stepY(-1)));
        yPlus = addRenderableWidget(stepButton(left + 226, top + 102, "+", () -> stepY(1)));

        // ⑤ 大小 / 透明度
        scaleMinus = addRenderableWidget(stepButton(left + 52, top + 126, "-", () -> stepScale(-1)));
        scalePlus = addRenderableWidget(stepButton(left + 74, top + 126, "+", () -> stepScale(1)));
        opacityMinus = addRenderableWidget(stepButton(left + 196, top + 126, "-", () -> stepOpacity(-1)));
        opacityPlus = addRenderableWidget(stepButton(left + 218, top + 126, "+", () -> stepOpacity(1)));

        // ⑥ 背景 / 是否显示已完成条目
        bgPrev = addRenderableWidget(stepButton(left + 52, top + 150, "<", () -> cycleBackground(-1)));
        bgNext = addRenderableWidget(stepButton(left + 74, top + 150, ">", () -> cycleBackground(1)));
        doneToggle = addRenderableWidget(Button.builder(Component.empty(), b ->
        {
            shelf().hud().showDone = !shelf().hud().showDone;
            push();
            refresh();
        }).bounds(left + 216, top + 150, 76, 18).build());

        // ⑦ 返回任务页
        backButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.note.back"), b -> pendingPage = 0)
                .bounds(left + 80, top + 174, 212, 18).build());
    }

    private Button stepButton(int x, int y, String label, Runnable action)
    {
        return Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, 20, 18).build();
    }

    // ------------------------------------------------------------------ 数据改动

    /** 把整册便签发回服务端（发副本，避免网络线程编码时主线程还在改同一份）；内容没变就不发 */
    private void push()
    {
        if (!ClientNoteCache.shouldPush(shelf()))
        {
            return;
        }
        GodNoteMessages.sendUpdate(shelf().copy());
    }

    private void submitInput()
    {
        if (input == null)
        {
            return;
        }
        String text = input.getValue();
        switch (inputMode)
        {
            case MODE_EDIT ->
            {
                if (editParent >= 0 && editChild >= 0)
                {
                    book().setChildText(editParent, editChild, text);
                }
                else if (editParent >= 0)
                {
                    book().setText(editParent, text);
                }
                inputMode = MODE_ADD;
                editParent = editChild = -1;
            }
            case MODE_SUB ->
            {
                if (text.isBlank())
                {
                    return; // 空回车 = 什么都不做，继续留在子任务模式
                }
                if (subParent >= 0 && book().addChild(subParent, text) >= 0)
                {
                    // 成功后**保持子任务模式**：可以连着加「热核 / 工业先锋 / …」一串，
                    // 加完按 Esc 或切到别处才退出 —— 之前每条都退出，才会让人以为只能加一个
                    input.setValue("");
                    push();
                    refresh();
                    return;
                }
                // 加不进去（父任务没了 / 子任务满）→ 退出子任务模式
                inputMode = MODE_ADD;
                subParent = -1;
            }
            default -> book().add(text);
        }
        input.setValue("");
        push();
        refresh();
    }

    /** 把输入框切到「给第 parent 行加子任务」模式 */
    private void startSubTask(int parentIndex)
    {
        if (input == null || parentIndex < 0 || parentIndex >= book().tasks().size())
        {
            return;
        }
        inputMode = MODE_SUB;
        subParent = parentIndex;
        editParent = editChild = -1;
        input.setValue("");
        input.setFocused(true);
        refresh();
    }

    /** 进入「改名」模式：主任务或子任务（rows 下标定位） */
    private void startEditing(int rowIndex)
    {
        RowRef row = rowAt(rowIndex);
        if (row == null || input == null)
        {
            return;
        }
        inputMode = MODE_EDIT;
        if (row.child() >= 0)
        {
            editParent = row.parent();
            editChild = row.child();
            input.setValue(row.task().text);
        }
        else
        {
            editParent = row.parent();
            editChild = -1;
            input.setValue(row.task().text);
        }
        subParent = -1;
        input.setFocused(true);
        refresh();
    }

    /** 取消输入框的编辑 / 子任务状态，回到新增模式 */
    private void resetInput()
    {
        inputMode = MODE_ADD;
        editParent = editChild = subParent = -1;
        if (input != null)
        {
            input.setValue("");
        }
        refresh();
    }

    /** Tab = 降级为上一条主任务的子任务；Shift+Tab = 升级为主任务（编辑中的那条） */
    private void handleIndentKeys(int keyCode, int modifiers)
    {
        if (input == null || !input.isFocused() || inputMode != MODE_EDIT)
        {
            return;
        }
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (keyCode != GLFW.GLFW_KEY_TAB)
        {
            return;
        }
        if (shift)
        {
            // 升级：子任务 → 主任务（插在父任务后面）
            if (editParent >= 0 && editChild >= 0 && book().promoteChild(editParent, editChild))
            {
                push();
                resetInput();
            }
            return;
        }
        // 降级：主任务 → 上一条主任务的子任务（子任务不能再降，只有两级）
        if (editChild >= 0 || editParent <= 0 || !book().demoteTop(editParent))
        {
            return;
        }
        push();
        resetInput();
    }

    private void refresh()
    {
        if (actionButton != null)
        {
            String key = switch (inputMode)
            {
                case MODE_EDIT -> "gui.godofthings.note.save_edit";
                case MODE_SUB -> "gui.godofthings.note.save_sub";
                default -> "gui.godofthings.note.add";
            };
            actionButton.setMessage(Component.translatable(key));
        }
        NoteHud hud = shelf().hud();
        if (hudToggleButton != null)
        {
            hudToggleButton.setMessage(toggleText("gui.godofthings.note.hud", hud.enabled));
        }
        if (deleteBookButton != null)
        {
            deleteBookButton.setMessage(Component.translatable(bookDeleteArmed
                    ? "gui.godofthings.note.book_delete_confirm"
                    : "gui.godofthings.note.book_delete"));
        }
        if (enableButton != null)
        {
            // 悬浮窗页的按钮只有 60px 宽，这里只显示开 / 关（行首已经画了标签）
            enableButton.setMessage(onOff(hud.enabled));
        }
        if (autoNameButton != null)
        {
            autoNameButton.setMessage(onOff(book().autoName()));
        }
        if (doneToggle != null)
        {
            doneToggle.setMessage(Component.translatable(hud.showDone
                    ? "gui.godofthings.note.show"
                    : "gui.godofthings.note.hide"));
        }
        if (input != null)
        {
            String key = switch (inputMode)
            {
                case MODE_EDIT -> "gui.godofthings.note.input_edit";
                case MODE_SUB -> "gui.godofthings.note.input_sub";
                default -> "gui.godofthings.note.input";
            };
            input.setHint(Component.translatable(key));
        }
    }

    /** 开 / 关 */
    private Component onOff(boolean on)
    {
        return Component.translatable(on ? "gui.godofthings.note.on" : "gui.godofthings.note.off");
    }

    private Component toggleText(String key, boolean on)
    {
        return Component.translatable(key, onOff(on));
    }

    /** 把名字输入框里还没提交的内容写进本地镜像并发服务端 */
    private void commitName()
    {
        if (nameField == null || !nameDirty)
        {
            return;
        }
        nameDirty = false;
        book().setName(nameField.getValue());
        if (book().autoName())
        {
            // 手动起名 = 关掉自动更名，否则下一 tick 就被当日日期覆盖了
            book().setAutoName(false);
        }
        push();
    }

    @Override
    public void tick()
    {
        super.tick();
        if (nameField == null)
        {
            return;
        }
        if (nameField.isFocused())
        {
            nameDirty = true; // 玩家正在打字，先别提交
        }
        else if (nameDirty)
        {
            commitName();
            refresh();
        }
        else if (!nameField.getValue().equals(book().name()))
        {
            // 服务端把名字改了（开着自动更名 / 跨天刷新）：输入框跟着回读
            nameField.setValue(book().name());
        }
    }

    @Override
    public void onClose()
    {
        commitName();
        super.onClose();
    }

    private void stepX(int delta)
    {
        NoteHud hud = shelf().hud();
        hud.x = Mth.clamp(hud.x + delta * NoteHud.POS_STEP, 0.0F, 1.0F);
        push();
    }

    private void stepY(int delta)
    {
        NoteHud hud = shelf().hud();
        hud.y = Mth.clamp(hud.y + delta * NoteHud.POS_STEP, 0.0F, 1.0F);
        push();
    }

    private void stepScale(int delta)
    {
        NoteHud hud = shelf().hud();
        hud.scale = Mth.clamp(hud.scale + delta * NoteHud.SCALE_STEP, NoteHud.MIN_SCALE, NoteHud.MAX_SCALE);
        push();
    }

    private void stepOpacity(int delta)
    {
        NoteHud hud = shelf().hud();
        hud.opacity = Mth.clamp(hud.opacity + delta * 0.1F, NoteHud.MIN_OPACITY, NoteHud.MAX_OPACITY);
        push();
    }

    private void cycleBackground(int delta)
    {
        NoteHud hud = shelf().hud();
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
            commitName(); // 切页前先把名字改动提交掉，免得输入框被销毁后丢失
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

    /**
     * 把当前书摊平成显示行（主任务 + 缩进的子任务）。
     *
     * <p><b>不做缓存</b>：任务是在同一个书对象里原地改的（勾选 / 加子任务 / 折叠都是），按对象身份
     * 缓存会拿到旧列表 —— 曾因此让折叠按钮看起来「完全没反应」。每帧重建一个小列表比缓存失效
     * 出 bug 便宜，界面本身也只在打开时跑。</p>
     */
    private List<RowRef> rows()
    {
        List<RowRef> list = new ArrayList<>();
        NoteBook book = book();
        List<NoteTask> tasks = book.tasks();
        for (int p = 0; p < tasks.size(); p++)
        {
            NoteTask task = tasks.get(p);
            list.add(new RowRef(task, p, -1));
            if (task.collapsed)
            {
                continue; // 折叠的子任务不进扁平行列表（点行首三角展开）
            }
            List<NoteTask> children = task.children();
            for (int c = 0; c < children.size(); c++)
            {
                list.add(new RowRef(children.get(c), p, c));
            }
        }
        return list;
    }

    private RowRef rowAt(int rowIndex)
    {
        List<RowRef> list = rows();
        return rowIndex < 0 || rowIndex >= list.size() ? null : list.get(rowIndex);
    }

    /** 行文字左缩进（子任务多缩一格，像论文的小标题） */
    private static final int CHILD_INDENT = 10;
    /** 行右侧「+ 子任务」与「× 删除」两个热区的宽度 */
    private static final int HOTSPOT_W = 14;

    private void drawTaskPage(GuiGraphics gui, int mouseX, int mouseY)
    {
        NoteBook book = book();
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.counter",
                        book.doneCount() + book.doneChildren(),
                        book.tasks().size() + book.childCount()),
                left + 10, top + 20, 0xFF9AA0A8, false);

        // ---- 便签册栏：◂ [名字 (i/n)] ▸ 新建 删除 ----
        Component bookLabel = Component.translatable("gui.godofthings.note.book_label",
                bookTitle(book), shelf().selected() + 1, shelf().size());
        gui.drawString(this.font, this.font.plainSubstrByWidth(bookLabel.getString(), 116),
                left + 28, top + BOOK_BAR_Y + 4, 0xFFE8C86A, false);

        gui.fill(left + 8, top + ROWS_TOP - 3, left + PANEL_W - 8, top + ROWS_BOTTOM, 0xFF11151A);

        List<RowRef> list = rows();
        int maxScroll = Math.max(0, list.size() - VISIBLE_ROWS);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        if (list.isEmpty())
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.note.empty"),
                    left + 14, top + ROWS_TOP + 2, 0xFF7E8794, false);
        }

        for (int i = 0; i < VISIBLE_ROWS; i++)
        {
            int index = scroll + i;
            if (index >= list.size())
            {
                break;
            }
            RowRef row = list.get(index);
            NoteTask task = row.task();
            boolean child = row.child() >= 0;
            int rowY = top + ROWS_TOP + i * ROW_H;
            boolean hovered = isRowHovered(mouseX, mouseY, rowY);
            boolean editing = inputMode == MODE_EDIT && editParent == row.parent()
                    && editChild == row.child();
            boolean dragging = index == dragRow;
            boolean deleteArmed = index == deleteArmedRow;

            int indent = child ? CHILD_INDENT : 0;
            if (dragging)
            {
                gui.fill(left + 9, rowY - 1, left + PANEL_W - 9, rowY + ROW_H - 2, 0x60E8C86A);
            }
            else if (hovered || editing || deleteArmed)
            {
                gui.fill(left + 9, rowY - 1, left + PANEL_W - 9, rowY + ROW_H - 2,
                        deleteArmed ? 0x50FF6060 : 0x30FFFFFF);
            }

            // 行首三角：有子任务的主任务可以折叠 / 展开（v 展开，> 折起）
            if (!child && task.hasChildren())
            {
                gui.drawString(this.font, task.collapsed ? ">" : "v", left + 1, rowY + 2,
                        hovered ? 0xFFE8C86A : 0xFF8A929C, false);
            }

            // 勾选框（子任务的框随缩进右移）
            drawCheckbox(gui, left + 12 + indent, rowY + 2, task.done);
            int textColor = task.done ? 0xFF9AA0A8 : (child ? 0xFFC9D0D9 : 0xFFF2F4F8);
            // 主任务带子任务时显示 (n/m) 进度
            String text = task.hasChildren() && !child
                    ? task.text + " (" + task.doneChildren() + "/" + task.childCount() + ")"
                    : task.text;
            int textX = left + 26 + indent;
            int textMax = left + PANEL_W - 40 - indent;
            text = this.font.plainSubstrByWidth(text, Math.max(20, textMax - textX));
            gui.drawString(this.font, text, textX, rowY + 2, textColor, false);
            if (task.done)
            {
                gui.fill(textX, rowY + 6, textX + this.font.width(text), rowY + 7, 0x80FFFFFF);
            }

            // 行尾两个热区：+ 子任务（仅主任务显示）与 × 删除
            int xX = left + PANEL_W - 10;
            gui.drawString(this.font, "x", xX, rowY + 2,
                    deleteArmed ? 0xFFFFD0D0 : 0xFFFF8080, false);
            if (!child)
            {
                gui.drawString(this.font, "+", xX - HOTSPOT_W, rowY + 2,
                        hovered ? 0xFF9FE89F : 0xFF7FA97F, false);
            }
        }

        if (maxScroll > 0)
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.note.scroll_hint",
                            scroll + 1, Math.min(list.size(), scroll + VISIBLE_ROWS), list.size()),
                    left + PANEL_W - 118, top + 20, 0xFF6F7883, false);
        }

        // 提示行（任务区与输入框之间，独立一行，不会再压进输入框）
        Component hint = null;
        if (dragRow >= 0)
        {
            hint = Component.translatable("gui.godofthings.note.drag_sort_hint");
        }
        else if (deleteArmedRow >= 0)
        {
            hint = Component.translatable("gui.godofthings.note.delete_hint");
        }
        else if (inputMode == MODE_SUB)
        {
            hint = Component.translatable("gui.godofthings.note.input_sub_hint",
                    parentTitle(subParent));
        }
        if (hint != null)
        {
            gui.drawString(this.font, this.font.plainSubstrByWidth(hint.getString(), PANEL_W - 20),
                    left + 10, top + HINT_Y, 0xFFE8C86A, false);
        }
    }

    /** 「给谁加子任务」提示里用的父任务名（截断防溢出） */
    private String parentTitle(int parentIndex)
    {
        if (parentIndex < 0 || parentIndex >= book().tasks().size())
        {
            return "?";
        }
        return this.font.plainSubstrByWidth(book().tasks().get(parentIndex).text, 120);
    }

    /** 便签标题：没起名就用默认名 */
    private Component bookTitle(NoteBook book)
    {
        return book.name().isEmpty()
                ? Component.translatable("gui.godofthings.note.title")
                : Component.literal(book.name());
    }

    private void drawHudPage(GuiGraphics gui)
    {
        NoteHud hud = shelf().hud();
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.hud_hint"),
                left + 10, top + 20, 0xFF9AA0A8, false);

        // ① 名称（输入框由控件自己画）
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.name"),
                left + 10, top + 35, 0xFFF2F4F8, false);

        // ② 自动更名
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.auto_name"),
                left + 10, top + 59, 0xFFF2F4F8, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.auto_name_hint"),
                left + 148, top + 59, 0xFF7E8794, false);

        // ③ 悬浮窗
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.hud_label"),
                left + 10, top + 83, 0xFFF2F4F8, false);

        // ④ 位置 X / Y
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.pos_x"),
                left + 10, top + 107, 0xFFF2F4F8, false);
        gui.drawString(this.font, percent(hud.x), left + 112, top + 107, 0xFFE8C86A, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.pos_y"),
                left + 148, top + 107, 0xFFF2F4F8, false);
        gui.drawString(this.font, percent(hud.y), left + 250, top + 107, 0xFFE8C86A, false);

        // ⑤ 大小 / 透明度
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.scale"),
                left + 10, top + 131, 0xFFF2F4F8, false);
        gui.drawString(this.font, String.format("%.2fx", hud.scale),
                left + 98, top + 131, 0xFFE8C86A, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.opacity"),
                left + 150, top + 131, 0xFFF2F4F8, false);
        gui.drawString(this.font, percent(hud.opacity), left + 242, top + 131, 0xFFE8C86A, false);

        // ⑥ 背景 / 已完成条目
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.background"),
                left + 10, top + 155, 0xFFF2F4F8, false);
        gui.drawString(this.font, Component.translatable(NoteHud.backgroundKey(hud.background)),
                left + 98, top + 155, 0xFFE8C86A, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.note.show_done_label"),
                left + 160, top + 155, 0xFFF2F4F8, false);
    }

    /** 0~1 的比例显示成百分比整数 */
    private static String percent(float value)
    {
        return Math.round(value * 100.0F) + "%";
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
        return mouseX >= left + 1 && mouseX <= left + PANEL_W - 9
                && mouseY >= rowY - 1 && mouseY < rowY + ROW_H - 2;
    }

    /** 由屏幕 Y 反推「扁平行」下标（含滚动偏移）；点空白处返回 -1 */
    private int rowIndexAt(int mouseY)
    {
        int row = (mouseY - (top + ROWS_TOP)) / ROW_H;
        if (mouseY < top + ROWS_TOP - 1 || mouseY >= top + ROWS_BOTTOM)
        {
            return -1;
        }
        return scroll + Mth.clamp(row, 0, VISIBLE_ROWS - 1);
    }

    // ------------------------------------------------------------------ 交互

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (page == 0 && button == 0)
        {
            List<RowRef> list = rows();
            for (int i = 0; i < VISIBLE_ROWS; i++)
            {
                int rowY = top + ROWS_TOP + i * ROW_H;
                if (!isRowHovered((int) mouseX, (int) mouseY, rowY))
                {
                    continue;
                }
                int index = scroll + i;
                if (index >= list.size())
                {
                    break;
                }
                RowRef row = list.get(index);
                boolean child = row.child() >= 0;
                int indent = child ? CHILD_INDENT : 0;
                bookDeleteArmed = false;
                if (!child && row.task().hasChildren() && mouseX >= left + 1 && mouseX <= left + 10)
                {
                    // 行首三角：折叠 / 展开子任务
                    deleteArmedRow = -1;
                    book().toggleCollapsed(row.parent());
                    push();
                }
                else if (mouseX <= left + 24 + indent)
                {
                    // 勾选框
                    if (child)
                    {
                        book().toggleChild(row.parent(), row.child());
                    }
                    else
                    {
                        book().toggle(row.parent());
                    }
                    push();
                    deleteArmedRow = -1;
                }
                else if (mouseX >= left + PANEL_W - 22)
                {
                    // × 删除：所有行都点两次确认（防误触），主任务会连子任务一起删
                    if (deleteArmedRow != index)
                    {
                        deleteArmedRow = index;
                        return true;
                    }
                    deleteArmedRow = -1;
                    if (child)
                    {
                        book().removeChild(row.parent(), row.child());
                    }
                    else
                    {
                        book().remove(row.parent());
                    }
                    if (inputMode != MODE_ADD)
                    {
                        resetInput();
                    }
                    push();
                    refresh();
                }
                else if (!child && mouseX >= left + PANEL_W - 22 - HOTSPOT_W)
                {
                    // + 子任务
                    deleteArmedRow = -1;
                    startSubTask(row.parent());
                }
                else
                {
                    // 记录拖拽起点：松手时若没移动过就当成「点文字改名」
                    deleteArmedRow = -1;
                    dragRow = index;
                    dragMoved = false;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (page == 0 && dragRow >= 0 && button == 0)
        {
            int target = rowIndexAt((int) mouseY);
            if (target >= 0 && target != dragRow)
            {
                RowRef from = rowAt(dragRow);
                RowRef to = rowAt(target);
                if (from != null && to != null)
                {
                    // 只允许同级重排：主任务在主任务之间、子任务在同一个父任务的子任务之间
                    if (from.child() >= 0 && to.child() >= 0 && from.parent() == to.parent())
                    {
                        book().moveChild(from.parent(), from.child(), to.child());
                        dragRow = target;
                        dragMoved = true;
                    }
                    else if (from.child() < 0 && to.child() < 0)
                    {
                        book().moveTask(from.parent(), to.parent());
                        dragRow = target;
                        dragMoved = true;
                    }
                }
                // 拖到列表边缘时自动滚动
                int size = rows().size();
                if (mouseY >= top + ROWS_BOTTOM - ROW_H && scroll < Math.max(0, size - VISIBLE_ROWS))
                {
                    scroll++;
                }
                else if (mouseY <= top + ROWS_TOP + ROW_H && scroll > 0)
                {
                    scroll--;
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (page == 0 && dragRow >= 0)
        {
            int index = dragRow;
            dragRow = -1;
            if (dragMoved)
            {
                push();
            }
            else
            {
                startEditing(index);
            }
            dragMoved = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
        boolean enter = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
        if (input != null && input.isFocused() && enter)
        {
            submitInput();
            return true;
        }
        if (input != null && input.isFocused() && keyCode == GLFW.GLFW_KEY_TAB)
        {
            // Tab / Shift+Tab：编辑中的任务降级 / 升级（像写大纲那样）
            handleIndentKeys(keyCode, modifiers);
            return true;
        }
        if (nameField != null && nameField.isFocused() && enter)
        {
            // 回车提交名字并取消聚焦（悬浮窗标题立刻就会变）
            commitName();
            nameField.setFocused(false);
            refresh();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && (inputMode != MODE_ADD || deleteArmedRow >= 0))
        {
            // Esc 只取消改名 / 子任务模式 / 删除确认，不关界面
            deleteArmedRow = -1;
            resetInput();
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
