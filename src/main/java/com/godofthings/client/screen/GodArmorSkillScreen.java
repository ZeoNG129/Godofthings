package com.godofthings.client.screen;

import com.godofthings.armor.GodArmorFeatures;
import com.godofthings.armor.GodArmorState;
import com.godofthings.armor.skill.ArmorSkillCategory;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkillDef;
import com.godofthings.armor.skill.ArmorSkills;
import com.godofthings.config.ClientConfig;
import com.godofthings.network.ArmorMessages;
import com.godofthings.network.ArmorSkillMessages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 神之套装 · 配置界面（技能树 + 套装功能，合并为一个界面）。
 * <p>
 * 视觉与交互参照 <b>Zifeng Skill Tree（子枫的百宝箱）</b> 的技能树界面
 * （Copyright (c) 2026 zifeng, MIT License）：圆角贴片 + 左侧类别色条 + 等级进度条 +
 * 分类标签行 + 可滚动列表与滚动条；配色沿用其按分类区分的深色贴片方案。
 * <p>
 * 与原版差异（按需求）：无技能点、无前置，左键点击即解锁。
 * <ul>
 *   <li><b>左键点技能行</b>：开 / 关（开启即 1 级）</li>
 *   <li><b>右键点技能行</b>：等级 +1（Shift + 右键 +10）；<b>到上限即停，不再回绕</b></li>
 *   <li><b>按住左键在进度条上拖动</b>：按拖动位置直接设定等级（原版同款）</li>
 *   <li><b>滚轮悬停进度条</b>：等级 ±1；滚轮在别处：滚动列表</li>
 *   <li>标签行第 3 项「套装功能」= 原 O 键的 12 项开关（已合并进来）</li>
 * </ul>
 */
public class GodArmorSkillScreen extends Screen
{
    // ---- 圆角与框架 ----
    private static final int R_BIG = 12;
    private static final int R_SUB = 8;
    private static final int R_ROW = 6;
    private static final int FRAME_MARGIN = 8;

    // ---- 尺寸 ----
    private static final int MAX_PANEL_W = 486;
    private static final int MAX_PANEL_H = 336;
    private static final int TITLE_H = 22;
    private static final int CAT_H = 24;
    private static final int BOTTOM_H = 26;
    private static final int ROW_H = 28;
    private static final int ROW_GAP = 3;
    private static final int ROW_STEP = ROW_H + ROW_GAP;
    private static final int BAR_W = 78;
    private static final int BAR_H = 6;

    // ---- 配色（沿用参考模组按分类的深色贴片方案）----
    private static final int C_FRAME_BG = 0xF01A1C22;
    private static final int C_FRAME_EDGE = 0xFF3A3F4A;
    private static final int C_SUB_BG = 0xFF23262E;
    private static final int C_SUB_EDGE = 0xFF343945;
    private static final int C_TITLE = 0xFFF0E6D2;
    private static final int C_TITLE_ACCENT = 0xFFC8A03A;
    private static final int C_TEXT = 0xFFE6E8EE;
    private static final int C_TEXT_DIM = 0xFF9AA0AB;
    private static final int C_TEXT_OFF = 0xFF7C828C;
    private static final int C_LV = 0xFF55FF55;
    private static final int C_LV_OFF = 0xFF7C828C;
    private static final int C_BAR_TRACK = 0x59FFFFFF;
    private static final int C_BAR_TICK = 0xFFFFDD44;
    private static final int C_CELL_LINE = 0x22FFFFFF;
    private static final int C_BTN = 0xFF2E333C;
    private static final int C_BTN_HOVER = 0xFF3E444F;
    private static final int C_BTN_EDGE = 0xFF454B57;
    private static final int C_HIGHLIGHT = 0x18FFFFFF;

    private static final int C_ACCENT_BASE = 0xFF87CEEB;
    private static final int C_ACCENT_AMPLIFY = 0xFFFFAA55;
    private static final int C_ACCENT_FEATURE = 0xFF7FD48A;

