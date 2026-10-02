package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.note.NoteBook;
import com.godofthings.note.NoteHud;
import com.godofthings.note.NoteTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之便签的悬浮窗（HUD 层）：把便签内容常驻画在屏幕上。
 *
 * <h3>要点</h3>
 * <ul>
 *   <li>注册为独立 GUI 层（{@link VanillaGuiLayers#EXPERIENCE_LEVEL} 之上、聊天栏之下），
 *       不做任何 Mixin，与其他 HUD mod 零冲突</li>
 *   <li>位置：存的是一屏的<b>比例</b>，绘制时换算成像素并夹到屏幕内；大小是一套
 *       {@code pose().scale(...)} 缩放，文字随之放大（与「编辑悬浮窗」界面共用同一套算法）</li>
 *   <li>打开任何界面（{@code mc.screen != null}）时不画 —— 避免盖住 GUI；
 *       编辑模式改由 {@code GodNoteHudEditScreen} 自己调用 {@link #render} 画一份</li>
 *   <li>背景 5 档：无 / 半透明黑 / 羊皮纸 / 深空蓝 / 自定义（自定义色 + 透明度）</li>
 * </ul>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GodNoteHud
{
    private GodNoteHud() {}

    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "god_note");

    // ---- 版面常量（均为「未缩放」像素） ----
    private static final int PAD = 5;
    private static final int CHECK = 8;
    private static final int CHECK_GAP = 4;
    private static final int LINE_H = 10;
    private static final int HEADER_H = 12;
    private static final int MAX_TEXT_W = 132;
    private static final int MAX_LINES_PER_TASK = 6;
    private static final int MAX_ROWS = 24;
    private static final int MIN_PANEL_W = 78;
    private static final int MAX_PANEL_W = 200;

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, LAYER_ID,
                (guiGraphics, deltaTracker) -> renderLayer(guiGraphics));
    }

    private static void renderLayer(GuiGraphics gui)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null)
        {
            return; // 开界面 / 未进世界时不画
        }
        NoteBook book = ClientNoteCache.book();
        if (!book.hud().enabled)
        {
            return;
        }
        render(gui, mc.font, book, gui.guiWidth(), gui.guiHeight(), false);
    }

    /** 一行版面（一条任务可能折成多行，只有首行带勾选框） */
    private record Row(String text, boolean done, boolean checkbox) {}

    /** 一次排版的结果：行 + 未缩放面板尺寸 */
    private record Layout(List<Row> rows, int width, int height) {}

    private static Layout layout(Font font, NoteBook book)
    {
        List<Row> rows = new ArrayList<>();
        for (NoteTask task : book.tasks())
        {
            if (task.done && !book.hud().showDone)
            {
                continue;
            }
            List<String> wrapped = wrap(font, task.text, MAX_TEXT_W);
            for (int i = 0; i < wrapped.size() && rows.size() < MAX_ROWS; i++)
            {
                rows.add(new Row(wrapped.get(i), task.done, i == 0));
            }
            if (rows.size() >= MAX_ROWS)
            {
                break;
            }
        }

        int textW = font.width(header(book));
        for (Row r : rows)
        {
            textW = Math.max(textW, CHECK + CHECK_GAP + font.width(r.text()));
        }
        int width = Mth.clamp(textW + PAD * 2, MIN_PANEL_W, MAX_PANEL_W);
        int height = PAD * 2 + HEADER_H + Math.max(1, rows.size()) * LINE_H;
        return new Layout(rows, width, height);
    }

    /**
     * 便签在屏幕上占据的矩形：{@code {x, y, 缩放后宽, 缩放后高}}（单位 = GUI 像素）。
     * 编辑界面用它做命中判定与拖动换算。
     */
    public static int[] rect(Font font, NoteBook book, int screenW, int screenH)
    {
        Layout l = layout(font, book);
        float scale = Mth.clamp(book.hud().scale, NoteHud.MIN_SCALE, NoteHud.MAX_SCALE);
        int w = Math.round(l.width() * scale);
        int h = Math.round(l.height() * scale);
        int x = clampPos(Math.round(book.hud().x * screenW), w, screenW);
        int y = clampPos(Math.round(book.hud().y * screenH), h, screenH);
        return new int[] { x, y, w, h };
    }

    /** 把左上角夹到屏幕内（便签整体可见） */
    public static int clampPos(int pos, int size, int screenSize)
    {
        return Mth.clamp(pos, 0, Math.max(0, screenSize - size));
    }

    /** 把像素位置换算回 0~1 的比例并存回设置（拖动结束时用） */
    public static void storePosition(NoteHud hud, int px, int py, int screenW, int screenH)
    {
        hud.x = screenW <= 0 ? 0.0F : (float) px / screenW;
        hud.y = screenH <= 0 ? 0.0F : (float) py / screenH;
        hud.clamp();
    }

    /** 画一份便签；{@code editing} = true 时加一圈高亮虚框（编辑模式提示可拖动） */
    public static void render(GuiGraphics gui, Font font, NoteBook book, int screenW, int screenH,
                              boolean editing)
    {
        NoteHud hud = book.hud();
        Layout l = layout(font, book);
        float scale = Mth.clamp(hud.scale, NoteHud.MIN_SCALE, NoteHud.MAX_SCALE);
        int w = l.width();
        int h = l.height();
        int x = clampPos(Math.round(hud.x * screenW), Math.round(w * scale), screenW);
        int y = clampPos(Math.round(hud.y * screenH), Math.round(h * scale), screenH);

        boolean parchment = hud.background == NoteHud.BG_PARCHMENT;
        int textColor = parchment ? 0xFF3A2B12 : 0xFFF2F4F8;
        int doneColor = parchment ? 0xFF8A7A5A : 0xFF9AA0A8;
        int headerColor = parchment ? 0xFF6B4E1E : 0xFFE8C86A;
        int borderColor = parchment ? 0xFF8A7442 : 0x66FFFFFF;
        int boxBorder = parchment ? 0xFF7A6535 : 0xFFBFC6D0;
        int boxFill = parchment ? 0x22000000 : 0x40000000;

        gui.pose().pushPose();
        gui.pose().translate(x, y, 0.0F);
        gui.pose().scale(scale, scale, 1.0F);

        int bg = backgroundColor(hud);
        if ((bg >>> 24) != 0)
        {
            gui.fill(0, 0, w, h, bg);
        }
        if (editing || (bg >>> 24) != 0)
        {
            // 一圈 1px 边框（编辑模式下用金色示意「可以拖」）
            int frame = editing ? 0xFFFFD700 : borderColor;
            gui.fill(0, 0, w, 1, frame);
            gui.fill(0, h - 1, w, h, frame);
            gui.fill(0, 0, 1, h, frame);
            gui.fill(w - 1, 0, w, h, frame);
        }

        gui.drawString(font, header(book), PAD, PAD + 1, headerColor, true);

        int rowY = PAD + HEADER_H;
        if (l.rows().isEmpty())
        {
            gui.drawString(font, Component.translatable("gui.godofthings.note.empty").getString(),
                    PAD + CHECK + CHECK_GAP, rowY, doneColor, true);
        }
        for (Row row : l.rows())
        {
            int textX = PAD + CHECK + CHECK_GAP;
            if (row.checkbox())
            {
                drawCheckbox(gui, PAD, rowY + 1, row.done(), boxBorder, boxFill);
            }
            gui.drawString(font, row.text(), textX, rowY, row.done() ? doneColor : textColor, true);
            if (row.done() && !row.text().isEmpty())
            {
                gui.fill(textX, rowY + 4, textX + font.width(row.text()), rowY + 5,
                        parchment ? 0x808A7A5A : 0x80FFFFFF);
            }
            rowY += LINE_H;
        }

        gui.pose().popPose();
    }

    /** 悬浮窗标题：自定义名字（没设名字就用默认的「神之便签」）+ 已完成 / 总数 */
    private static Component header(NoteBook book)
    {
        Component name = book.name().isEmpty()
                ? Component.translatable("gui.godofthings.note.title")
                : Component.literal(book.name());
        return Component.translatable("gui.godofthings.note.hud_title",
                name, book.doneCount(), book.tasks().size());
    }

    /** 背景色（含透明度）；返回 0 表示完全不画背景 */
    public static int backgroundColor(NoteHud hud)
    {
        int alpha = Mth.clamp(Math.round(hud.opacity * 255.0F), 26, 255) << 24;
        return switch (hud.background)
        {
            case NoteHud.BG_NONE -> 0;
            case NoteHud.BG_PARCHMENT -> 0xF0F3E3B3;
            case NoteHud.BG_SPACE -> alpha | 0x0A1430;
            case NoteHud.BG_CUSTOM -> alpha | (hud.color & 0xFFFFFF);
            default -> alpha | 0x101418;
        };
    }

    private static void drawCheckbox(GuiGraphics gui, int x, int y, boolean done, int border, int fill)
    {
        gui.fill(x, y, x + CHECK, y + CHECK, border);
        gui.fill(x + 1, y + 1, x + CHECK - 1, y + CHECK - 1, done ? 0xFF2E8B3D : fill);
        if (done)
        {
            // 用 4 个小方块拼一个「✓」（不依赖字体里有没有 U+2713）
            gui.fill(x + 2, y + 4, x + 3, y + 5, 0xFFFFFFFF);
            gui.fill(x + 3, y + 5, x + 4, y + 6, 0xFFFFFFFF);
            gui.fill(x + 4, y + 3, x + 5, y + 4, 0xFFFFFFFF);
            gui.fill(x + 5, y + 2, x + 6, y + 3, 0xFFFFFFFF);
        }
    }

    /**
     * 按像素宽度折行；优先在空格处断（英文），没有合适空格就硬断（中文）。
     * 一条任务最多 {@link #MAX_LINES_PER_TASK} 行，超出部分丢弃（面板不会长到失控）。
     */
    private static List<String> wrap(Font font, String text, int maxWidth)
    {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty())
        {
            out.add("");
            return out;
        }
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (cur.length() > 0 && font.width(cur.toString() + c) > maxWidth)
            {
                if (out.size() >= MAX_LINES_PER_TASK - 1)
                {
                    break;
                }
                String line = cur.toString();
                int space = line.lastIndexOf(' ');
                if (space > 0 && line.length() - space <= 12)
                {
                    out.add(line.substring(0, space));
                    cur = new StringBuilder(line.substring(space + 1));
                }
                else
                {
                    out.add(line);
                    cur = new StringBuilder();
                }
            }
            cur.append(c);
        }
        if (cur.length() > 0 && out.size() < MAX_LINES_PER_TASK)
        {
            out.add(cur.toString());
        }
        if (out.isEmpty())
        {
            out.add("");
        }
        return out;
    }
}
