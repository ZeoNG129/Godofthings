package com.godofthings.godgrid;

import com.godofthings.Godofthings;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.Locale;

/**
 * 7 台神之机器共用的网格界面 —— v5.30.0 起<b>每台机器按自己的 UI 原画</b>绘制。
 *
 * <p>布局来自 {@link AbstractGodGridMenu#layout}（{@link GodGridLayouts} 按机器 id 查表），
 * 面板尺寸、槽位坐标、玩家栏位置全部由它决定，因此 7 台机器各不相同：</p>
 * <ul>
 *   <li>底图按机器名取 {@code textures/gui/<id>.png}，用 11 参数 blit 显式传贴图真实尺寸
 *       （否则会按 256×256 UV 采样被静默拉伸）；</li>
 *   <li>槽框 / 箭头 / 页签仍由本类按 spec 坐标程序化绘制 —— 保证点击区域与视觉永远对齐；</li>
 *   <li>输入槽画金框、输出槽画青框、加速槽画暗金框，与原画的功能分区呼应。</li>
 * </ul>
 */
public abstract class AbstractGodGridScreen<T extends AbstractGodGridMenu> extends AbstractContainerScreen<T>
{
    /** 本机的几何（构造时从菜单取的同一份 spec） */
    protected final GodGridLayoutSpec layout;
    /** 本机底图：textures/gui/<layout id>.png */
    private final ResourceLocation background;

    protected AbstractGodGridScreen(T menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.layout = menu.layout;
        this.imageWidth = this.layout.imageW;
        this.imageHeight = this.layout.imageH;
        this.background = ResourceLocation.fromNamespaceAndPath(
                Godofthings.MODID, "textures/gui/" + this.layout.id + ".png");
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;

        // 11 参数重载：显式传贴图真实尺寸，避免按 256×256 UV 采样被拉伸
        gui.blit(this.background, x, y, 0, 0, this.imageWidth, this.imageHeight,
                this.layout.texW, this.layout.texH);

        // 输入槽（金框）
        for (int i = 0; i < this.layout.inputCount(); i++)
        {
            drawSlot(gui, x + this.layout.inputX(i), y + this.layout.inputY(i),
                    GodGridLayout.COLOR_INPUT_BORDER);
        }
        // 输出槽（青框）
        for (int j = 0; j < this.layout.outputCount(); j++)
        {
            drawSlot(gui, x + this.layout.outputX(j), y + this.layout.outputY(j),
                    GodGridLayout.COLOR_OUTPUT_BORDER);
        }
        // 加速槽（暗金框）
        if (this.layout.hasAccel)
        {
            drawSlot(gui, x + this.layout.accelX, y + this.layout.accelY,
                    GodGridLayout.COLOR_ACCEL_BORDER);
        }

        // 页签：面配置（左侧）、AE / 排序（右侧）。位置固定在面板外边框上。
        int tabLeftX = x - 26;
        int tabRightX = x + this.imageWidth + 4;
        if (this.menu.host.gridSupportsFaceConfig())
        {
            drawTab(gui, tabLeftX, y + 33, Component.translatable("gui.godofthings.grid.face"),
                    isHovered(mouseX, mouseY, tabLeftX, y + 33), 0xFF6B6B6B);
        }
        drawTab(gui, tabRightX, y + 33, Component.literal("AE"),
                isHovered(mouseX, mouseY, tabRightX, y + 33),
                this.menu.isAeEnabled() ? 0xFF2F8F3F : 0xFF3A4048);
        drawTab(gui, tabRightX, y + 57,
                Component.translatable(GodGridSorting.modeLabelKey(this.menu.sortMode())),
                isHovered(mouseX, mouseY, tabRightX, y + 57), 0xFF2F5F8F);
    }

    /** 按 spec 的槽位尺寸画一格（外框 + 内底） */
    private void drawSlot(GuiGraphics gui, int sx, int sy, int border)
    {
        int s = this.layout.slot;
        gui.fill(sx, sy, sx + s, sy + s, border);
        gui.fill(sx + 1, sy + 1, sx + s - 1, sy + s - 1, GodGridLayout.COLOR_SLOT_INNER);
    }

    /**
     * 自管 long 槽：图标 + 自绘数量数字。
     *
     * <p>原版 {@code AbstractContainerScreen#renderSlot} 会因「视图 count &gt; 1」在物品左上角
     * 另外画一个堆叠数字，与自绘数字重影，所以这里不复用原版。数字紧跟图标之后画 ——
     * 顺序即层次，「物品先、数字后」，数字永远在图标上层。</p>
     */
    @Override
    protected void renderSlot(GuiGraphics gui, Slot slot)
    {
        if (this.menu.host.gridHasLongAmounts() && slot instanceof LongSlotHandler.LongSlot longSlot)
        {
            gui.renderItem(slot.getItem(), slot.x, slot.y);
            drawLongAmount(gui, longSlot);
            return;
        }
        super.renderSlot(gui, slot);
    }

