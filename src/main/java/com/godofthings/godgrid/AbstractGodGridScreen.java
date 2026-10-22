package com.godofthings.godgrid;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.resources.ResourceLocation;

import com.godofthings.Godofthings;


import java.util.Locale;

/**
 * 六台神之机器共用的网格界面 —— 几何一比一照抄 PBG「无限通用机械离心机工厂」，
 * 全部用 {@link GuiGraphics#fill} 绘制（不 blit 贴图）。
 *
 * <p>与原版一致的部分：面板 362×218、输入行 y=17、进度箭头 y=33、输出三排 y=57/77/97、
 * 物品栏 y=135 居中。改动的部分：删掉流体罐 / 多流体页签 / PB 升级页签 / Mek 升级槽，
 * 原流体罐位置改成 1 格加速槽，左右两侧只留「面配置」「AE」「排序」三个按钮。</p>
 */
public abstract class AbstractGodGridScreen<T extends AbstractGodGridMenu> extends AbstractContainerScreen<T>
{
    /** 面板底图（v5.27.0 起改用贴图，重绘资源包风格） */
    private static final ResourceLocation GOD_GRID_BG =
            ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "textures/gui/god_grid_bg.png");
    /** 底图实际像素尺寸（blit 时显式传入，避免按 256×256 UV 采样被拉伸） */
    private static final int GOD_GRID_BG_W = 512;
    private static final int GOD_GRID_BG_H = 256;

    protected AbstractGodGridScreen(T menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = GodGridLayout.IMAGE_W;
        this.imageHeight = GodGridLayout.IMAGE_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;

        // v5.27.0：面板底图改成贴图（重绘资源包风格：深空底 + 双线金边），
        // 槽位 / 箭头 / 页签仍由本类按 GodGridLayout 的精确坐标程序化绘制，
        // 这样 18 台机器共用同一张底图、且槽位点击区域与视觉永远对齐。
        // blit 用 11 参数重载显式传入贴图实际尺寸（512×256），避免按 256×256 UV 采样被拉伸。
        gui.blit(GOD_GRID_BG, x, y, 0, 0, this.imageWidth, this.imageHeight,
                GOD_GRID_BG_W, GOD_GRID_BG_H);

        for (int i = 0; i < GodGridLayout.INPUT_COUNT; i++)
        {
            drawSlot(gui, x + GodGridLayout.inputX(i), y + GodGridLayout.INPUT_Y,
                    GodGridLayout.COLOR_INPUT_BORDER);
        }
        for (int i = 0; i < GodGridLayout.COLS; i++)
        {
            drawArrow(gui, x + GodGridLayout.columnX(i) + (GodGridLayout.SLOT - 8) / 2,
                    y + GodGridLayout.ARROW_Y);
        }
        for (int j = 0; j < GodGridLayout.OUTPUT_COUNT; j++)
        {
            drawSlot(gui, x + GodGridLayout.outputX(j), y + GodGridLayout.outputY(j),
                    GodGridLayout.COLOR_OUTPUT_BORDER);
        }
        // 原版流体罐位置（x=7,y=84）→ 1 格加速槽
        drawSlot(gui, x + GodGridLayout.accelX(), y + GodGridLayout.ACCEL_Y,
                GodGridLayout.COLOR_ACCEL_BORDER);

        // 左侧页签：面配置（只有真正有面配置的机器才画）
        if (this.menu.host.gridSupportsFaceConfig())
        {
            drawTab(gui, x + GodGridLayout.TAB_LEFT_X, y + GodGridLayout.TAB_FACE_Y,
                    Component.translatable("gui.godofthings.grid.face"), isHovered(mouseX, mouseY,
                            x + GodGridLayout.TAB_LEFT_X, y + GodGridLayout.TAB_FACE_Y), 0xFF6B6B6B);
        }
        // 右侧页签：AE / 排序
        drawTab(gui, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_AE_Y,
                Component.literal("AE"), isHovered(mouseX, mouseY,
                        x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_AE_Y),
                this.menu.isAeEnabled() ? 0xFF2F8F3F : 0xFF3A4048);
        drawTab(gui, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_SORT_Y,
                Component.translatable(GodGridSorting.modeLabelKey(this.menu.sortMode())),
                isHovered(mouseX, mouseY, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_SORT_Y),
                0xFF2F5F8F);
    }

    private void drawSlot(GuiGraphics gui, int sx, int sy, int border)
    {
        gui.fill(sx, sy, sx + GodGridLayout.SLOT, sy + GodGridLayout.SLOT, border);
        gui.fill(sx + 1, sy + 1, sx + GodGridLayout.SLOT - 1, sy + GodGridLayout.SLOT - 1,
                GodGridLayout.COLOR_SLOT_INNER);
    }

    /** 原版是 ProgressType.DOWN 的箭头贴图，这里用 4 段矩形近似同尺寸（8×20） */
    private void drawArrow(GuiGraphics gui, int ax, int ay)
    {
        int color = GodGridLayout.COLOR_ARROW;
        gui.fill(ax + 3, ay, ax + 5, ay + 12, color);
        gui.fill(ax + 2, ay + 12, ax + 6, ay + 14, color);
        gui.fill(ax + 1, ay + 14, ax + 7, ay + 16, color);
        gui.fill(ax, ay + 16, ax + 8, ay + 18, color);
    }

    /**
     * 自管 long 槽：图标 + 自绘数量数字。
     *
     * <p>原版 {@code AbstractContainerScreen#renderSlot} 会因「视图 count &gt; 1」在物品左上角另外画一个
     * 堆叠数字（视图被钳到 ≤99，例如 4.6M 的槽会画出「64」），与自绘数字重影，所以这里不复用原版。
     * 数字<b>紧跟</b>在图标之后画 —— 顺序即层次，「物品先、数字后」，数字永远在图标上层，
     * 不会被图标盖住（v5.17.x 之前数字在 {@code render()} 里另外单独画，顺序上仍会被后画的图标压住）。</p>
     *
     * <p>非 long 槽（加速槽 / 玩家物品栏）原样走原版；没有 long 槽的机器
     * {@link GodGridHost#gridHasLongAmounts()} 为 false，整个方法等价于原版，观感零变化。</p>
     *
     * <p>坐标：{@code AbstractContainerScreen#render} 调用本方法时 pose 已经平移过 {@code leftPos / topPos}，
     * 所以这里只用槽位本地坐标 {@code slot.x / slot.y}，<b>不能</b>再加 leftPos / topPos。</p>
     */
    @Override
    protected void renderSlot(GuiGraphics gui, Slot slot)
    {
        if (this.menu.host.gridHasLongAmounts() && slot instanceof LongSlotHandler.LongSlot longSlot)
        {
            // 先图标、后数字：数字压在图标上，不会被物品图标遮挡
            gui.renderItem(slot.getItem(), slot.x, slot.y);
            drawLongAmount(gui, longSlot);
            return;
        }
        super.renderSlot(gui, slot);
    }

    /**
     * 单个 long 槽的数量数字 —— 白色带阴影（{@code drawString(..., true)}）、<b>没有</b>半透明底衬，
     * 整段文本完整落在这一个槽的 16×16 内容区内。
     *
     * <p>宽度：缩放后可视宽度压到 ≤12px（原版堆叠数字大约就是这个宽度），
     * {@code scale = min(0.75, 12 / font.width(text))}；因此短文本固定 0.75 倍（高约 6px），
     * 长文本（99.9M 之类）按 12px 反推缩得更小，保证不越出槽框、不压相邻槽。</p>
     *
     * <p>位置：右对齐贴槽内右下角 —— 右边缘 {@code slot.x + 16}、底边 {@code slot.y + 16}，
     * 即 {@code x = slot.x + 16 - ceil(width * scale)}、{@code y = slot.y + 16 - ceil(8 * scale)}，
     * 让数字尽量只压图标右下角一小块，而不是横贯整个图标。</p>
     *
     * <p>{@link AbstractGodGridMenu#gridAmount(int)} &gt; 99 画紧凑数字（≥1e9 → 1.1B、≥1e6 → 1.1M、
     * ≥1000 → 1.2K）；1 &lt; 存量 ≤ 99 画普通数字（原版堆叠数字已被
     * {@link #renderSlot(GuiGraphics, Slot)} 关掉，这里不补就会出现「存量 50 却一个数字都没有」）；
     * ≤ 1 不画。</p>
     */
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
        // 以槽右下角为锚点整体缩到 0.85：比原版计数再小一档，观感更收敛（用户选定）
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
        // Mekanism 页签观感：深底 + 左上高光 + 右下阴影
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
        // 没有面配置的机器（资源三机 / 掉落机 / 怪蛋机）不画左侧页签，命中检测也一律不成立
        if (!this.menu.host.gridSupportsFaceConfig()
                && bx == this.leftPos + GodGridLayout.TAB_LEFT_X
                && by == this.topPos + GodGridLayout.TAB_FACE_Y)
        {
            return false;
        }
        int size = GodGridLayout.TAB_SIZE;
        return mouseX >= bx && mouseX < bx + size && mouseY >= by && mouseY < by + size;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        // 原版工厂界面标题居中在顶部
        // v5.28.0：标题画在铭牌上（铭牌中心与面板中心对齐，文字基线 y=6）
        gui.drawString(this.font, this.title,
                (this.imageWidth - this.font.width(this.title)) / 2, 6,
                GodGridLayout.COLOR_TITLE, false);
        // v5.28.0：物品栏现在画在白色大理石底板上，文字改用深色才看得清
        gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"),
                GodGridLayout.INVENTORY_X, GodGridLayout.INVENTORY_Y - 12,
                GodGridLayout.COLOR_TEXT_DARK, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int x = this.leftPos;
        int y = this.topPos;
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (isHovered(mx, my, x + GodGridLayout.TAB_LEFT_X, y + GodGridLayout.TAB_FACE_Y))
        {
            sendButton(AbstractGodGridMenu.BTN_FACE_CONFIG);
            return true;
        }
        if (isHovered(mx, my, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_AE_Y))
        {
            sendButton(AbstractGodGridMenu.BTN_AE_TOGGLE);
            return true;
        }
        if (isHovered(mx, my, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_SORT_Y))
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
        // long 槽的数量数字已在 renderSlot 里紧跟图标画完（见 drawLongAmount），这里不再单独画
        this.renderTooltip(gui, mouseX, mouseY);

        int x = this.leftPos;
        int y = this.topPos;

        if (isHovered(mouseX, mouseY, x + GodGridLayout.TAB_LEFT_X, y + GodGridLayout.TAB_FACE_Y))
        {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.godofthings.grid.face.tooltip"), mouseX, mouseY);
        }
        else if (isHovered(mouseX, mouseY, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_AE_Y))
        {
            gui.renderTooltip(this.font, Component.translatable(this.menu.isAeEnabled()
                    ? "gui.godofthings.grid.ae.on" : "gui.godofthings.grid.ae.off"), mouseX, mouseY);
        }
        else if (isHovered(mouseX, mouseY, x + GodGridLayout.TAB_RIGHT_X, y + GodGridLayout.TAB_SORT_Y))
        {
            gui.renderTooltip(this.font, Component.translatable("gui.godofthings.grid.sort.tooltip",
                    Component.translatable(GodGridSorting.modeLabelKey(this.menu.sortMode()))), mouseX, mouseY);
        }
        else if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()
                && mouseX >= x + GodGridLayout.ACCEL_X && mouseX < x + GodGridLayout.ACCEL_X + GodGridLayout.SLOT
                && mouseY >= y + GodGridLayout.ACCEL_Y && mouseY < y + GodGridLayout.ACCEL_Y + GodGridLayout.SLOT)
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
