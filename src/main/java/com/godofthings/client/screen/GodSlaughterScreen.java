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
 * 神之砍杀屏幕：功能 / 存储 / 经验三个板块（顶部按钮切换），面配置由顶部按钮打开独立界面。
 * 玩家物品栏固定在底部（y=84 起），各板块内容在 y=24-78 之间，互不重合。
 */
public class GodSlaughterScreen extends AbstractContainerScreen<GodSlaughterMenu>
{
    private static final int TAB_W = 36;
    private static final int TAB_H = 14;

    public GodSlaughterScreen(GodSlaughterMenu menu, Inventory playerInv, Component title)
    {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;
        gui.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E22);

        // 顶部：三个板块按钮 + 面配置按钮
        drawButton(gui, x + 8, y + 4, TAB_W, TAB_H,
                Component.translatable("gui.godofthings.slaughter.tab_function"),
                this.menu.getCurrentTab() == GodSlaughterMenu.TAB_FUNCTION);
        drawButton(gui, x + 46, y + 4, TAB_W, TAB_H,
                Component.translatable("gui.godofthings.slaughter.tab_storage"),
                this.menu.getCurrentTab() == GodSlaughterMenu.TAB_STORAGE);
        drawButton(gui, x + 84, y + 4, TAB_W, TAB_H,
                Component.translatable("gui.godofthings.slaughter.tab_experience"),
                this.menu.getCurrentTab() == GodSlaughterMenu.TAB_EXPERIENCE);
        drawButton(gui, x + 122, y + 4, 46, TAB_H,
                Component.translatable("gui.godofthings.face_config"), false);

        int tab = this.menu.getCurrentTab();
        if (tab == GodSlaughterMenu.TAB_FUNCTION)
        {
            renderFunction(gui, x, y);
        }
        else if (tab == GodSlaughterMenu.TAB_EXPERIENCE)
        {
            renderExperience(gui, x, y);
        }
        // 存储板块的槽位由 AbstractContainerScreen 渲染
    }

    private void renderFunction(GuiGraphics gui, int x, int y)
    {
        // 开关 + 秒杀
        drawButton(gui, x + 8, y + 24, 76, 16,
                this.menu.isEnabled()
                        ? Component.translatable("gui.godofthings.slaughter.on")
                        : Component.translatable("gui.godofthings.slaughter.off"),
                this.menu.isEnabled());
        drawButton(gui, x + 88, y + 24, 76, 16,
                this.menu.isInstantKill()
                        ? Component.translatable("gui.godofthings.slaughter.instant_on")
                        : Component.translatable("gui.godofthings.slaughter.instant_off"),
                this.menu.isInstantKill());

        // 范围
        gui.drawString(this.font, Component.translatable("gui.godofthings.slaughter.range",
                this.menu.getRange()), x + 8, y + 50, 0xFFFFFF);
        drawButton(gui, x + 96, y + 46, 18, 16, Component.literal("-"), false);
        drawButton(gui, x + 116, y + 46, 18, 16, Component.literal("+"), false);

        // 抢夺
        drawButton(gui, x + 8, y + 66, 76, 16,
                this.menu.isLootingEnabled()
                        ? Component.translatable("gui.godofthings.slaughter.looting_on")
                        : Component.translatable("gui.godofthings.slaughter.looting_off"),
                this.menu.isLootingEnabled());
        gui.drawString(this.font, Component.translatable("gui.godofthings.slaughter.looting",
                this.menu.getLooting()), x + 136, y + 70, 0xFFFFFF);
        drawButton(gui, x + 96, y + 66, 18, 16, Component.literal("-"), false);
        drawButton(gui, x + 116, y + 66, 18, 16, Component.literal("+"), false);
    }

    private void renderExperience(GuiGraphics gui, int x, int y)
    {
        gui.drawString(this.font, Component.translatable("gui.godofthings.slaughter.xp_stored",
                this.menu.getExperienceLevel()), x + 8, y + 26, 0x55FF55);
        drawButton(gui, x + 8, y + 48, 40, 16,
                Component.translatable("gui.godofthings.slaughter.xp_take_1"), false);
        drawButton(gui, x + 52, y + 48, 40, 16,
                Component.translatable("gui.godofthings.slaughter.xp_take_10"), false);
        drawButton(gui, x + 96, y + 48, 40, 16,
                Component.translatable("gui.godofthings.slaughter.xp_take_100"), false);
        drawButton(gui, x + 8, y + 66, 40, 16,
                Component.translatable("gui.godofthings.slaughter.xp_take_all"), false);
    }

    private void drawButton(GuiGraphics gui, int bx, int by, int w, int h, Component label, boolean active)
    {
        gui.fill(bx, by, bx + w, by + h, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, active ? 0xFF57B757 : 0xFF3A4048);
        gui.drawString(this.font, label, bx + (w - this.font.width(label)) / 2, by + 3, 0xFFFFFF);
    }

    /** 存储槽数量无上限，超过 999 用 K/M/G 等紧凑显示（槽位是否渲染由 isActive 控制，与板块独立）。 */
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

        // 顶部 tab + 面配置
        if (inRect(relX, relY, 8, 4, TAB_W, TAB_H))
        {
            this.menu.setCurrentTab(GodSlaughterMenu.TAB_FUNCTION);
            return true;
        }
        if (inRect(relX, relY, 46, 4, TAB_W, TAB_H))
        {
            this.menu.setCurrentTab(GodSlaughterMenu.TAB_STORAGE);
            return true;
        }
        if (inRect(relX, relY, 84, 4, TAB_W, TAB_H))
        {
            this.menu.setCurrentTab(GodSlaughterMenu.TAB_EXPERIENCE);
            return true;
        }
        if (inRect(relX, relY, 122, 4, 46, TAB_H))
        {
            sendButton(3);
            return true;
        }

        if (tab == GodSlaughterMenu.TAB_FUNCTION)
        {
            if (inRect(relX, relY, 8, 24, 76, 16))
            {
                this.menu.toggleEnabledLocal();
                sendButton(0);
                return true;
            }
            if (inRect(relX, relY, 88, 24, 76, 16))
            {
                this.menu.toggleInstantKillLocal();
                sendButton(2);
                return true;
            }
            if (inRect(relX, relY, 96, 46, 18, 16))
            {
                adjustRange(-GuiStep.amount());
                return true;
            }
            if (inRect(relX, relY, 116, 46, 18, 16))
            {
                adjustRange(GuiStep.amount());
                return true;
            }
            if (inRect(relX, relY, 8, 66, 76, 16))
            {
                this.menu.toggleLootingEnabledLocal();
                sendButton(1);
                return true;
            }
            if (inRect(relX, relY, 96, 66, 18, 16))
            {
                adjustLooting(-GuiStep.amount());
                return true;
            }
            if (inRect(relX, relY, 116, 66, 18, 16))
            {
                adjustLooting(GuiStep.amount());
                return true;
            }
        }
        else if (tab == GodSlaughterMenu.TAB_EXPERIENCE)
        {
            if (inRect(relX, relY, 8, 48, 40, 16))
            {
                sendButton(4);
                return true;
            }
            if (inRect(relX, relY, 52, 48, 40, 16))
            {
                sendButton(5);
                return true;
            }
            if (inRect(relX, relY, 96, 48, 40, 16))
            {
                sendButton(6);
                return true;
            }
            if (inRect(relX, relY, 8, 66, 40, 16))
            {
                sendButton(7);
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
