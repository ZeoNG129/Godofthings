package com.godofthings.client.screen;

import com.godofthings.armor.skill.ArmorSkillCategory;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkillDef;
import com.godofthings.armor.skill.ArmorSkills;
import com.godofthings.network.ArmorSkillMessages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之套装技能树界面（阶段 1：基础属性 + 特殊增幅两列）。
 * <p>
 * 布局与交互参照 <b>Zifeng Skill Tree（子枫的百宝箱）</b> 的技能树界面
 * （Copyright (c) 2026 zifeng, MIT），按神之套装重做为「无技能点、无前置」：
 * <ul>
 *   <li><b>左键</b>：开 / 关（开启即解锁到 1 级）</li>
 *   <li><b>右键</b>：等级 +1（到上限后回到 1 级）</li>
 *   <li><b>Shift + 右键</b>：等级 +10</li>
 *   <li>底部：全部开启 / 全部关闭 / 返回</li>
 * </ul>
 */
public class GodArmorSkillScreen extends Screen
{
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 330;

    private static final int COL_X_0 = 8;
    private static final int COL_X_1 = 184;
    private static final int COL_W = 168;

    private static final int ROWS_Y = 34;
    private static final int ROW_H = 17;

    private static final int BTN_Y = 294;
    private static final int BTN_H = 18;
    private static final int BTN_W = 84;
    private static final int BTN_X_0 = 8;
    private static final int BTN_X_1 = 100;
    private static final int BTN_X_2 = 268;

    private static final int HINT_Y = 316;

    private static final int BG_OUTER = 0xE4000000;
    private static final int BG_INNER = 0xE41A1A1F;
    private static final int NODE_OFF_BG = 0xFF2B2F38;
    private static final int NODE_ON_BG = 0xFF1F4A24;
    private static final int NODE_BORDER = 0xFF11131A;
    private static final int TXT_ON = 0xFFA8E6A8;
    private static final int TXT_OFF = 0xFF8A9099;
    private static final int TXT_LV = 0xFFE0B030;

    /** 两列的分类顺序 */
    private static final ArmorSkillCategory[] COLUMNS = {
            ArmorSkillCategory.BASE, ArmorSkillCategory.AMPLIFY };