    private static final int[][] CAT_BG = {
            { 0xFF203A55, 0xFF2B4B6D }, // BASE 基础属性
            { 0xFF503A27, 0xFF674A30 }, // AMPLIFY 特殊增幅
            { 0xFF512B34, 0xFF693641 }, // ULTIMATE 终极节点
            { 0xFF4C3B27, 0xFF625035 }, // SPECIAL 特殊被动
            { 0xFF2B3A2E, 0xFF3A4E3E }, // 套装功能
    };

    /** 标签页顺序：前 4 个技能分类 + 最后套装功能 */
    private static final ArmorSkillCategory[] TAB_CATEGORIES = {
            ArmorSkillCategory.BASE, ArmorSkillCategory.AMPLIFY,
            ArmorSkillCategory.ULTIMATE, ArmorSkillCategory.SPECIAL };

    private static final int[] TAB_ACCENT = {
            C_ACCENT_BASE, C_ACCENT_AMPLIFY, 0xFFFF6B6B, 0xFFFFD166, C_ACCENT_FEATURE };

    /** 标签页索引（O 键 → 套装功能页；K 键 → 基础属性页） */
    public static final int TAB_BASE = 0;
    public static final int TAB_AMPLIFY = 1;
    public static final int TAB_ULTIMATE = 2;
    public static final int TAB_SPECIAL = 3;
    public static final int TAB_FEATURE = 4;
    /** 标签页总数（前 4 个是技能分类，最后一个是套装功能） */
    private static final int TAB_COUNT = 5;

    /** 标签页：0 = 基础属性，1 = 特殊增幅，2 = 套装功能 */
    private int tab;
    private int scroll;
    private boolean draggingBar;
    private String draggingSkill = "";
    private boolean draggingScrollbar;
    private int barDragRow = -1;

    private int px0;
    private int py0;
    private int panelW;
    private int panelH;
    private int listX0;
    private int listY0;
    private int listW;
    private int listH;
    private int contentH;
    private int maxScroll;

    public GodArmorSkillScreen()
    {
        // 默认回到上次离开的那一页（用户要求：不要每次都跳回第一页）
        this(ClientConfig.getLastTab());
    }

    /** @param initialTab 0=基础属性 1=特殊增幅 2=套装功能（O 键直接进套装功能页） */
    public GodArmorSkillScreen(int initialTab)
    {
        super(Component.translatable("gui.godofthings.armor.skill.title"));
        this.tab = Math.max(0, Math.min(TAB_COUNT - 1, initialTab));
    }

    // ══════════════════ 布局 ══════════════════

    private void computeLayout()
    {
        this.panelW = Math.min(MAX_PANEL_W, this.width - FRAME_MARGIN * 2);
        this.panelH = Math.min(MAX_PANEL_H, this.height - FRAME_MARGIN * 2);
        this.px0 = (this.width - this.panelW) / 2;
        this.py0 = (this.height - this.panelH) / 2;

        this.listX0 = px0 + 8;
        this.listY0 = py0 + TITLE_H + CAT_H + 6;
        this.listW = panelW - 16;
        int listBottom = py0 + panelH - BOTTOM_H - 6;
        this.listH = Math.max(ROW_H + 2, listBottom - listY0);

        this.contentH = rowCount() * ROW_STEP;
        this.maxScroll = Math.max(0, contentH - listH);
        this.scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    private int rowCount()
    {
        return tab == TAB_FEATURE ? GodArmorFeatures.COUNT : ArmorSkills.of(categoryOfTab()).size();
    }

    private ArmorSkillCategory categoryOfTab()
    {
        return tab >= 0 && tab < TAB_CATEGORIES.length ? TAB_CATEGORIES[tab] : ArmorSkillCategory.BASE;
    }

    // ══════════════════ 渲染 ══════════════════

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        computeLayout();

        // 大框架：圆角底 + 描边
        fillRound(gui, px0, py0, px0 + panelW, py0 + panelH, R_BIG, C_FRAME_EDGE);
        fillRound(gui, px0 + 1, py0 + 1, px0 + panelW - 1, py0 + panelH - 1, R_BIG - 1, C_FRAME_BG);

        drawTitle(gui);
        drawTabs(gui, mouseX, mouseY);
        drawList(gui, mouseX, mouseY);
        drawScrollbar(gui, mouseX, mouseY);
        drawBottom(gui, mouseX, mouseY);

        ArmorSkillDef hoveredSkill = (tab == TAB_FEATURE) ? null : skillAt(mouseX, mouseY);
        if (hoveredSkill != null)
        {
            renderSkillTooltip(gui, hoveredSkill, mouseX, mouseY);
        }
        else
        {
            int hoveredFeature = (tab == TAB_FEATURE) ? featureAt(mouseX, mouseY) : -1;
            if (hoveredFeature >= 0)
            {
                gui.renderTooltip(this.font, List.of(
                        Component.translatable(GodArmorFeatures.LANG_KEYS[hoveredFeature])
                                .withStyle(net.minecraft.ChatFormatting.GOLD),
                        Component.translatable("gui.godofthings.armor.skill.hint.feature")
                                .withStyle(net.minecraft.ChatFormatting.GRAY)),
                        Optional.empty(), mouseX, mouseY);
            }
        }
    }

