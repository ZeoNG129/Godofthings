package com.godofthings.client.screen;

import com.godofthings.armor.GodArmorFeatures;
import com.godofthings.armor.GodArmorState;
import com.godofthings.armor.skill.ArmorSkillCategory;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkillDef;
import com.godofthings.armor.skill.ArmorSkills;
import com.godofthings.config.ClientConfig;
import com.godofthings.handler.GodArmorHandler;
import com.godofthings.network.ArmorMessages;
import com.godofthings.network.ArmorSkillMessages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * 神之套装界面（K 键打开「神之增幅」页、O 键打开「神之套装」页，两页在同一个界面里切换）。
 *
 * <p><b>v5.9.0 起按用户要求大幅简化</b>：</p>
 * <ul>
 *   <li>只两页：<b>神之增幅</b>（7 个节点，全部是<b>开关</b> —— 开启即给到该节点的最大等级）
 *       + <b>神之套装</b>（8 个功能开关）。</li>
 *   <li>原来的「基础属性 / 特殊增幅 / 机械共鸣 / 魔法增幅」四页与其余节点已整体删除。</li>
 *   <li>「万民敬仰」不再占开关位：<b>穿齐套装即生效</b>，界面底部只写一行说明。</li>
 * </ul>
 *
 * <p><b>坐标纪律</b>：面板左上角是 {@code (px0, py0)}，本文件所有坐标都是 {@code px0/py0 + 偏移}
 * （曾经在便签界面上踩过「把面板内偏移当绝对坐标用」的坑）。</p>
 */
public class GodArmorSkillScreen extends Screen
{
    /** 页索引：0 = 神之增幅（K），1 = 神之套装（O） */
    public static final int TAB_ULTIMATE = 0;
    public static final int TAB_FEATURE = 1;
    private static final int TAB_COUNT = 2;

    private static final int PANEL_MAX_W = 420;
    private static final int PANEL_MAX_H = 250;
    private static final int PAD = 8;
    private static final int TITLE_H = 20;
    private static final int TAB_H = 16;
    private static final int ROW_H = 22;
    private static final int BOTTOM_H = 30;

    private static final int C_FRAME_BG = 0xF01A1C22;
    private static final int C_FRAME_EDGE = 0xFF3A3F4A;
    private static final int C_TITLE = 0xFFF0E6D2;
    private static final int C_ACCENT = 0xFFC8A03A;
    private static final int C_TEXT = 0xFFE6E8EE;
    private static final int C_TEXT_DIM = 0xFF9AA0AB;
    private static final int C_ON = 0xFF7FD48A;
    private static final int C_OFF = 0xFF5A6068;
    private static final int C_OFF_TEXT = 0xFF8A9099;
    private static final int C_LIST_BG = 0xFF23262E;
    private static final int C_ROW_HOVER = 0x22FFFFFF;
    private static final int C_WARN = 0xFFFF9A6B;

    private int tab;
    private int scroll;

    /** 穿齐状态缓存：每 5 tick 重算一次（不每帧扫背包） */
    private boolean wornCache;
    private int wornTick;

    private int px0;
    private int py0;
    private int panelW;
    private int panelH;

    public GodArmorSkillScreen()
    {
        // 默认回到上次离开的那一页（用户要求：不要每次都跳回第一页）
        this(ClientConfig.getLastTab());
    }

    public GodArmorSkillScreen(int initialTab)
    {
        super(Component.translatable("gui.godofthings.armor.config"));
        this.tab = Mth.clamp(initialTab, 0, TAB_COUNT - 1);
    }

    // ------------------------------------------------------------------ 布局

    @Override
    protected void init()
    {
        super.init();
        // 打开界面时主动拉一次：技能表与开关位图都要是最新的
        ArmorMessages.requestSync();
        ArmorSkillMessages.requestSync();

        this.panelW = Math.max(260, Math.min(PANEL_MAX_W, this.width - 32));
        this.panelH = Math.max(180, Math.min(PANEL_MAX_H, this.height - 32));
        this.px0 = (this.width - panelW) / 2;
        this.py0 = (this.height - panelH) / 2;
        this.scroll = 0;

        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(px0 + panelW - PAD - 16, py0 + 4, 16, 16).build());

        int tabW = Math.max(60, (panelW - PAD * 2 - 4) / TAB_COUNT);
        for (int i = 0; i < TAB_COUNT; i++)
        {
            int index = i;
            addRenderableWidget(Button.builder(tabLabel(i), b ->
            {
                tab = index;
                scroll = 0;
            }).bounds(px0 + PAD + i * (tabW + 4), py0 + TITLE_H, tabW, TAB_H).build());
        }