    /** 单个 long 槽的数量数字 —— 右对齐贴槽内右下角，缩到 0.85 倍 */
    private void drawLongAmount(GuiGraphics gui, LongSlotHandler.LongSlot slot)
    {
        long amount = this.menu.gridAmount(slot.slotIndex());
        String text;
        if (amount > 99L)
        {
            text = compactAmount(amount);
        }
        else if (amount > 1L)
        {
            text = Long.toString(amount);
        }
        else
        {
            return;
        }
        int textWidth = this.font.width(text);
        if (textWidth <= 0)
        {
            return;
        }
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(slot.x + 16, slot.y + 16, 0.0D);
        pose.scale(0.85F, 0.85F, 1.0F);
        pose.translate(-(slot.x + 16), -(slot.y + 16), 0.0D);
        gui.renderItemDecorations(this.font, slot.getItem(), slot.x, slot.y, text);
        pose.popPose();
    }

    /** 紧凑数字：≥1e9 用 1.1B，≥1e6 用 1.1M，≥1000 用 1.2K，否则原样 */
    private static String compactAmount(long amount)
    {
        if (amount >= 1_000_000_000L)
        {
            return String.format(Locale.ROOT, "%.1fB", amount / 1_000_000_000.0D);
        }
        if (amount >= 1_000_000L)
        {
            return String.format(Locale.ROOT, "%.1fM", amount / 1_000_000.0D);
        }
        if (amount >= 1_000L)
        {
            return String.format(Locale.ROOT, "%.1fK", amount / 1_000.0D);
        }
        return Long.toString(amount);
    }

    private void drawTab(GuiGraphics gui, int bx, int by, Component label, boolean hovered, int fillColor)
    {
        int size = GodGridLayout.TAB_SIZE;
        gui.fill(bx, by, bx + size, by + size, 0xFF1B1B1B);
        gui.fill(bx + 1, by + 1, bx + size - 1, by + size - 1,
                hovered ? GodGridLayout.COLOR_BUTTON_BG_HOVER : fillColor);
        gui.fill(bx + 1, by + 1, bx + size - 1, by + 2, 0xFF9A9A9A);
        gui.fill(bx + 1, by + 1, bx + 2, by + size - 1, 0xFF9A9A9A);
        gui.fill(bx + 1, by + size - 2, bx + size - 1, by + size - 1, 0xFF2B2B2B);
        gui.fill(bx + size - 2, by + 1, bx + size - 1, by + size - 1, 0xFF2B2B2B);
        int textWidth = this.font.width(label);
        gui.drawString(this.font, label, bx + (size - textWidth) / 2, by + (size - 8) / 2, 0xFFFFFFFF, false);
    }

    private boolean isHovered(int mouseX, int mouseY, int bx, int by)
    {
        int size = GodGridLayout.TAB_SIZE;
        return mouseX >= bx && mouseX < bx + size && mouseY >= by && mouseY < by + size;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        // 标题居中在面板顶部（各机器面板高度不同，这里用常量 6 的基线刚好落在铭牌上）
        gui.drawString(this.font, this.title,
                (this.imageWidth - this.font.width(this.title)) / 2, 6,
                GodGridLayout.COLOR_TITLE, false);
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"),
                this.layout.invX, this.layout.invY - 11,
                GodGridLayout.COLOR_TEXT_DARK, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int x = this.leftPos;
        int y = this.topPos;
        int mx = (int) mouseX;
        int my = (int) mouseY;

        int tabLeftX = x - 26;
        int tabRightX = x + this.imageWidth + 4;

        if (this.menu.host.gridSupportsFaceConfig() && isHovered(mx, my, tabLeftX, y + 33))
        {
            sendButton(AbstractGodGridMenu.BTN_FACE_CONFIG);
            return true;
        }
        if (isHovered(mx, my, tabRightX, y + 33))
        {
            sendButton(AbstractGodGridMenu.BTN_AE_TOGGLE);
            return true;
        }
        if (isHovered(mx, my, tabRightX, y + 57))
        {
            sendButton(AbstractGodGridMenu.BTN_SORT);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendButton(int buttonId)
    {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn != null)
        {
            conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, buttonId));
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        int x = this.leftPos;
        int y = this.topPos;
        int tabLeftX = x - 26;
        int tabRightX = x + this.imageWidth + 4;

        if (this.menu.host.gridSupportsFaceConfig() && isHovered(mouseX, mouseY, tabLeftX, y + 33))
        {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.godofthings.grid.face.tooltip"), mouseX, mouseY);
        }
        else if (isHovered(mouseX, mouseY, tabRightX, y + 33))
        {
            gui.renderTooltip(this.font, Component.translatable(this.menu.isAeEnabled()
                    ? "gui.godofthings.grid.ae.on" : "gui.godofthings.grid.ae.off"), mouseX, mouseY);
        }
        else if (isHovered(mouseX, mouseY, tabRightX, y + 57))
        {
            gui.renderTooltip(this.font, Component.translatable("gui.godofthings.grid.sort.tooltip",
                    Component.translatable(GodGridSorting.modeLabelKey(this.menu.sortMode()))), mouseX, mouseY);
        }
        else if (this.layout.hasAccel && this.hoveredSlot != null && !this.hoveredSlot.hasItem()
                && mouseX >= x + this.layout.accelX && mouseX < x + this.layout.accelX + this.layout.slot
                && mouseY >= y + this.layout.accelY && mouseY < y + this.layout.accelY + this.layout.slot)
        {
            gui.renderTooltip(this.font, Component.translatable("gui.godofthings.grid.accel.tooltip",
                    this.menu.host.gridOperationsPerColumn()), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