    private void drawTitle(GuiGraphics gui)
    {
        fillRound(gui, px0 + 4, py0 + 4, px0 + panelW - 4, py0 + TITLE_H, R_SUB, C_SUB_BG);
        gui.drawString(this.font, Component.translatable("gui.godofthings.armor.skill.title"),
                px0 + 12, py0 + 8, C_TITLE, false);
        // 右侧显示当前是否穿齐全套（技能生效条件）
        // 实时读玩家背包（每帧查询，穿脱护甲立刻反映；不再错用"功能开关位图"）
        net.minecraft.client.player.LocalPlayer me = net.minecraft.client.Minecraft.getInstance().player;
        boolean worn = me != null && com.godofthings.handler.GodArmorHandler.isFullSetWorn(me);
        Component state = Component.translatable(worn
                ? "gui.godofthings.armor.skill.worn"
                : "gui.godofthings.armor.skill.not_worn");
        int color = worn ? 0xFF7FD48A : 0xFFD48A7F;
        gui.drawString(this.font, state, px0 + panelW - 12 - this.font.width(state), py0 + 8, color, false);
    }

    private void drawTabs(GuiGraphics gui, int mouseX, int mouseY)
    {
        int y = py0 + TITLE_H + 2;
        int tabW = (listW - (TAB_COUNT - 1) * 4) / TAB_COUNT;
        Component[] labels = new Component[TAB_COUNT];
        for (int i = 0; i < TAB_CATEGORIES.length; i++)
        {
            labels[i] = Component.translatable(TAB_CATEGORIES[i].getLangKey());
        }
        labels[TAB_FEATURE] = Component.translatable("gui.godofthings.armor.skill.tab.feature");
        for (int i = 0; i < TAB_COUNT; i++)
        {
            int x = listX0 + i * (tabW + 4);
            boolean selected = tab == i;
            boolean hovered = mouseX >= x && mouseX < x + tabW && mouseY >= y && mouseY < y + 18;
            int accent = TAB_ACCENT[i];
            int bg = selected ? (accent & 0x00FFFFFF) | 0x55000000 : (hovered ? C_BTN_HOVER : C_SUB_BG);
            fillRound(gui, x, y, x + tabW, y + 18, R_SUB - 2, C_SUB_EDGE);
            fillRound(gui, x + 1, y + 1, x + tabW - 1, y + 17, R_SUB - 3, bg);
            int tx = x + (tabW - this.font.width(labels[i])) / 2;
            gui.drawString(this.font, labels[i], tx, y + 5, selected ? accent : C_TEXT_DIM, false);
        }
    }