        int btnW = Math.max(70, (panelW - PAD * 2 - 6) / 2);
        addRenderableWidget(Button.builder(Component.translatable("gui.godofthings.armor.all_on"), b -> setAll(true))
                .bounds(px0 + PAD, py0 + panelH - PAD - 18, btnW, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.godofthings.armor.all_off"), b -> setAll(false))
                .bounds(px0 + PAD + btnW + 6, py0 + panelH - PAD - 18, btnW, 18).build());
    }

    private Component tabLabel(int index)
    {
        return index == TAB_ULTIMATE
                ? Component.translatable(ArmorSkillCategory.ULTIMATE.getLangKey())
                : Component.translatable("gui.godofthings.armor.config");
    }

    private int tabW()
    {
        return Math.max(60, (panelW - PAD * 2 - 4) / TAB_COUNT);
    }

    /** 当前页的行数（神之增幅 = 节点个数；神之套装 = 开关位数） */
    private int rowCount()
    {
        return tab == TAB_ULTIMATE ? ArmorSkills.all().size() : GodArmorFeatures.COUNT;
    }

    private int listTop()
    {
        return py0 + TITLE_H + TAB_H + 6;
    }

    private int listBottom()
    {
        return py0 + panelH - PAD - BOTTOM_H;
    }

    private int visibleRows()
    {
        return Math.max(1, (listBottom() - listTop()) / ROW_H);
    }

    // ------------------------------------------------------------------ 数据

    private boolean worn()
    {
        int now = this.minecraft == null || this.minecraft.player == null ? 0 : this.minecraft.player.tickCount;
        if (Math.abs(now - wornTick) >= 5)
        {
            wornTick = now;
            wornCache = this.minecraft != null && this.minecraft.player != null
                    && GodArmorHandler.isFullSetWorn(this.minecraft.player);
        }
        return wornCache;
    }

    private Map<String, Integer> levels()
    {
        return ArmorSkillData.getClientLevels();
    }

    /** 开关一个增幅节点：开启 = 直接给满级；关闭 = 只关、等级留着 */
    private void toggleSkill(ArmorSkillDef def, boolean on)
    {
        Map<String, Integer> next = new HashMap<>(levels());
        if (on)
        {
            ArmorSkillMessages.sendAction(def.id(), ArmorSkillMessages.ACTION_SET_LEVEL, def.maxLevel());
            next.put(def.id(), def.maxLevel()); // 本地先按满级显示，等回包覆盖成权威值
        }
        else
        {
            ArmorSkillMessages.sendAction(def.id(), ArmorSkillMessages.ACTION_TOGGLE, 0);
            int remembered = ArmorSkillData.level(levels(), def.id());
            if (remembered > 0)
            {
                next.put(def.id(), -remembered);
            }
        }
        ArmorSkillData.setClientLevels(next);
    }

    private void toggleFeature(int feature)
    {
        int mask = GodArmorState.getClientMask();
        int next = GodArmorFeatures.isOn(mask, feature) ? mask & ~(1 << feature) : mask | (1 << feature);
        GodArmorState.setClientMask(next);
        ArmorMessages.sendMask(next);
    }

    /** 当前页全部开 / 全部关 */
    private void setAll(boolean on)
    {
        if (tab == TAB_ULTIMATE)
        {
            for (ArmorSkillDef def : ArmorSkills.all())
            {
                toggleSkill(def, on);
            }
            return;
        }
        int mask = on ? GodArmorFeatures.ALL : 0;
        GodArmorState.setClientMask(mask);
        ArmorMessages.sendMask(mask);
    }

    // ------------------------------------------------------------------ 渲染

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        gui.fill(0, 0, this.width, this.height, 0x66000000);
        // 面板
        gui.fill(px0 - 1, py0 - 1, px0 + panelW + 1, py0 + panelH + 1, C_FRAME_EDGE);
        gui.fill(px0, py0, px0 + panelW, py0 + panelH, C_FRAME_BG);
        gui.fill(px0, py0, px0 + panelW, py0 + 1, C_ACCENT);

        gui.drawString(this.font, Component.translatable("gui.godofthings.armor.config"),
                px0 + PAD, py0 + 6, C_TITLE, false);

        // 当前页签下方的金色高亮线
        int tx = px0 + PAD + tab * (tabW() + 4);
        gui.fill(tx, py0 + TITLE_H + TAB_H, tx + tabW(), py0 + TITLE_H + TAB_H + 1, C_ACCENT);

        drawList(gui, mouseX, mouseY);

        // 悬停某一行时显示该功能的说明（键与旧版一致，内容已按合并后的语义改写）
        int hoveredRow = rowIndexAt(mouseX, mouseY);
        if (hoveredRow >= 0)
        {
            Component desc = tab == TAB_ULTIMATE
                    ? Component.translatable(ArmorSkills.all().get(hoveredRow).nameKey() + ".desc")
                    : Component.translatable(GodArmorFeatures.LANG_KEYS[hoveredRow] + ".desc");
            gui.renderTooltip(this.font, this.font.split(desc, Math.max(120, panelW - 60)), mouseX, mouseY);
        }

