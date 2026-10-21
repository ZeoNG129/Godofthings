package com.godofthings.construction.client;

import com.godofthings.Godofthings;
import com.godofthings.construction.GodConstructionItem;
import com.godofthings.construction.network.ConstructionActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 神之建筑的设置界面：模式 / 撤销 / 预览 / 相似方块四个开关。
 *
 * <p>纯原版控件（{@link Button} + {@link Screen}），<b>没有 MenuType、没有容器</b> ——
 * 所有状态都存在物品的四个数据组件里，所以不需要服务端的菜单来同步槽位。</p>
 *
 * <p>打开方式：手持神之建筑<b>潜行 + 右键空气</b>（见 {@code GodConstructionItem#use}）。</p>
 *
 * <p>每个按钮点下去都做两件事：① 先改本地物品栈的组件（界面立刻响应）② 发一条
 * {@link ConstructionActionPayload} 让服务端改权威值 —— 组件在物品栏同步时会自己回来。</p>
 */
public class GodConstructionScreen extends Screen
{
    /** 面板尺寸（宽度 = 左右各 12 的边距 + 192 的按钮区） */
    private static final int W = 216;
    private static final int H = 150;
    /** 一排按钮的高度 */
    private static final int BTN_H = 20;
    /** 按钮区左边距 */
    private static final int PAD = 12;
    /** 三个模式按钮之间的间隙 */
    private static final int GAP = 4;

    private Button buildButton;
    private Button destroyButton;
    private Button replaceButton;
    private Button previewButton;
    private Button similarButton;
    private Button undoButton;

    public GodConstructionScreen()
    {
        super(Component.translatable("screen.godofthings.god_construction.title"));
    }

    /** 客户端打开界面的唯一入口（从物品的 use() 里调） */
    public static void open()
    {
        Minecraft.getInstance().setScreen(new GodConstructionScreen());
    }

    @Override
    protected void init()
    {
        int cx = (this.width - W) / 2;
        int cy = (this.height - H) / 2;
        int left = cx + PAD;
        // 三个模式按钮等宽排一行（192 宽塞 3 个 61 + 2 个间隙 4 = 191）
        int colW = 61;

        this.buildButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("button.godofthings.god_construction.build"),
                        b -> changeMode(GodConstructionItem.MODE_BUILD))
                .bounds(left, cy + 34, colW, BTN_H).build());
        this.destroyButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("button.godofthings.god_construction.destroy"),
                        b -> changeMode(GodConstructionItem.MODE_DESTROY))
                .bounds(left + colW + GAP, cy + 34, colW, BTN_H).build());
        this.replaceButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("button.godofthings.god_construction.replace"),
                        b -> changeMode(GodConstructionItem.MODE_REPLACE))
                .bounds(left + (colW + GAP) * 2, cy + 34, colW, BTN_H).build());

        // 两个开关按钮（文字后面拼 on / off）
        this.previewButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("button.godofthings.god_construction.preview"),
                        b -> togglePreview())
                .bounds(left, cy + 62, 94, BTN_H).build());
        this.similarButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("button.godofthings.god_construction.similar"),
                        b -> toggleSimilar())
                .bounds(left + 98, cy + 62, 94, BTN_H).build());

        // 撤销（按钮上不显示剩余次数，按一次撤一步）
        this.undoButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("button.godofthings.god_construction.undo"),
                        b -> undo())
                .bounds(left, cy + 90, 192, BTN_H).build());

        // 关掉界面就用原版的 Done（CommonComponents.GUI_DONE 就是原版 gui.done 那个键）
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(left, cy + 120, 192, BTN_H).build());

        refreshLabels();
    }

    /**
     * 每 tick 刷一次按钮文字：组件从服务端同步回来（比如别处切了模式）时界面能跟上。
     */
    @Override
    public void tick()
    {
        super.tick();
        refreshLabels();
    }

    /** 切模式：本地先改，再让服务端改权威值 */
    private void changeMode(int mode)
    {
        ItemStack wand = wandStack();
        if (wand.isEmpty())
        {
            return;
        }
        if (GodConstructionItem.getMode(wand) != mode)
        {
            wand.set(Godofthings.GOD_CONSTRUCTION_MODE.get(), mode);
            ConstructionActionPayload.sendSetMode(mode);
        }
        refreshLabels();
    }

    /** 翻转预览线框开关 */
    private void togglePreview()
    {
        ItemStack wand = wandStack();
        if (wand.isEmpty())
        {
            return;
        }
        // 本地取反后立刻发服务端；服务端也是「取反」，两边起点一致就不会打架
        wand.set(Godofthings.GOD_CONSTRUCTION_PREVIEW.get(), !GodConstructionItem.isPreviewEnabled(wand));
        ConstructionActionPayload.sendTogglePreview();
        refreshLabels();
    }

    /** 翻转相似方块开关 */
    private void toggleSimilar()
    {
        ItemStack wand = wandStack();
        if (wand.isEmpty())
        {
            return;
        }
        wand.set(Godofthings.GOD_CONSTRUCTION_SIMILAR.get(), !GodConstructionItem.isSimilarEnabled(wand));
        ConstructionActionPayload.sendToggleSimilar();
        refreshLabels();
    }

    /** 撤销上一次建造 / 销毁 / 替换（结果由服务端在行动栏反馈） */
    private void undo()
    {
        ConstructionActionPayload.sendUndo();
    }

    /** 按当前组件值重写一遍按钮文字（当前模式加 "&gt; &lt;" 标记并染黄） */
    private void refreshLabels()
    {
        ItemStack wand = wandStack();
        this.buildButton.setMessage(modeLabel(wand, GodConstructionItem.MODE_BUILD,
                Component.translatable("button.godofthings.god_construction.build")));
        this.destroyButton.setMessage(modeLabel(wand, GodConstructionItem.MODE_DESTROY,
                Component.translatable("button.godofthings.god_construction.destroy")));
        this.replaceButton.setMessage(modeLabel(wand, GodConstructionItem.MODE_REPLACE,
                Component.translatable("button.godofthings.god_construction.replace")));

        this.previewButton.setMessage(toggleLabel(
                Component.translatable("button.godofthings.god_construction.preview"),
                !wand.isEmpty() && GodConstructionItem.isPreviewEnabled(wand)));
        this.similarButton.setMessage(toggleLabel(
                Component.translatable("button.godofthings.god_construction.similar"),
                !wand.isEmpty() && GodConstructionItem.isSimilarEnabled(wand)));
    }

    /** 当前模式：{@code > 名字 <} 且染黄；其它模式原样显示 */
    private static Component modeLabel(ItemStack wand, int mode, Component label)
    {
        if (!wand.isEmpty() && GodConstructionItem.getMode(wand) == mode)
        {
            return Component.literal("> ")
                    .append(label.copy().withStyle(ChatFormatting.YELLOW))
                    .append(" <");
        }
        return label;
    }

    /** 开 / 关按钮：{@code 名字 开} / {@code 名字 关} */
    private static Component toggleLabel(Component label, boolean on)
    {
        Component state = on
                ? Component.translatable("option.godofthings.god_construction.on")
                : Component.translatable("option.godofthings.god_construction.off");
        return label.copy().append(" ").append(state);
    }

    /** 主手优先、其次副手；界面对应的就是手上那一个（神之建筑 / 天神建筑都认） */
    private ItemStack wandStack()
    {
        if (this.minecraft == null || this.minecraft.player == null)
        {
            return ItemStack.EMPTY;
        }
        return GodConstructionItem.findWand(this.minecraft.player);
    }

    @Override
    public void renderBackground(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        // 不画默认的渐变暗化背景，改由 render() 手动铺纯色面板（本界面不带贴图）
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        int cx = (this.width - W) / 2;
        int cy = (this.height - H) / 2;
        gui.fill(0, 0, this.width, this.height, 0xC0101010);
        gui.fill(cx, cy, cx + W, cy + H, 0xFF1E1E22);
        gui.drawString(this.font, this.title, cx + PAD, cy + 12, 0xFFFFFF);
        super.render(gui, mouseX, mouseY, partialTick); // renderBackground(no-op) + 控件
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
