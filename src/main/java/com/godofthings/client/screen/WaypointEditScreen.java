package com.godofthings.client.screen;

import com.godofthings.network.WaypointMessages;
import com.godofthings.waypoint.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Predicate;

/**
 * 传送点编辑界面：修改点位名字与坐标（维度 / 面对方向保持不变）。
 * 由 {@link AbstractWaypointScreen} 的「编辑」按钮打开。
 */
public class WaypointEditScreen extends Screen
{
    private static final int W = 256;
    private static final int H = 158;

    /** 只允许数字 / 负号 / 小数点的输入过滤 */
    private static final Predicate<String> NUMERIC = s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*");

    private final Screen parent;
    /** null = 新建模式（无已有点位，坐标默认玩家当前位置） */
    private final Waypoint wp;

    private EditBox nameField;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    public WaypointEditScreen(Screen parent, Waypoint wp)
    {
        super(wp == null
                ? Component.translatable("gui.godofthings.waypoint.new_title")
                : Component.translatable("gui.godofthings.waypoint.edit_title"));
        this.parent = parent;
        this.wp = wp;
    }

    /** 新建模式：名字为空、坐标默认玩家当前位置。 */
    public WaypointEditScreen(Screen parent)
    {
        this(parent, null);
    }

    @Override
    protected void init()
    {
        int cx = (this.width - W) / 2;
        int cy = (this.height - H) / 2;

        this.nameField = new EditBox(this.font, cx + 80, cy + 36, 160, 18, Component.translatable("gui.godofthings.waypoint.edit_name"));
        this.nameField.setMaxLength(32);
        this.nameField.setValue(wp == null ? "" : wp.name);

        this.xField = new EditBox(this.font, cx + 80, cy + 62, 48, 18, Component.literal("X"));
        this.yField = new EditBox(this.font, cx + 134, cy + 62, 48, 18, Component.literal("Y"));
        this.zField = new EditBox(this.font, cx + 188, cy + 62, 48, 18, Component.literal("Z"));
        this.xField.setFilter(NUMERIC);
        this.yField.setFilter(NUMERIC);
        this.zField.setFilter(NUMERIC);
        if (wp == null)
        {
            this.xField.setValue("0");
            this.yField.setValue("0");
            this.zField.setValue("0");
        }
        else
        {
            this.xField.setValue(String.valueOf(Math.round(wp.x)));
            this.yField.setValue(String.valueOf(Math.round(wp.y)));
            this.zField.setValue(String.valueOf(Math.round(wp.z)));
        }

        this.addRenderableWidget(this.nameField);
        this.addRenderableWidget(this.xField);
        this.addRenderableWidget(this.yField);
        this.addRenderableWidget(this.zField);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.waypoint.edit_here"),
                b -> fillFromPlayerPos()).bounds(cx + 80, cy + 88, 160, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.waypoint.save"),
                b -> save()).bounds(cx + 40, cy + 120, 80, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.godofthings.waypoint.cancel"),
                b -> this.minecraft.setScreen(parent)).bounds(cx + 136, cy + 120, 80, 20).build());

        this.setInitialFocus(this.nameField);
        if (wp == null)
        {
            fillFromPlayerPos(); // 新建默认填玩家当前位置
        }
    }

    /** 用玩家当前站立位置填充坐标输入框 */
    private void fillFromPlayerPos()
    {
        if (this.minecraft != null && this.minecraft.player != null)
        {
            this.xField.setValue(String.valueOf(Math.round(this.minecraft.player.getX())));
            this.yField.setValue(String.valueOf(Math.round(this.minecraft.player.getY())));
            this.zField.setValue(String.valueOf(Math.round(this.minecraft.player.getZ())));
        }
    }

    /** 保存：校验名字与坐标后发送编辑请求并返回列表 */
    private void save()
    {
        String newName = this.nameField.getValue().trim();
        if (newName.isEmpty())
        {
            return;
        }
        double x, y, z;
        try
        {
            x = Double.parseDouble(this.xField.getValue().trim());
            y = Double.parseDouble(this.yField.getValue().trim());
            z = Double.parseDouble(this.zField.getValue().trim());
        }
        catch (NumberFormatException e)
        {
            return; // 非法数字输入忽略
        }
        if (wp == null)
        {
            WaypointMessages.sendCreate(newName, x, y, z);
        }
        else
        {
            WaypointMessages.sendEdit(wp.name, newName, x, y, z);
        }
        this.minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 不渲染默认渐变暗化背景，改由 render() 手动绘制纯色面板
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        int cx = (this.width - W) / 2;
        int cy = (this.height - H) / 2;
        gui.fill(0, 0, this.width, this.height, 0xC0101010);
        gui.fill(cx, cy, cx + W, cy + H, 0xFF1E1E22);
        gui.drawString(this.font, this.title, cx + 20, cy + 14, 0xFFFFFF);
        gui.drawString(this.font, Component.translatable("gui.godofthings.waypoint.edit_name"), cx + 20, cy + 41, 0xCCCCCC);
        gui.drawString(this.font, Component.translatable("gui.godofthings.waypoint.edit_coord"), cx + 20, cy + 67, 0xCCCCCC);
        super.render(gui, mouseX, mouseY, partialTick); // renderBackground(no-op) + widgets
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
