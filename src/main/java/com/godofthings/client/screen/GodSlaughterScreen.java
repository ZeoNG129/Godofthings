package com.godofthings.client.screen;

import com.godofthings.block.entity.GodSlaughterBlockEntity;
import com.godofthings.client.GuiStep;
import com.godofthings.menu.GodSlaughterMenu;
import com.godofthings.network.SlaughterMessages;
import com.godofthings.util.NumberText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 神之砍杀屏幕：功能 / 存储两个板块（顶部按钮切换）。
 */
public class GodSlaughterScreen extends AbstractContainerScreen<GodSlaughterMenu>
{
    private static final int TAB_W = 58;
    private static final int TAB_H = 16;

    private static final String[] FACE_LABELS = { "—", "入", "出", "双" };

    public GodSlaughterScreen(GodSlaughterMenu menu, Inventory playerInv, Component title)
    {
        super(menu, playerInv, title);
        this.imageWidth = 256;
        this.imageHeight = 200;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;
        gui.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E22);

        // 顶部两个板块按钮
        drawButton(gui, x + 8, y + 8, TAB_W, TAB_H,
                Component.translatable("gui.godofthings.slaughter.tab_function"),
                this.menu.getCurrentTab() == GodSlaughterMenu.TAB_FUNCTION);
        drawButton(gui, x + 70, y + 8, TAB_W, TAB_H,
                Component.translatable("gui.godofthings.slaughter.tab_storage"),
                this.menu.getCurrentTab() == GodSlaughterMenu.TAB_STORAGE);