        // 底部说明：常驻效果 + 未穿齐提示
        Component note = Component.translatable(GodArmorFeatures.ALWAYS_ON_LANG_KEY);
        gui.drawString(this.font, this.font.plainSubstrByWidth(note.getString(), panelW - PAD * 2),
                px0 + PAD, py0 + panelH - PAD - BOTTOM_H + 1, C_TEXT_DIM, false);
        if (!worn())
        {
            Component warn = Component.translatable("gui.godofthings.armor.not_worn");
            gui.drawString(this.font, this.font.plainSubstrByWidth(warn.getString(), panelW - PAD * 2),
                    px0 + PAD, py0 + panelH - PAD - BOTTOM_H + 11, C_WARN, false);
        }

        super.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 底色自己画（见 render）
    }

    private void drawList(GuiGraphics gui, int mouseX, int mouseY)
    {
        int top = listTop();
        int bottom = listBottom();
        gui.fill(px0 + PAD, top, px0 + panelW - PAD, bottom, C_LIST_BG);

        int rows = visibleRows();
        int count = rowCount();
        int maxScroll = Math.max(0, count - rows);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        Map<String, Integer> levels = levels();
        int mask = GodArmorState.getClientMask();

        for (int i = 0; i < rows; i++)
        {
            int index = scroll + i;
            if (index >= count)
            {
                break;
            }
            int rowY = top + i * ROW_H;
            boolean hovered = mouseX >= px0 + PAD && mouseX <= px0 + panelW - PAD
                    && mouseY >= rowY && mouseY < rowY + ROW_H - 2;

            Component label;
            Component state;
            boolean on;
            if (tab == TAB_ULTIMATE)
            {
                ArmorSkillDef def = ArmorSkills.all().get(index);
                label = Component.translatable(def.nameKey());
                on = ArmorSkillData.isEnabled(levels, def.id());
                state = Component.translatable("gui.godofthings.armor.skill_state", onOff(on), def.maxLevel());
            }
            else
            {
                label = Component.translatable(GodArmorFeatures.LANG_KEYS[index]);
                on = GodArmorFeatures.isOn(mask, index);
                state = onOff(on);
            }

            if (hovered)
            {
                gui.fill(px0 + PAD + 1, rowY, px0 + panelW - PAD - 1, rowY + ROW_H - 2, C_ROW_HOVER);
            }
            gui.fill(px0 + PAD + 3, rowY + 4, px0 + PAD + 7, rowY + ROW_H - 6, on ? C_ON : C_OFF);
            gui.drawString(this.font, this.font.plainSubstrByWidth(label.getString(), panelW - PAD * 2 - 100),
                    px0 + PAD + 12, rowY + 6, on ? C_TEXT : C_TEXT_DIM, false);
            int stateW = this.font.width(state);
            gui.drawString(this.font, state, px0 + panelW - PAD - stateW - 4, rowY + 6,
                    on ? C_ON : C_OFF_TEXT, false);
        }

        if (maxScroll > 0)
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.armor.scroll_hint",
                            scroll + 1, Math.min(count, scroll + rows), count),
                    px0 + PAD + 2, bottom - 11, C_TEXT_DIM, false);
        }
    }

    private Component onOff(boolean on)
    {
        return Component.translatable(on ? "gui.godofthings.armor.on" : "gui.godofthings.armor.off");
    }

    // ------------------------------------------------------------------ 交互

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int index = rowIndexAt(mouseX, mouseY);
        if (button == 0 && index >= 0)
        {
            if (tab == TAB_ULTIMATE)
            {
                ArmorSkillDef def = ArmorSkills.all().get(index);
                toggleSkill(def, !ArmorSkillData.isEnabled(levels(), def.id()));
            }
            else
            {
                toggleFeature(index);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 鼠标所在的行（越界返回 -1） */
    private int rowIndexAt(double mouseX, double mouseY)
    {
        if (mouseX < px0 + PAD || mouseX > px0 + panelW - PAD
                || mouseY < listTop() || mouseY >= listBottom())
        {
            return -1;
        }
        int index = scroll + (int) (mouseY - listTop()) / ROW_H;
        return index >= 0 && index < rowCount() ? index : -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        int maxScroll = Math.max(0, rowCount() - visibleRows());
        scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll);
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
        // Tab / 左右键切页
        if (keyCode == GLFW.GLFW_KEY_TAB || keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_LEFT)
        {
            boolean back = keyCode == GLFW.GLFW_KEY_LEFT || (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            tab = Math.floorMod(tab + (back ? -1 : 1), TAB_COUNT);
            scroll = 0;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
}