    private void drawList(GuiGraphics gui, int mouseX, int mouseY)
    {
        fillRound(gui, listX0 - 2, listY0 - 2, listX0 + listW + 2, listY0 + listH + 2, R_SUB, C_SUB_EDGE);
        fillRound(gui, listX0 - 1, listY0 - 1, listX0 + listW + 1, listY0 + listH + 1, R_SUB - 1, 0xFF1C1F26);

        gui.enableScissor(listX0, listY0, listX0 + listW, listY0 + listH);
        int n = rowCount();
        for (int i = 0; i < n; i++)
        {
            int ry = listY0 + i * ROW_STEP - scroll;
            if (ry + ROW_H < listY0 || ry > listY0 + listH)
            {
                continue; // 视口外不画
            }
            boolean hovered = mouseX >= listX0 && mouseX < listX0 + listW
                    && mouseY >= ry && mouseY < ry + ROW_H
                    && mouseY >= listY0 && mouseY < listY0 + listH;
            if (tab == TAB_FEATURE)
            {
                drawFeatureRow(gui, i, ry, hovered);
            }
            else
            {
                drawSkillRow(gui, ArmorSkills.of(categoryOfTab()).get(i), ry, hovered);
            }
        }
        gui.disableScissor();
    }

    private void drawSkillRow(GuiGraphics gui, ArmorSkillDef def, int ry, boolean hovered)
    {
        int lv = ArmorSkillData.clientLevel(def.id());
        boolean on = ArmorSkillData.clientEnabled(def.id());
        int[] pal = CAT_BG[Math.max(0, Math.min(CAT_BG.length - 1, tab))];
        int accent = TAB_ACCENT[Math.max(0, Math.min(TAB_ACCENT.length - 1, tab))];

        int rowW = listW - 6;
        fillRound(gui, listX0 + 3, ry, listX0 + 3 + rowW, ry + ROW_H, R_ROW, hovered ? pal[1] : pal[0]);
        // 顶部极淡高光（参考模组的贴片层次感）
        gui.fill(listX0 + 3 + R_ROW, ry + 1, listX0 + 3 + rowW - R_ROW, ry + 2, C_HIGHLIGHT);
        // 左侧类别色条：未开启时压暗
        int stripe = on ? accent : (accent & 0x00FFFFFF) | 0x66000000;
        fillRound(gui, listX0 + 4, ry + 2, listX0 + 7, ry + ROW_H - 2, 2, stripe);

        // 第一行：技能名 + 右侧等级
        gui.drawString(this.font, Component.translatable(def.nameKey()),
                listX0 + 12, ry + 4, on ? C_TEXT : C_TEXT_OFF, false);
        // 关闭时把记住的等级也显示出来，让「关掉不会丢等级」一目了然
        Component lvText;
        if (on)
        {
            lvText = Component.translatable("gui.godofthings.armor.skill.lv_max", lv, def.maxLevel());
        }
        else if (lv > 0)
        {
            lvText = Component.translatable("gui.godofthings.armor.skill.off_lv", lv);
        }
        else
        {
            lvText = Component.translatable("gui.godofthings.armor.skill.off");
        }
        gui.drawString(this.font, lvText, listX0 + 3 + rowW - 8 - this.font.width(lvText), ry + 4,
                on ? C_LV : C_LV_OFF, false);

        // 第二行：效果说明（灰色）+ 右侧进度条
        gui.drawString(this.font, Component.translatable(def.descKey()),
                listX0 + 12, ry + 16, C_TEXT_DIM, false);
        int bx = listX0 + 3 + rowW - 8 - BAR_W;
        drawLevelBar(gui, bx, ry + 17, BAR_W, def.maxLevel() > 0 ? (float) lv / def.maxLevel() : 0f, on, accent);
    }

