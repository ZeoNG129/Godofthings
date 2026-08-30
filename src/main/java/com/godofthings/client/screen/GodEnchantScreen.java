package com.godofthings.client.screen;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodEnchantMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/**
 * 神之附魔界面：放入任意物品，点选附魔、调等级、点应用。
 */
public class GodEnchantScreen extends AbstractContainerScreen<GodEnchantMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.tryBuild(Godofthings.MODID, "textures/gui/god_enchant.png");

    private static final int LIST_X = 8;
    private static final int LIST_Y = 58;
    private static final int LIST_W = 160;
    private static final int ROW_H = 14;
    private static final int VISIBLE_ROWS = 6;

    private static final int LEVEL_MINUS_X = 70;
    private static final int LEVEL_PLUS_X = 96;
    private static final int LEVEL_Y = 144;
    private static final int BTN_W = 24;
    private static final int BTN_H = 14;

    // 行2（y=162）：清除（左）+ 附魔 + 批量附魔（右，等比缩小并排）
    private static final int CLEAR_X = 8;
    private static final int CLEAR_Y = 162;
    private static final int CLEAR_W = 48;
    private static final int CLEAR_H = 16;

    private static final int APPLY_X = 112;
    private static final int APPLY_Y = 162;
    private static final int APPLY_W = 27;
    private static final int APPLY_H = 16;

    private static final int BATCH_X = 141;
    private static final int BATCH_Y = 162;
    private static final int BATCH_W = 27;
    private static final int BATCH_H = 16;

    private int scrollOffset = 0;

    public GodEnchantScreen(GodEnchantMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 250;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 附魔列表（随物品动态过滤：只显示该物品能附的魔咒）
        List<Enchantment> enchantments = this.menu.currentList();
        int maxScroll = Math.max(0, enchantments.size() - VISIBLE_ROWS);
        if (scrollOffset > maxScroll)
        {
            scrollOffset = maxScroll;
        }
        for (int i = 0; i < VISIBLE_ROWS; i++)
        {
            int index = scrollOffset + i;
            if (index >= enchantments.size())
            {
                break;
            }
            Enchantment ench = enchantments.get(index);
            int rowY = y + LIST_Y + i * ROW_H;
            boolean selected = index == this.menu.getSelectedIndex();
            gui.fill(x + LIST_X, rowY, x + LIST_X + LIST_W, rowY + ROW_H,
                    selected ? 0xFF3F5F3F : (i % 2 == 0 ? 0xFF3A3A3A : 0xFF333333));
            Component name = Component.translatable(ench.getDescriptionId());
            gui.drawString(this.font, name, x + LIST_X + 3, rowY + 3, selected ? 0xFFFFFF : 0xCCCCCC);
        }
        // 空物品时提示
        if (this.menu.getBlockEntity().getItemHandler().getStackInSlot(0).isEmpty())
        {
            gui.drawString(this.font, Component.translatable("gui.godofthings.enchant.empty"), x + 8, y + LIST_Y + 3, 0xCCCCCC);
        }

        // 等级
        Component levelText = Component.translatable("gui.godofthings.enchant.level",
                this.menu.getSelectedLevel(), this.menu.currentMaxLevel());
        gui.drawString(this.font, levelText, x + 8, y + 148, 0xFFFFFF);

        // 等级按钮（行1：y=144）
        gui.fill(x + LEVEL_MINUS_X, y + LEVEL_Y, x + LEVEL_MINUS_X + BTN_W, y + LEVEL_Y + BTN_H, 0xFF16181D);
        gui.fill(x + LEVEL_MINUS_X + 1, y + LEVEL_Y + 1, x + LEVEL_MINUS_X + BTN_W - 1, y + LEVEL_Y + BTN_H - 1, 0xFF4A4A4A);
        gui.drawString(this.font, Component.literal("-"), x + LEVEL_MINUS_X + 8, y + LEVEL_Y + 3, 0xFFFFFF);
        gui.fill(x + LEVEL_PLUS_X, y + LEVEL_Y, x + LEVEL_PLUS_X + BTN_W, y + LEVEL_Y + BTN_H, 0xFF16181D);
        gui.fill(x + LEVEL_PLUS_X + 1, y + LEVEL_Y + 1, x + LEVEL_PLUS_X + BTN_W - 1, y + LEVEL_Y + BTN_H - 1, 0xFF4A4A4A);
        gui.drawString(this.font, Component.literal("+"), x + LEVEL_PLUS_X + 8, y + LEVEL_Y + 3, 0xFFFFFF);

        // 应用按钮（行2 右侧，等比缩小）
        int bx = x + APPLY_X;
        int by = y + APPLY_Y;
        gui.fill(bx, by, bx + APPLY_W, by + APPLY_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + APPLY_W - 1, by + APPLY_H - 1, 0xFF3F7F3F);
        Component apply = Component.translatable("gui.godofthings.enchant.apply");
        gui.drawString(this.font, apply, bx + (APPLY_W - this.font.width(apply)) / 2, by + 4, 0xFFFFFF);

        // 清除附魔按钮（行2 左侧）
        int cx = x + CLEAR_X;
        int cy = y + CLEAR_Y;
        gui.fill(cx, cy, cx + CLEAR_W, cy + CLEAR_H, 0xFF16181D);
        gui.fill(cx + 1, cy + 1, cx + CLEAR_W - 1, cy + CLEAR_H - 1, 0xFF7F3F3F);
        Component clear = Component.translatable("gui.godofthings.enchant.clear");
        gui.drawString(this.font, clear, cx + (CLEAR_W - this.font.width(clear)) / 2, cy + 4, 0xFFFFFF);

        // 批量应用按钮（行2，与附魔等比并排）
        int bx2 = x + BATCH_X;
        int by2 = y + BATCH_Y;
        gui.fill(bx2, by2, bx2 + BATCH_W, by2 + BATCH_H, 0xFF16181D);
        gui.fill(bx2 + 1, by2 + 1, bx2 + BATCH_W - 1, by2 + BATCH_H - 1, 0xFF3F5F8F);
        Component batch = Component.translatable("gui.godofthings.enchant.batch");
        gui.drawString(this.font, batch, bx2 + (BATCH_W - this.font.width(batch)) / 2, by2 + 4, 0xFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY)
    {
        // 高对比度文字（不绘制物品栏标签：按钮区与物品栏之间无空隙，绘制会重叠）
        gui.drawString(this.font, this.title, 8, 6, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        List<Enchantment> enchantments = this.menu.currentList();

        // 列表点击选附魔
        if (relX >= LIST_X && relX < LIST_X + LIST_W && relY >= LIST_Y && relY < LIST_Y + VISIBLE_ROWS * ROW_H)
        {
            int index = (relY - LIST_Y) / ROW_H + scrollOffset;
            if (index >= 0 && index < enchantments.size())
            {
                sendButton(10 + index);
            }
            return true;
        }

        if (relX >= LEVEL_MINUS_X && relX < LEVEL_MINUS_X + BTN_W && relY >= LEVEL_Y && relY < LEVEL_Y + BTN_H)
        {
            sendButton(2);
            return true;
        }
        if (relX >= LEVEL_PLUS_X && relX < LEVEL_PLUS_X + BTN_W && relY >= LEVEL_Y && relY < LEVEL_Y + BTN_H)
        {
            sendButton(1);
            return true;
        }
        if (relX >= CLEAR_X && relX < CLEAR_X + CLEAR_W && relY >= CLEAR_Y && relY < CLEAR_Y + CLEAR_H)
        {
            sendButton(3);
            return true;
        }
        if (relX >= APPLY_X && relX < APPLY_X + APPLY_W && relY >= APPLY_Y && relY < APPLY_Y + APPLY_H)
        {
            sendButton(0);
            return true;
        }

        if (relX >= BATCH_X && relX < BATCH_X + BATCH_W && relY >= BATCH_Y && relY < BATCH_Y + BATCH_H)
        {
            sendButton(4);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        List<Enchantment> enchantments = this.menu.currentList();
        int maxScroll = Math.max(0, enchantments.size() - VISIBLE_ROWS);
        int next = (int) (scrollOffset - delta);
        scrollOffset = Math.max(0, Math.min(maxScroll, next));
        return true;
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
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