        if (this.menu.getCurrentTab() == GodSlaughterMenu.TAB_FUNCTION)
        {
            renderFunction(gui, x, y);
        }
        else
        {
            renderStorage(gui, x, y);
        }
    }

    private void renderFunction(GuiGraphics gui, int x, int y)
    {
        // 开关
        drawButton(gui, x + 8, y + 34, 110, 16,
                this.menu.isEnabled()
                        ? Component.translatable("gui.godofthings.slaughter.on")
                        : Component.translatable("gui.godofthings.slaughter.off"),
                this.menu.isEnabled());

        // 范围
        gui.drawString(this.font, Component.translatable("gui.godofthings.slaughter.range",
                this.menu.getRange()), x + 8, y + 60, 0xFFFFFF);
        drawButton(gui, x + 150, y + 56, 20, 16, Component.literal("-"), false);
        drawButton(gui, x + 172, y + 56, 20, 16, Component.literal("+"), false);

        // 抢夺
        drawButton(gui, x + 8, y + 80, 110, 16,
                this.menu.isLootingEnabled()
                        ? Component.translatable("gui.godofthings.slaughter.looting_on")
                        : Component.translatable("gui.godofthings.slaughter.looting_off"),
                this.menu.isLootingEnabled());
        gui.drawString(this.font, Component.translatable("gui.godofthings.slaughter.looting",
                this.menu.getLooting()), x + 124, y + 84, 0xFFFFFF);
        drawButton(gui, x + 150, y + 100, 20, 16, Component.literal("-"), false);
        drawButton(gui, x + 172, y + 100, 20, 16, Component.literal("+"), false);

        // 秒杀
        drawButton(gui, x + 8, y + 124, 110, 16,
                this.menu.isInstantKill()
                        ? Component.translatable("gui.godofthings.slaughter.instant_on")
                        : Component.translatable("gui.godofthings.slaughter.instant_off"),
                this.menu.isInstantKill());

        // 面配置
        renderFaceButtons(gui, x, y + 148);
    }

    private void renderStorage(GuiGraphics gui, int x, int y)
    {
        gui.drawString(this.font, Component.translatable("gui.godofthings.slaughter.storage",
                this.menu.getBlockEntity().getStorageCount()), x + 8, y + 34, 0x55FFFF);
        // 27 格槽位由 AbstractContainerScreen 渲染
        renderFaceButtons(gui, x, y + 148);
    }

    private void renderFaceButtons(GuiGraphics gui, int x, int y)
    {
        for (int i = 0; i < 6; i++)
        {
            int mode = this.menu.getFaceMode(i);
            drawButton(gui, x + 8 + i * 40, y, 36, 16,
                    Component.literal(FACE_LABELS[((mode % 4) + 4) % 4]), mode != 0);
        }
    }

    private void drawButton(GuiGraphics gui, int bx, int by, int w, int h, Component label, boolean active)
    {
        gui.fill(bx, by, bx + w, by + h, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, active ? 0xFF57B757 : 0xFF3A4048);
        gui.drawString(this.font, label, bx + (w - this.font.width(label)) / 2, by + 4, 0xFFFFFF);
    }

    /** 存储槽数量无上限，超过 999 用 K/M/G 等紧凑显示。 */
    @Override
    protected void renderSlot(GuiGraphics gui, Slot slot)
    {
        if (slot.index < GodSlaughterBlockEntity.STORAGE_SLOTS)
        {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && stack.getCount() > 999)
            {
                int x = slot.x;
                int y = slot.y;
                gui.pose().pushPose();
                gui.pose().translate(0.0F, 0.0F, 100.0F);
                gui.renderItem(stack, x, y, x + y * this.imageWidth);
                gui.renderItemDecorations(this.font, stack, x, y, NumberText.format(stack.getCount()));
                gui.pose().popPose();
                return;
            }
        }
        super.renderSlot(gui, slot);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        int tab = this.menu.getCurrentTab();

        // 板块切换
        if (inRect(relX, relY, 8, 8, TAB_W, TAB_H))
        {
            this.menu.setCurrentTab(GodSlaughterMenu.TAB_FUNCTION);
            return true;
        }
        if (inRect(relX, relY, 70, 8, TAB_W, TAB_H))
        {
            this.menu.setCurrentTab(GodSlaughterMenu.TAB_STORAGE);
            return true;
        }

        // 面配置按钮（两个板块都有，位于 y+148）
        for (int i = 0; i < 6; i++)
        {
            if (inRect(relX, relY, 8 + i * 40, 148, 36, 16))
            {
                sendButton(3 + i);
                return true;
            }
        }

        if (tab == GodSlaughterMenu.TAB_FUNCTION)
        {
            if (inRect(relX, relY, 8, 34, 110, 16))
            {
                this.menu.toggleEnabledLocal();
                sendButton(0);
                return true;
            }
            if (inRect(relX, relY, 150, 56, 20, 16))
            {
                adjustRange(-GuiStep.amount());
                return true;
            }
            if (inRect(relX, relY, 172, 56, 20, 16))
            {
                adjustRange(GuiStep.amount());
                return true;
            }
            if (inRect(relX, relY, 8, 80, 110, 16))
            {
                this.menu.toggleLootingEnabledLocal();
                sendButton(1);
                return true;
            }
            if (inRect(relX, relY, 150, 100, 20, 16))
            {
                adjustLooting(-GuiStep.amount());
                return true;
            }
            if (inRect(relX, relY, 172, 100, 20, 16))
            {
                adjustLooting(GuiStep.amount());
                return true;
            }
            if (inRect(relX, relY, 8, 124, 110, 16))
            {
                this.menu.toggleInstantKillLocal();
                sendButton(2);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void adjustRange(int delta)
    {
        int v = Math.max(0, Math.min(GodSlaughterBlockEntity.MAX_RANGE, this.menu.getRange() + delta));
        this.menu.setRangeLocal(v);
        SlaughterMessages.sendRange(delta);
    }

    private void adjustLooting(int delta)
    {
        int v = Math.max(0, Math.min(GodSlaughterBlockEntity.MAX_RANGE, this.menu.getLooting() + delta));
        this.menu.setLootingLocal(v);
        SlaughterMessages.sendLooting(delta);
    }

    private static boolean inRect(int x, int y, int bx, int by, int w, int h)
    {
        return x >= bx && x < bx + w && y >= by && y < by + h;
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
    public boolean isPauseScreen()
    {
        return false;
    }
}