    private void drawFeatureRow(GuiGraphics gui, int feature, int ry, boolean hovered)
    {
        int mask = GodArmorState.getClientMask();
        boolean on = GodArmorFeatures.isOn(mask, feature);
        int rowW = listW - 6;
        fillRound(gui, listX0 + 3, ry, listX0 + 3 + rowW, ry + ROW_H, R_ROW,
                hovered ? CAT_BG[2][1] : CAT_BG[2][0]);
        gui.fill(listX0 + 3 + R_ROW, ry + 1, listX0 + 3 + rowW - R_ROW, ry + 2, C_HIGHLIGHT);
        fillRound(gui, listX0 + 4, ry + 2, listX0 + 7, ry + ROW_H - 2, 2,
                on ? C_ACCENT_FEATURE : (C_ACCENT_FEATURE & 0x00FFFFFF) | 0x66000000);

        gui.drawString(this.font, Component.translatable(GodArmorFeatures.LANG_KEYS[feature]),
                listX0 + 12, ry + 9, on ? C_TEXT : C_TEXT_OFF, false);
        Component state = Component.translatable(on
                ? "gui.godofthings.armor.skill.on"
                : "gui.godofthings.armor.skill.off2");
        gui.drawString(this.font, state, listX0 + 3 + rowW - 8 - this.font.width(state), ry + 9,
                on ? C_LV : C_LV_OFF, false);
    }

    /** 等级进度条：双轨底 + 填充 + 黄色手柄（与参考模组一致）。 */
    private void drawLevelBar(GuiGraphics gui, int x, int y, int w, float ratio, boolean on, int accent)
    {
        fillRound(gui, x, y, x + w, y + BAR_H, BAR_H / 2, 0x42000000);
        fillRound(gui, x + 1, y + 1, x + w - 1, y + BAR_H - 1, Math.max(1, BAR_H / 2 - 1), C_BAR_TRACK);
        float r = Math.max(0f, Math.min(1f, ratio));
        if (r > 0f)
        {
            int fw = Math.max(BAR_H, Math.round(w * r));
            fillRound(gui, x, y, x + Math.min(w, fw), y + BAR_H, BAR_H / 2, on ? accent : C_TEXT_OFF);
        }
        int tx = x + Math.round(w * r);
        fillRound(gui, tx - 2, y - 2, tx + 3, y + BAR_H + 2, 2, 0xCC202028);
        fillRound(gui, tx - 1, y - 1, tx + 2, y + BAR_H + 1, 1, C_BAR_TICK);
    }

    private void drawScrollbar(GuiGraphics gui, int mouseX, int mouseY)
    {
        if (maxScroll <= 0)
        {
            return;
        }
        int sx = listX0 + listW - 3;
        int trackH = listH;
        int thumbH = Math.max(16, trackH * listH / Math.max(1, contentH));
        int thumbY = listY0 + (trackH - thumbH) * scroll / Math.max(1, maxScroll);
        fillRound(gui, sx, listY0, sx + 3, listY0 + trackH, 1, 0x30FFFFFF);
        boolean hovered = mouseX >= sx - 3 && mouseX < sx + 6 && mouseY >= thumbY && mouseY < thumbY + thumbH;
        fillRound(gui, sx, thumbY, sx + 3, thumbY + thumbH, 1, hovered ? 0xCCFFFFFF : 0x88FFFFFF);
    }

    private void drawBottom(GuiGraphics gui, int mouseX, int mouseY)
    {
        int y = py0 + panelH - BOTTOM_H + 2;
        int bw = (listW - 12) / 4;
        Component[] labels = {
                Component.translatable("gui.godofthings.armor.skill.all_on"),
                Component.translatable("gui.godofthings.armor.skill.all_off"),
                Component.translatable("gui.godofthings.armor.skill.reset"),
                Component.translatable("gui.godofthings.back"),
        };
        for (int i = 0; i < 4; i++)
        {
            int x = listX0 + i * (bw + 4);
            boolean hovered = inRect(mouseX, mouseY, x, y, bw, 20);
            fillRound(gui, x, y, x + bw, y + 20, R_SUB - 2, C_BTN_EDGE);
            fillRound(gui, x + 1, y + 1, x + bw - 1, y + 19, R_SUB - 3, hovered ? C_BTN_HOVER : C_BTN);
            gui.drawString(this.font, labels[i], x + (bw - this.font.width(labels[i])) / 2, y + 6, C_TEXT, false);
        }
    }