    public GodArmorSkillScreen()
    {
        super(Component.translatable("gui.godofthings.armor.skill.title"));
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        int x0 = (this.width - PANEL_W) / 2;
        int y0 = (this.height - PANEL_H) / 2;

        gui.fill(x0, y0, x0 + PANEL_W, y0 + PANEL_H, BG_OUTER);
        gui.fill(x0 + 1, y0 + 1, x0 + PANEL_W - 1, y0 + PANEL_H - 1, BG_INNER);

        gui.drawCenteredString(this.font, this.title, this.width / 2, y0 + 6, 0xFFFFFF);

        for (int c = 0; c < COLUMNS.length; c++)
        {
            ArmorSkillCategory category = COLUMNS[c];
            int colX = x0 + (c == 0 ? COL_X_0 : COL_X_1);
            Component header = Component.translatable(category.getLangKey());
            gui.drawString(this.font, header, colX + 2, y0 + 20, 0xFFC8A03A, false);

            List<ArmorSkillDef> defs = ArmorSkills.of(category);
            for (int r = 0; r < defs.size(); r++)
            {
                drawNode(gui, colX, y0 + ROWS_Y + r * ROW_H, defs.get(r), mouseX, mouseY);
            }
        }

        drawButton(gui, x0 + BTN_X_0, y0 + BTN_Y, BTN_W, Component.translatable("gui.godofthings.armor.skill.all_on"), mouseX, mouseY);
        drawButton(gui, x0 + BTN_X_1, y0 + BTN_Y, BTN_W, Component.translatable("gui.godofthings.armor.skill.all_off"), mouseX, mouseY);
        drawButton(gui, x0 + BTN_X_2, y0 + BTN_Y, 84, Component.translatable("gui.godofthings.back"), mouseX, mouseY);

        gui.drawCenteredString(this.font, Component.translatable("gui.godofthings.armor.skill.hint"),
                this.width / 2, y0 + HINT_Y, 0x707780);

        // 悬停说明
        ArmorSkillDef hovered = nodeAt(mouseX, mouseY);
        if (hovered != null)
        {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(hovered.nameKey()).withStyle(net.minecraft.ChatFormatting.GOLD));
            lines.add(Component.translatable(hovered.descKey()).withStyle(net.minecraft.ChatFormatting.GRAY));
            int lv = ArmorSkillData.clientLevel(hovered.id());
            lines.add(Component.translatable("gui.godofthings.armor.skill.level",
                    lv, hovered.maxLevel()).withStyle(net.minecraft.ChatFormatting.AQUA));
            lines.add(Component.translatable("gui.godofthings.armor.skill.hint.node")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            gui.renderTooltip(this.font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void drawNode(GuiGraphics gui, int bx, int by, ArmorSkillDef def, int mouseX, int mouseY)
    {
        int lv = ArmorSkillData.clientLevel(def.id());
        boolean on = lv > 0;
        boolean hovered = isHovering(bx, by, COL_W, ROW_H - 1, mouseX, mouseY);

        gui.fill(bx, by, bx + COL_W, by + ROW_H - 1, NODE_BORDER);
        gui.fill(bx + 1, by + 1, bx + COL_W - 1, by + ROW_H - 2, on ? NODE_ON_BG : NODE_OFF_BG);

        Component name = Component.translatable(def.nameKey());
        gui.drawString(this.font, name, bx + 4, by + 4, on ? TXT_ON : TXT_OFF, false);

        Component state = on
                ? Component.translatable("gui.godofthings.armor.skill.lv", lv)
                : Component.translatable("gui.godofthings.armor.skill.off");
        int sx = bx + COL_W - 4 - this.font.width(state);
        gui.drawString(this.font, state, sx, by + 4, on ? TXT_LV : TXT_OFF, false);

        if (hovered)
        {
            gui.fill(bx, by, bx + COL_W, by + 1, 0xFFFFFFFF);
            gui.fill(bx, by + ROW_H - 2, bx + COL_W, by + ROW_H - 1, 0xFFFFFFFF);
        }
    }

    private void drawButton(GuiGraphics gui, int bx, int by, int w, Component label, int mouseX, int mouseY)
    {
        boolean hovered = isHovering(bx, by, w, BTN_H, mouseX, mouseY);
        gui.fill(bx, by, bx + w, by + BTN_H, NODE_BORDER);
        gui.fill(bx + 1, by + 1, bx + w - 1, by + BTN_H - 1, hovered ? 0xFF4E5660 : 0xFF3A4048);
        gui.drawString(this.font, label, bx + (w - this.font.width(label)) / 2, by + 5, 0xFFFFFF, false);
    }

    private static boolean isHovering(int bx, int by, int w, int h, int mouseX, int mouseY)
    {
        return mouseX >= bx && mouseX < bx + w && mouseY >= by && mouseY < by + h;
    }

    /** 命中测试：返回鼠标下的技能（无则 null） */
    private ArmorSkillDef nodeAt(int mouseX, int mouseY)
    {
        int x0 = (this.width - PANEL_W) / 2;
        int y0 = (this.height - PANEL_H) / 2;
        for (int c = 0; c < COLUMNS.length; c++)
        {
            int colX = x0 + (c == 0 ? COL_X_0 : COL_X_1);
            List<ArmorSkillDef> defs = ArmorSkills.of(COLUMNS[c]);
            for (int r = 0; r < defs.size(); r++)
            {
                int by = y0 + ROWS_Y + r * ROW_H;
                if (isHovering(colX, by, COL_W, ROW_H - 1, mouseX, mouseY))
                {
                    return defs.get(r);
                }
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        int x0 = (this.width - PANEL_W) / 2;
        int y0 = (this.height - PANEL_H) / 2;

        ArmorSkillDef def = nodeAt(mx, my);
        if (def != null)
        {
            if (button == 1)
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

        if (isHovering(x0 + BTN_X_0, y0 + BTN_Y, BTN_W, BTN_H, mx, my))
        {
            ArmorSkillMessages.sendBulk(ArmorSkillMessages.CATEGORY_ALL, 1);
            return true;
        }
        if (isHovering(x0 + BTN_X_1, y0 + BTN_Y, BTN_W, BTN_H, mx, my))
        {
            ArmorSkillMessages.sendBulk(ArmorSkillMessages.CATEGORY_ALL, 0);
            return true;
        }
        if (isHovering(x0 + BTN_X_2, y0 + BTN_Y, 84, BTN_H, mx, my))
        {
            this.onClose();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
