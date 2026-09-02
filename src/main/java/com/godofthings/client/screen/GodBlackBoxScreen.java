package com.godofthings.client.screen;

import com.godofthings.item.BlackBoxData;
import com.godofthings.menu.GodBlackBoxMenu;
import com.godofthings.util.NumberText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 神之黑盒配置屏幕：复用原版投掷器（dispenser）贴图的 3×3 槽位区 + 左侧开关按钮。
 */
public class GodBlackBoxScreen extends AbstractContainerScreen<GodBlackBoxMenu>
{
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

    private static final int SWITCH_X = 8;
    private static final int SWITCH_Y = 17;
    private static final int SWITCH_W = 48;
    private static final int SWITCH_H = 20;

    private static final int MODE_X = 8;
    private static final int MODE_Y = 41;
    private static final int MODE_W = 48;
    private static final int MODE_H = 20;

    public GodBlackBoxScreen(GodBlackBoxMenu menu, Inventory playerInv, Component title)
    {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    /** 黑盒过滤槽（白名单模式兼作存储）数量无上限，超过 1000 用 K/M/G 等单位紧凑显示。 */
    @Override
    protected void renderSlot(GuiGraphics gui, Slot slot)
    {
        if (slot.index < BlackBoxData.FILTER_SLOTS)
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
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 开关按钮
        boolean enabled = this.menu.isEnabled();
        int bx = x + SWITCH_X;
        int by = y + SWITCH_Y;
        gui.fill(bx, by, bx + SWITCH_W, by + SWITCH_H, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + SWITCH_W - 1, by + SWITCH_H - 1, enabled ? 0xFF57B757 : 0xFF3A4048);
        Component label = Component.translatable(enabled
                ? "gui.godofthings.black_box.enabled"
                : "gui.godofthings.black_box.disabled");
        gui.drawString(this.font, label, bx + (SWITCH_W - this.font.width(label)) / 2, by + 6, 0xFFFFFF);

        // 过滤模式按钮（白名单 / 黑名单）
        boolean whitelist = this.menu.isWhitelistMode();
        int mx = x + MODE_X;
        int my = y + MODE_Y;
        gui.fill(mx, my, mx + MODE_W, my + MODE_H, 0xFF16181D);
        gui.fill(mx + 1, my + 1, mx + MODE_W - 1, my + MODE_H - 1, whitelist ? 0xFF57B757 : 0xFFB7573A);
        Component modeLabel = Component.translatable(whitelist
                ? "gui.godofthings.black_box.mode_whitelist"
                : "gui.godofthings.black_box.mode_blacklist");
        gui.drawString(this.font, modeLabel, mx + (MODE_W - this.font.width(modeLabel)) / 2, my + 6, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        if (relX >= SWITCH_X && relX < SWITCH_X + SWITCH_W && relY >= SWITCH_Y && relY < SWITCH_Y + SWITCH_H)
        {
            // 乐观更新本地开关，再发包让服务端切换
            this.menu.toggleEnabledLocal();
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null)
            {
                conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, 0));
            }
            return true;
        }
        if (relX >= MODE_X && relX < MODE_X + MODE_W && relY >= MODE_Y && relY < MODE_Y + MODE_H)
        {
            // 乐观更新本地模式，再发包让服务端切换
            this.menu.toggleModeLocal();
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null)
            {
                conn.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, 1));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }
}