    private void renderSkillTooltip(GuiGraphics gui, ArmorSkillDef def, int mouseX, int mouseY)
    {
        int lv = ArmorSkillData.clientLevel(def.id());
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(def.nameKey()).withStyle(net.minecraft.ChatFormatting.GOLD));
        lines.add(Component.translatable(def.descKey()).withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.godofthings.armor.skill.level", lv, def.maxLevel())
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.translatable("gui.godofthings.armor.skill.hint.node")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        gui.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
    }

    // ══════════════════ 命中测试与交互 ══════════════════

    private static boolean inRect(int mx, int my, int x, int y, int w, int h)
    {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** 行下标（-1 = 不在列表内） */
    private int rowIndexAt(int mouseX, int mouseY)
    {
        if (mouseX < listX0 || mouseX >= listX0 + listW || mouseY < listY0 || mouseY >= listY0 + listH)
        {
            return -1;
        }
        int idx = (mouseY - listY0 + scroll) / ROW_STEP;
        return (idx >= 0 && idx < rowCount()) ? idx : -1;
    }

    private ArmorSkillDef skillAt(int mouseX, int mouseY)
    {
        int idx = rowIndexAt(mouseX, mouseY);
        return idx < 0 || tab == TAB_FEATURE ? null : ArmorSkills.of(categoryOfTab()).get(idx);
    }

    private int featureAt(int mouseX, int mouseY)
    {
        int idx = rowIndexAt(mouseX, mouseY);
        return idx < 0 ? -1 : idx;
    }

    /** 进度条命中（容差放宽，条只有 6px 高） */
    private boolean overBar(int mouseX, int mouseY, ArmorSkillDef def)
    {
        int idx = rowIndexAt(mouseX, mouseY);
        if (idx < 0 || tab == TAB_FEATURE)
        {
            return false;
        }
        int ry = listY0 + idx * ROW_STEP - scroll;
        int bx = listX0 + 3 + (listW - 6) - 8 - BAR_W;
        return mouseX >= bx - 4 && mouseX <= bx + BAR_W + 4 && mouseY >= ry + 10 && mouseY <= ry + ROW_H;
    }

    private int barLevelFromMouse(int mouseX, ArmorSkillDef def)
    {
        int bx = listX0 + 3 + (listW - 6) - 8 - BAR_W;
        double r = (mouseX - bx) / (double) BAR_W;
        return (int) Math.round(Math.max(0.0, Math.min(1.0, r)) * def.maxLevel());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        computeLayout();
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // 标签行
        int ty = py0 + TITLE_H + 2;
        int tabW = (listW - (TAB_COUNT - 1) * 4) / TAB_COUNT;
        for (int i = 0; i < TAB_COUNT; i++)
        {
            int x = listX0 + i * (tabW + 4);
            if (inRect(mx, my, x, ty, tabW, 18))
            {
                if (tab != i)
                {
                    tab = i;
                    scroll = 0;
                }
                return true;
            }
        }

        // 滚动条
        if (maxScroll > 0 && mx >= listX0 + listW - 6 && inRect(mx, my, listX0 + listW - 6, listY0, 6, listH))
        {
            draggingScrollbar = true;
            scroll = (int) ((my - listY0) / (double) listH * maxScroll);
            return true;
        }

        // 列表内容
        if (tab == TAB_FEATURE)
        {
            int f = featureAt(mx, my);
            if (f >= 0)
            {
                int mask = GodArmorState.getClientMask();
                GodArmorState.setClientMask(GodArmorFeatures.isOn(mask, f) ? mask & ~(1 << f) : mask | (1 << f));
                ArmorMessages.sendMask(GodArmorState.getClientMask());
                return true;
            }
        }
        else
        {
            ArmorSkillDef def = skillAt(mx, my);
            if (def != null)
            {
                if (overBar(mx, my, def))
                {
                    draggingBar = true;
                    draggingSkill = def.id();
                    ArmorSkillMessages.sendAction(def.id(), ArmorSkillMessages.ACTION_SET_LEVEL, barLevelFromMouse(mx, def));
                }
                else if (button == 1)
                {
                    ArmorSkillMessages.sendAction(def.id(), hasShiftDown()
                            ? ArmorSkillMessages.ACTION_LEVEL_UP_10
                            : ArmorSkillMessages.ACTION_LEVEL_UP);
                }
                else if (button == 0)
                {
                    ArmorSkillMessages.sendAction(def.id(), ArmorSkillMessages.ACTION_TOGGLE);
                }
                return true;
            }
        }

        // 底部按钮
        int by = py0 + panelH - BOTTOM_H + 2;
        int bw = (listW - 12) / 4;
        for (int i = 0; i < 4; i++)
        {
            int x = listX0 + i * (bw + 4);
            if (inRect(mx, my, x, by, bw, 20))
            {
                switch (i)
                {
                    case 0 -> bulk(1);
                    case 1 -> bulk(-1);    // 全部关闭（只关，保留等级）
                    case 2 -> resetAll();
                    default -> this.onClose();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void bulk(int level)
    {
        if (tab == TAB_FEATURE)
        {
            GodArmorState.setClientMask(level > 0 ? GodArmorFeatures.ALL : 0);
            ArmorMessages.sendMask(GodArmorState.getClientMask());
            return;
        }
        // level > 0 = 全部开启；level = -1 = 全部关闭但保留等级
        ArmorSkillMessages.sendBulk(categoryOfTab().ordinal(), level);
    }

    private void resetAll()
    {
        GodArmorState.setClientMask(0);
        ArmorMessages.sendMask(0);
        // 0 = 真重置：连等级一起清除
        ArmorSkillMessages.sendBulk(ArmorSkillMessages.CATEGORY_ALL, 0);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (draggingScrollbar)
        {
            scroll = (int) ((my - listY0) / (double) Math.max(1, listH) * maxScroll);
            scroll = Math.max(0, Math.min(maxScroll, scroll));
            return true;
        }
        if (draggingBar && !draggingSkill.isEmpty())
        {
            ArmorSkillDef def = ArmorSkills.get(draggingSkill);
            if (def != null)
            {
                ArmorSkillMessages.sendAction(def.id(), ArmorSkillMessages.ACTION_SET_LEVEL, barLevelFromMouse(mx, def));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        draggingBar = false;
        draggingSkill = "";
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        computeLayout();
        int mx = (int) mouseX;
        int my = (int) mouseY;
        // 悬停在进度条上 → 调等级；否则滚动列表
        if (tab != TAB_FEATURE)
        {
            ArmorSkillDef def = skillAt(mx, my);
            if (def != null && overBar(mx, my, def))
            {
                int dir = verticalAmount > 0 ? 1 : -1;
                ArmorSkillMessages.sendAction(def.id(), dir > 0
                        ? ArmorSkillMessages.ACTION_LEVEL_UP
                        : ArmorSkillMessages.ACTION_SET_LEVEL,
                        dir > 0 ? 0 : Math.max(0, ArmorSkillData.clientLevel(def.id()) - 1));
                return true;
            }
        }
        if (maxScroll > 0)
        {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(verticalAmount * ROW_STEP * 2)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose()
    {
        ClientConfig.setLastTab(tab); // 记住这一页，下次打开直接回来
        super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    // ══════════════════ 圆角矩形 ══════════════════

    /** 像素级圆角矩形（逐行按圆的方程内缩，视觉与参考模组的 fillRound 同类）。 */
    private static void fillRound(GuiGraphics gui, int x0, int y0, int x1, int y1, int radius, int color)
    {
        if (x1 <= x0 || y1 <= y0)
        {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min((x1 - x0) / 2, (y1 - y0) / 2)));
        if (r == 0)
        {
            gui.fill(x0, y0, x1, y1, color);
            return;
        }
        for (int y = y0; y < y1; y++)
        {
            int inset = 0;
            double dy = 0;
            if (y < y0 + r)
            {
                dy = (y0 + r) - y - 0.5;
            }
            else if (y >= y1 - r)
            {
                dy = y - (y1 - r) + 0.5;
            }
            if (dy > 0)
            {
                inset = (int) Math.round(r - Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            }
            gui.fill(x0 + inset, y, x1 - inset, y + 1, color);
        }
    }
}
