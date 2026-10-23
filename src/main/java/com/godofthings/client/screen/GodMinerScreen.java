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
 * <p><b>面板 352×256</b>（原画金框实测 988×718，宽高比 1.376，故选 352×256 保持同一比例）。
 * 底图 {@code textures/gui/god_miner.png} 画的是 512×512 画布上左上角 352×256 的那一块，
 * blit 必须用 11 参数重载显式传贴图真实尺寸，否则会按 256×256 采样被拉伸。</p>
 *
 * <p><b>槽位</b>：本机<b>没有输入/输出格</b> —— 挖掘产物直接进内置无限储存并推 AE，
 * 所以界面上只有 1 个神之加速槽（左侧熔岩柱位置）+ 玩家物品栏 9×3 + 快捷栏 9。
 * 底图上那条青色槽位带是纯装饰（原画里的「产物展示带」），不挂任何槽位。</p>
 *
 * <p><b>按钮</b>：6 个范围 ±（左 3 右 3）、3 个符文按钮（时运 / 精准采集 / 面配置）、
 * 中央范围读数、右侧 AE 接入核心。全部按底图的原画位置摆放。</p>
 */
public class GodMinerScreen extends AbstractContainerScreen<GodMinerMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "textures/gui/god_miner.png");
    /** 贴图实际像素尺寸（512×512 画布，面板画在左上角） */
    private static final int TEX_W = 210;
    private static final int TEX_H = 210;

    private static final String[] RADIUS_LABELS = { "-100", "-10", "-1", "+1", "+10", "+100" };
    private static final int[] RADIUS_DELTAS = { -100, -10, -1, 1, 10, 100 };

    /** 神之加速槽（画在左侧熔岩柱顶端的位置） */
    private static final int ACCEL_X = 14;
    private static final int ACCEL_Y = 24;

    // ---- 范围调节：左侧 3 个（-100/-10/-1）、右侧 3 个（+1/+10/+100） ----
    private static final int RANGE_BTN_W = 34;
    private static final int RANGE_BTN_H = 16;
    private static final int RANGE_L_X = 20;
    private static final int RANGE_R_X = 156;
    private static final int RANGE_BTN_Y0 = 24;
    private static final int RANGE_BTN_PITCH = 20;

    /** 中央范围读数面板 */
    private static final int RANGE_PANEL_X = 60;
    private static final int RANGE_PANEL_Y = 24;
    private static final int RANGE_PANEL_W = 90;
    private static final int RANGE_PANEL_H = 26;

    // ---- 3 个符文按钮：时运 / 精准采集 / 面配置 ----
    private static final int RUNE_R = 17;
    private static final int RUNE_Y = 92;
    private static final int[] RUNE_X = { 46, 105, 164 };

    /** 右侧 AE 核心（点它切换 AE 接入） */
    private static final int AE_CORE_X = 189;
    private static final int AE_CORE_Y = 31;
    private static final int AE_CORE_R = 13;

    /** 玩家物品栏（底图 9×4 网格的精确位置） */
    private static final int INV_X = 92;
    private static final int INV_Y = 172;

    public GodMinerScreen(GodMinerMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 210;
        this.imageHeight = 210;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;

        // 11 参数重载：显式给贴图真实尺寸 512×512，避免按 256×256 采样被拉伸
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

        // ---- AE 核心 ----
        boolean aeOn = this.menu.isAeEnabled();
        boolean aeHover = isInCircle(mouseX, mouseY, x + AE_CORE_X, y + AE_CORE_Y, AE_CORE_R);
        int coreGlow = aeOn ? 0xFF9EF6FF : 0xFF43505C;
        gui.fill(x + AE_CORE_X - 9, y + AE_CORE_Y - 9, x + AE_CORE_X + 9, y + AE_CORE_Y + 9,
                aeHover ? 0xFFE8FFFF : coreGlow);
        gui.fill(x + AE_CORE_X - 6, y + AE_CORE_Y - 6, x + AE_CORE_X + 6, y + AE_CORE_Y + 6,
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

    /** 符文按钮：金环 + 内部暗底；开启时环变亮青色 */
    private void drawRuneRing(GuiGraphics gui, int cx, int cy, int r, boolean hover, boolean on, int index)
    {
        int ring = on ? 0xFF7DF0FF : (hover ? 0xFFFFF0B0 : 0xFFC8983E);
        gui.fill(cx - r, cy - r, cx + r, cy + r, ring);
        gui.fill(cx - r + 2, cy - r + 2, cx + r - 2, cy + r - 2, 0xFF121828);
        // 中间一个小符号（不引贴图，用两条短线区分三个按钮）
        int glyph = on ? 0xFF7DF0FF : 0xFFE0C070;
        if (index == 0)
        {
            gui.fill(cx - 4, cy - 1, cx + 4, cy, glyph);
            gui.fill(cx - 1, cy - 4, cx, cy + 4, glyph);
        }
        else if (index == 1)
        {
            gui.fill(cx - 4, cy - 4, cx + 4, cy - 3, glyph);
            gui.fill(cx - 4, cy + 3, cx + 4, cy + 4, glyph);
            gui.fill(cx - 4, cy - 4, cx - 3, cy + 4, glyph);
            gui.fill(cx + 3, cy - 4, cx + 4, cy + 4, glyph);
        }
        else
        {
            gui.fill(cx - 4, cy - 3, cx + 4, cy - 2, glyph);
            gui.fill(cx - 4, cy + 2, cx + 4, cy + 3, glyph);
            gui.fill(cx - 4, cy - 3, cx - 3, cy + 3, glyph);
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
        // 标题画在拱形铭牌正中（铭牌 y 0..26，文字基线 9）
        gui.drawString(this.font, this.title,
                (this.imageWidth - this.font.width(this.title)) / 2, 9,
                0xFFFFE6A8, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        GodMinerBlockEntity be = this.menu.getBlockEntity();

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
