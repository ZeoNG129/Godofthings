package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodMinerBlockEntity;
import com.godofthings.menu.GodMinerMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之掘机界面 —— 按《撼地穿霄掘机_UI设计原画》重做（v5.29.0）。
 *
 * 神之掘机界面 —— 面板 224×248，全部元素各自分道，互不重叠（v5.31.3）。
 *
 * <p>本机<b>没有输入/输出格</b> —— 挖掘产物直接进内置无限储存并推 AE，所以界面上只有
 * 1 个神之加速槽 + 玩家物品栏 9×3 + 快捷栏 9。</p>
 *
 * <p><b>v5.31.3 修复的布局事故</b>：v5.31.2 把面板压到 210×210 时，几个元素直接叠在一起 ——</p>
 * <ul>
 *   <li>左侧范围按钮 {@code x=20..54} 压住加速槽 {@code (14,24)}（截图里物品被按钮盖住）；</li>
 *   <li>右侧范围按钮 {@code x=156..190} 压住 AE 核心 {@code x=189}；</li>
 *   <li>符文按钮 {@code r=17} 用 {@code gui.fill} 画成了 34×34 实心方块，比 ± 按钮大两倍。</li>
 * </ul>
 * <p>现在：面板 224×248，元素分行分列；符文按钮缩到 r=13，并且改用<b>逐行扫描填圆</b>
 * （{@link #fillCircle}）而不是 {@code fill} 矩形，圆就是圆。</p>
 *
 * <p><b>v7.19.0 补上「开始 / 停止」按钮</b>：服务端 {@code GodMinerMenu#clickMenuButton} 的
 * {@code buttonId == 0} 分支一直都在（翻转 {@code running}），但界面从来没画过这个按钮，
 * 玩家只能看到机器永远不动。按钮位置按要求取「范围读数下方、两组范围调节之间、精准采集按钮上方」。</p>
 */
public class GodMinerScreen extends AbstractContainerScreen<GodMinerMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "textures/gui/god_miner.png");
    /** 贴图实际像素尺寸（与面板一致，1:1 采样，不会拉伸） */
    private static final int TEX_W = 224;
    private static final int TEX_H = 248;

    private static final String[] RADIUS_LABELS = { "-100", "-10", "-1", "+1", "+10", "+100" };
    private static final int[] RADIUS_DELTAS = { -100, -10, -1, 1, 10, 100 };

    /** 神之加速槽（左上角） */
    private static final int ACCEL_X = 11;
    private static final int ACCEL_Y = 25;

    // ---- 范围调节：左侧 3 个（-100/-10/-1）、右侧 3 个（+1/+10/+100） ----
    private static final int RANGE_BTN_W = 46;
    private static final int RANGE_BTN_H = 16;
    private static final int RANGE_L_X = 13;
    private static final int RANGE_R_X = 165;
    private static final int RANGE_BTN_Y0 = 60;
    private static final int RANGE_BTN_PITCH = 18;

    /** 中央范围读数面板 */
    private static final int RANGE_PANEL_X = 68;
    private static final int RANGE_PANEL_Y = 24;
    private static final int RANGE_PANEL_W = 88;
    private static final int RANGE_PANEL_H = 24;

    // ---- 3 个符文按钮：时运 / 精准采集 / 面配置 ----
    private static final int RUNE_R = 13;
    private static final int RUNE_Y = 126;
    private static final int[] RUNE_X = { 56, 112, 168 };

    /**
     * 开始 / 停止 按钮（v7.19.0 新增）。
     *
     * <p>位置按要求取「范围读数下方、左右两组范围调节之间、精准采集按钮上方」：
     * 横向与中央读数面板 {@link #RANGE_PANEL_X} 左对齐、同宽 88。</p>
     *
     * <p>纵向：范围按钮左列占到 {@code 60 + 2*18 + 16 = 112}，符文按钮上沿（中心 126、半径 13）是 113；
     * 中间只有 1px 富余，塞不下 88×20 的块。因此按<b>「中间列夹在左右两组之间」</b>的本意，
     * 让按钮落在左右按钮的纵向跨度 {@code 60..112} 之内 —— 该区间中央列是空的（读数面板到 48 结束），
     * 取 {@code y=68..88}，左右留 6px 呼吸、与两组范围按钮视觉齐平。</p>
     */
    private static final int START_BTN_X = 68;
    private static final int START_BTN_Y = 68;
    private static final int START_BTN_W = 88;
    private static final int START_BTN_H = 20;

    /** 右侧 AE 核心（点它切换 AE 接入） */
    private static final int AE_CORE_X = 204;
    private static final int AE_CORE_Y = 36;
    private static final int AE_CORE_R = 12;

    /** 玩家物品栏（背包 3 行 + 快捷栏，格距 18） */
    private static final int INV_X = 32;
    private static final int INV_Y = 166;
    private static final int INV_PITCH = 18;

    public GodMinerScreen(GodMinerMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 224;
        this.imageHeight = 248;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;

        // 11 参数重载：显式给贴图真实尺寸，避免按 256×256 采样被拉伸
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        GodMinerBlockEntity be = this.menu.getBlockEntity();

        // ---- 神之加速槽：只画内框，金色外圈底图上已有 ----
        int ax = x + ACCEL_X;
        int ay = y + ACCEL_Y;
        gui.fill(ax - 1, ay - 1, ax + 17, ay + 17, 0xFF1A1006);
        gui.fill(ax, ay, ax + 16, ay + 16, 0xFF241A0A);

        // ---- 范围读数 ----
        Component rangeText = Component.translatable("gui.godofthings.miner.radius_value", be.getRadius());
        gui.drawString(this.font, rangeText,
                x + RANGE_PANEL_X + (RANGE_PANEL_W - this.font.width(rangeText)) / 2,
                y + RANGE_PANEL_Y + 9, 0xFFFFD24A, false);

        // ---- 6 个范围按钮 ----
        for (int i = 0; i < 6; i++)
        {
            boolean left = i < 3;
            int bx = x + (left ? RANGE_L_X : RANGE_R_X);
            int by = y + RANGE_BTN_Y0 + (left ? i : i - 3) * RANGE_BTN_PITCH;
            boolean hover = mouseX >= bx && mouseX < bx + RANGE_BTN_W
                    && mouseY >= by && mouseY < by + RANGE_BTN_H;
            drawGoldButton(gui, bx, by, RANGE_BTN_W, RANGE_BTN_H, hover);
            Component label = Component.literal(RADIUS_LABELS[i]);
            gui.drawString(this.font, label,
                    bx + (RANGE_BTN_W - this.font.width(label)) / 2,
                    by + (RANGE_BTN_H - 8) / 2, hover ? 0xFFFFFFE0 : 0xFFD9C58A, false);
        }

        // ---- 开始 / 停止 按钮（范围读数下方、夹在左右两组范围调节之间） ----
        boolean running = be.isRunning();
        int sbx = x + START_BTN_X;
        int sby = y + START_BTN_Y;
        boolean startHover = mouseX >= sbx && mouseX < sbx + START_BTN_W
                && mouseY >= sby && mouseY < sby + START_BTN_H;
        drawStartButton(gui, sbx, sby, running, startHover);
        Component startLabel = Component.translatable(running
                ? "gui.godofthings.miner.stop" : "gui.godofthings.miner.start");
        gui.drawString(this.font, startLabel,
                sbx + (START_BTN_W - this.font.width(startLabel)) / 2,
                sby + (START_BTN_H - 8) / 2,
                startHover ? 0xFFFFFFE0 : (running ? 0xFFFF9A6A : 0xFF9EF6FF), false);

        // ---- 3 个符文按钮 ----
        boolean fortune = be.getFortuneLevel() == 3;
        boolean silk = be.isSilkTouch();
        for (int i = 0; i < 3; i++)
        {
            int cx = x + RUNE_X[i];
            int cy = y + RUNE_Y;
            boolean hover = isInCircle(mouseX, mouseY, cx, cy, RUNE_R);
            boolean on = (i == 0 && fortune) || (i == 1 && silk);
            drawRuneRing(gui, cx, cy, RUNE_R, hover, on, i);
        }

        // ---- AE 核心（同心圆，不是方块） ----
        boolean aeOn = this.menu.isAeEnabled();
        boolean aeHover = isInCircle(mouseX, mouseY, x + AE_CORE_X, y + AE_CORE_Y, AE_CORE_R);
        int coreGlow = aeOn ? 0xFF9EF6FF : 0xFF43505C;
        fillCircle(gui, x + AE_CORE_X, y + AE_CORE_Y, AE_CORE_R,
                aeHover ? 0xFFE8FFFF : coreGlow);
        fillCircle(gui, x + AE_CORE_X, y + AE_CORE_Y, AE_CORE_R - 3,
                aeOn ? 0xFF37C8E8 : 0xFF2A3540);
        Component aeLabel = Component.literal("AE");
        gui.drawString(this.font, aeLabel,
                x + AE_CORE_X - this.font.width(aeLabel) / 2, y + AE_CORE_Y - 4,
                aeOn ? 0xFF06222E : 0xFF8A98A4, false);
    }

    /** 金色按钮：底图上已画出金框，这里只画内凹面 + hover 高光 */
    private void drawGoldButton(GuiGraphics gui, int bx, int by, int w, int h, boolean hover)
    {
        gui.fill(bx + 2, by + 2, bx + w - 2, by + h - 2, hover ? 0xFF3A4560 : 0xFF1E2438);
        if (hover)
        {
            gui.fill(bx + 2, by + 2, bx + w - 2, by + 3, 0xFF7A8AA8);
        }
    }

    /**
     * 开始 / 停止 按钮外观：运行中为暖橙（提示「点它会停」），停止时为冷青（提示「点它会开始」）。
     *
     * <p>用几何绘制而非贴图，与旁边 {@link #drawGoldButton} / {@link #drawRuneRing} 风格一致。</p>
     */
    private void drawStartButton(GuiGraphics gui, int bx, int by, boolean running, boolean hover)
    {
        int outer = hover ? 0xFF8FA6C8 : 0xFFC8983E;
        int inner = running
                ? (hover ? 0xFF7A2A10 : 0xFF5E1F0C)
                : (hover ? 0xFF10406A : 0xFF0C2A46);
        gui.fill(bx, by, bx + START_BTN_W, by + START_BTN_H, outer);
        gui.fill(bx + 1, by + 1, bx + START_BTN_W - 1, by + START_BTN_H - 1, inner);
        if (hover)
        {
            gui.fill(bx + 1, by + 1, bx + START_BTN_W - 1, by + 2, 0xFF7A8AA8);
        }
    }

    /**
     * 逐行扫描填一个实心圆 —— {@code GuiGraphics} 只有 {@code fill} 矩形，
     * 直接拿矩形当圆画出来就是「大方块」（v5.31.2 的符文按钮正是这么废掉的）。
     * 每行按半弦长 {@code sqrt(r² - dy²)} 求左右边界，再填一条横向像素带。
     */
    private static void fillCircle(GuiGraphics gui, int cx, int cy, int r, int color)
    {
        if (r <= 0)
        {
            return;
        }
        for (int dy = -r; dy <= r; dy++)
        {
            int half = (int) Math.sqrt((double) (r * r - dy * dy));
            if (half <= 0)
            {
                continue;
            }
            gui.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    /** 符文按钮：金环 + 内部暗底；开启时环变亮青色。环与底都是真圆。 */
    private void drawRuneRing(GuiGraphics gui, int cx, int cy, int r, boolean hover, boolean on, int index)
    {
        int ring = on ? 0xFF7DF0FF : (hover ? 0xFFFFF0B0 : 0xFFC8983E);
        fillCircle(gui, cx, cy, r, ring);
        fillCircle(gui, cx, cy, r - 2, 0xFF121828);
        // 中间一个小符号（不引贴图，用几何图形区分三个按钮）
        int glyph = on ? 0xFF7DF0FF : 0xFFE0C070;
        int g = Math.max(3, r / 3);
        if (index == 0)
        {
            // 时运：加号
            gui.fill(cx - g, cy, cx + g + 1, cy + 1, glyph);
            gui.fill(cx, cy - g, cx + 1, cy + g + 1, glyph);
        }
        else if (index == 1)
        {
            // 精准采集：空心方框
            gui.fill(cx - g, cy - g, cx + g + 1, cy - g + 1, glyph);
            gui.fill(cx - g, cy + g, cx + g + 1, cy + g + 1, glyph);
            gui.fill(cx - g, cy - g, cx - g + 1, cy + g + 1, glyph);
            gui.fill(cx + g, cy - g, cx + g + 1, cy + g + 1, glyph);
        }
        else
        {
            // 面配置：齿轮感的双横线
            gui.fill(cx - g, cy - g / 2 - 1, cx + g + 1, cy - g / 2, glyph);
            gui.fill(cx - g, cy + g / 2, cx + g + 1, cy + g / 2 + 1, glyph);
            gui.fill(cx - g, cy - g / 2 - 1, cx - g + 1, cy + g / 2 + 1, glyph);
        }
    }

    private static boolean isInCircle(int mx, int my, int cx, int cy, int r)
    {
        int dx = mx - cx;
        int dy = my - cy;
        return dx * dx + dy * dy <= r * r;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        // 标题画在铭牌正中（铭牌 y 2..17，文字基线 6）
        gui.drawString(this.font, this.title,
                (this.imageWidth - this.font.width(this.title)) / 2, 6,
                0xFFFFE6A8, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        GodMinerBlockEntity be = this.menu.getBlockEntity();

        // 开始 / 停止
        if (relX >= START_BTN_X && relX < START_BTN_X + START_BTN_W
                && relY >= START_BTN_Y && relY < START_BTN_Y + START_BTN_H)
        {
            be.setRunning(!be.isRunning()); // 乐观更新，服务端权威值随 DataSlot 收敛
            sendButton(0);
            return true;
        }

        // AE 核心
        if (isInCircle(relX, relY, AE_CORE_X, AE_CORE_Y, AE_CORE_R))
        {
            sendButton(9);
            return true;
        }

        // 3 个符文按钮：时运 / 精准采集 / 面配置
        for (int i = 0; i < 3; i++)
        {
            if (isInCircle(relX, relY, RUNE_X[i], RUNE_Y, RUNE_R))
            {
                if (i == 0)
                {
                    be.toggleFortune(); // 乐观更新
                    sendButton(7);
                }
                else if (i == 1)
                {
                    be.toggleSilkTouch();
                    sendButton(8);
                }
                else
                {
                    sendButton(10);
                }
                return true;
            }
        }

        // 范围 ± 按钮
        for (int i = 0; i < 6; i++)
        {
            boolean left = i < 3;
            int bx = left ? RANGE_L_X : RANGE_R_X;
            int by = RANGE_BTN_Y0 + (left ? i : i - 3) * RANGE_BTN_PITCH;
            if (relX >= bx && relX < bx + RANGE_BTN_W && relY >= by && relY < by + RANGE_BTN_H)
            {
                be.setRadius(be.getRadius() + RADIUS_DELTAS[i]); // 乐观更新
                sendButton(i + 1);
                return true;
            }
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
        int relX = mouseX - x;
        int relY = mouseY - y;

        // 加速槽 hover 提示（槽内有物品时让位给物品 tooltip）
        if (isHovering(ACCEL_X, ACCEL_Y, 16, 16, mouseX, mouseY)
                && (this.hoveredSlot == null || !this.hoveredSlot.hasItem()))
        {
            gui.renderTooltip(this.font,
                    Component.translatable("tooltip.godofthings.miner_accelerator"), mouseX, mouseY);
        }
        else if (relX >= START_BTN_X && relX < START_BTN_X + START_BTN_W
                && relY >= START_BTN_Y && relY < START_BTN_Y + START_BTN_H)
        {
            gui.renderTooltip(this.font, Component.translatable(this.menu.getBlockEntity().isRunning()
                    ? "gui.godofthings.miner.stop.tooltip" : "gui.godofthings.miner.start.tooltip"), mouseX, mouseY);
        }
        else if (isInCircle(relX, relY, AE_CORE_X, AE_CORE_Y, AE_CORE_R))
        {
            gui.renderTooltip(this.font, Component.translatable(this.menu.isAeEnabled()
                    ? "gui.godofthings.miner.ae.on" : "gui.godofthings.miner.ae.off"), mouseX, mouseY);
        }
        else if (isInCircle(relX, relY, RUNE_X[0], RUNE_Y, RUNE_R))
        {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.godofthings.miner.fortune.tooltip"), mouseX, mouseY);
        }
        else if (isInCircle(relX, relY, RUNE_X[1], RUNE_Y, RUNE_R))
        {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.godofthings.miner.silk_touch.tooltip"), mouseX, mouseY);
        }
        else if (isInCircle(relX, relY, RUNE_X[2], RUNE_Y, RUNE_R))
        {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.godofthings.miner.face_config"), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
