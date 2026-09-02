package com.godofthings.client.screen;

import com.godofthings.block.entity.GodTransmitterBlockEntity;
import com.godofthings.menu.GodTransmitterMenu;
import com.godofthings.network.TransmitterMessages;
import com.godofthings.util.NumberText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.function.Predicate;

/**
 * 神之传输配置屏幕：三分区（无线连接 / 玩家充能 / 权限），无玩家物品栏。
 * 速率滑块 5 档 + 手动输入 FE/T + 速率限制/无上限模式 + 跨维度开关 + 绑定设备数/清除绑定。
 */
public class GodTransmitterScreen extends AbstractContainerScreen<GodTransmitterMenu>
{
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

    private static final Predicate<String> DIGITS = s -> s.isEmpty() || s.matches("\\d*");

    private static final int MODE_MACHINE = 0;
    private static final int MODE_PLAYER = 1;

    // 无线连接分区
    private static final int WIRELESS_TITLE_Y = 14;
    private static final int M_RATE_LABEL_Y = 30;
    private static final int M_SLIDER_Y = 44;
    private static final int M_EDIT_Y = 62;
    private static final int M_MODE_Y = 80;
    private static final int M_BOUND_Y = 100;
    private static final int M_CLEAR_Y = 112;

    // 玩家充能分区
    private static final int PLAYER_TITLE_Y = 130;
    private static final int P_TOGGLE_Y = 144;
    private static final int P_RATE_LABEL_Y = 164;
    private static final int P_SLIDER_Y = 178;
    private static final int P_EDIT_Y = 196;
    private static final int P_MODE_Y = 214;

    // 权限分区
    private static final int PERM_TITLE_Y = 232;
    private static final int SLOT_Y = 248;

    private static final int[] SLIDER_X = { 10, 58, 106, 154, 202 };
    private static final int SLIDER_W = 46;
    private static final int SLIDER_H = 14;

    private EditBox machineEdit;
    private EditBox playerEdit;

    public GodTransmitterScreen(GodTransmitterMenu menu, Inventory playerInv, Component title)
    {
        super(menu, playerInv, title);
        this.imageWidth = 256;
        this.imageHeight = 268;
    }

    @Override
    protected void init()
    {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        this.machineEdit = new EditBox(this.font, x + 10, y + M_EDIT_Y, 90, 14, Component.literal(""));
        this.machineEdit.setFilter(DIGITS);
        this.machineEdit.setMaxLength(7);
        this.machineEdit.setValue(String.valueOf(this.menu.getMachineRate()));

        this.playerEdit = new EditBox(this.font, x + 10, y + P_EDIT_Y, 90, 14, Component.literal(""));
        this.playerEdit.setFilter(DIGITS);
        this.playerEdit.setMaxLength(7);
        this.playerEdit.setValue(String.valueOf(this.menu.getPlayerRate()));
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 面板背景
        gui.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E22);

        // ---- 无线连接分区 ----
        gui.fill(x + 4, y + WIRELESS_TITLE_Y - 4, x + this.imageWidth - 4, y + PLAYER_TITLE_Y - 6, 0xFF2A2A30);
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.wireless"), x + 10, y + WIRELESS_TITLE_Y, 0x55FFFF);
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.rate",
                NumberText.format(this.menu.getMachineRate())), x + 10, y + M_RATE_LABEL_Y, 0xFFFFFF);
        drawSlider(gui, x, y + M_SLIDER_Y, this.menu.getMachineRate());
        // 模式按钮（速率限制 / 无上限）
        drawButton(gui, x + 10, y + M_MODE_Y, 118, 16,
                this.menu.isMachineUnlimited()
                        ? Component.translatable("gui.godofthings.transmitter.unlimited")
                        : Component.translatable("gui.godofthings.transmitter.rate_limit"),
                this.menu.isMachineUnlimited());
        // 跨维度按钮
        drawButton(gui, x + 132, y + M_MODE_Y, 114, 16,
                this.menu.isMachineCrossDimension()
                        ? Component.translatable("gui.godofthings.transmitter.cross_on")
                        : Component.translatable("gui.godofthings.transmitter.cross_off"),
                this.menu.isMachineCrossDimension());
        // 绑定设备数
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.bound_count",
                this.menu.getBoundCount()), x + 10, y + M_BOUND_Y, 0xCCCCCC);
        // 清除绑定按钮
        drawButton(gui, x + 150, y + M_CLEAR_Y, 96, 16,
                Component.translatable("gui.godofthings.transmitter.clear_bindings"), false);

        // ---- 玩家充能分区 ----
        gui.fill(x + 4, y + PLAYER_TITLE_Y - 4, x + this.imageWidth - 4, y + PERM_TITLE_Y - 6, 0xFF2A2A30);
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.player_charge"), x + 10, y + PLAYER_TITLE_Y, 0xFFFF55);
        drawButton(gui, x + 10, y + P_TOGGLE_Y, 110, 16,
                this.menu.isPlayerEnabled()
                        ? Component.translatable("gui.godofthings.transmitter.player_on")
                        : Component.translatable("gui.godofthings.transmitter.player_off"),
                this.menu.isPlayerEnabled());
        drawButton(gui, x + 124, y + P_TOGGLE_Y, 122, 16,
                this.menu.isPlayerCrossDimension()
                        ? Component.translatable("gui.godofthings.transmitter.player_cross_on")
                        : Component.translatable("gui.godofthings.transmitter.player_cross_off"),
                this.menu.isPlayerCrossDimension());
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.rate",
                NumberText.format(this.menu.getPlayerRate())), x + 10, y + P_RATE_LABEL_Y, 0xFFFFFF);
        drawSlider(gui, x, y + P_SLIDER_Y, this.menu.getPlayerRate());
        drawButton(gui, x + 10, y + P_MODE_Y, 118, 16,
                this.menu.isPlayerUnlimited()
                        ? Component.translatable("gui.godofthings.transmitter.unlimited")
                        : Component.translatable("gui.godofthings.transmitter.rate_limit"),
                this.menu.isPlayerUnlimited());

        // ---- 权限分区 ----
        gui.fill(x + 4, y + PERM_TITLE_Y - 4, x + this.imageWidth - 4, y + this.imageHeight - 4, 0xFF2A2A30);
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.permission"), x + 10, y + PERM_TITLE_Y, 0xCCCCCC);
        gui.blitSprite(SLOT_SPRITE, x + 10, y + SLOT_Y, 18, 18);
        gui.blitSprite(SLOT_SPRITE, x + 40, y + SLOT_Y, 18, 18);
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.binder"), x + 32, y + SLOT_Y + 5, 0xCCCCCC);
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.accel"), x + 62, y + SLOT_Y + 5, 0xCCCCCC);
    }

    /** 画 5 档速率滑块按钮（当前档高亮）。 */
    private void drawSlider(GuiGraphics gui, int panelX, int panelY, int currentRate)
    {
        for (int i = 0; i < GodTransmitterBlockEntity.RATE_PRESETS.length; i++)
        {
            int preset = GodTransmitterBlockEntity.RATE_PRESETS[i];
            boolean active = currentRate == preset;
            drawButton(gui, panelX + SLIDER_X[i], panelY, SLIDER_W, SLIDER_H,
                    Component.literal(NumberText.format(preset)), active);
        }
    }

    private void drawButton(GuiGraphics gui, int bx, int by, int w, int h, Component label, boolean active)
    {
        gui.fill(bx, by, bx + w, by + h, 0xFF16181D);
        gui.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, active ? 0xFF57B757 : 0xFF3A4048);
        gui.drawString(this.font, label, bx + (w - this.font.width(label)) / 2, by + 4, 0xFFFFFF);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick)
    {
        super.render(gui, mouseX, mouseY, partialTick);
        this.machineEdit.render(gui, mouseX, mouseY, partialTick);
        this.playerEdit.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
        // 加速槽 hover 提示
        if (isHovering(40, SLOT_Y, 18, 18, mouseX, mouseY)
                && (this.hoveredSlot == null || !this.hoveredSlot.hasItem()))
        {
            gui.renderTooltip(this.font, Component.translatable("tooltip.godofthings.accelerator"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        // 机器滑块
        for (int i = 0; i < GodTransmitterBlockEntity.RATE_PRESETS.length; i++)
        {
            if (inRect(relX, relY, SLIDER_X[i], M_SLIDER_Y, SLIDER_W, SLIDER_H))
            {
                int rate = GodTransmitterBlockEntity.RATE_PRESETS[i];
                this.menu.setMachineRateLocal(rate);
                this.machineEdit.setValue(String.valueOf(rate));
                TransmitterMessages.sendRate(rate, MODE_MACHINE);
                return true;
            }
        }
        // 玩家滑块
        for (int i = 0; i < GodTransmitterBlockEntity.RATE_PRESETS.length; i++)
        {
            if (inRect(relX, relY, SLIDER_X[i], P_SLIDER_Y, SLIDER_W, SLIDER_H))
            {
                int rate = GodTransmitterBlockEntity.RATE_PRESETS[i];
                this.menu.setPlayerRateLocal(rate);
                this.playerEdit.setValue(String.valueOf(rate));
                TransmitterMessages.sendRate(rate, MODE_PLAYER);
                return true;
            }
        }
        // 机器模式按钮
        if (inRect(relX, relY, 10, M_MODE_Y, 118, 16))
        {
            this.menu.toggleMachineUnlimitedLocal();
            sendButton(0);
            return true;
        }
        // 机器跨维度按钮
        if (inRect(relX, relY, 132, M_MODE_Y, 114, 16))
        {
            this.menu.toggleMachineCrossDimensionLocal();
            sendButton(1);
            return true;
        }
        // 清除绑定按钮
        if (inRect(relX, relY, 150, M_CLEAR_Y, 96, 16))
        {
            sendButton(2);
            return true;
        }
        // 玩家开关
        if (inRect(relX, relY, 10, P_TOGGLE_Y, 110, 16))
        {
            this.menu.togglePlayerEnabledLocal();
            sendButton(3);
            return true;
        }
        // 玩家跨维度
        if (inRect(relX, relY, 124, P_TOGGLE_Y, 122, 16))
        {
            this.menu.togglePlayerCrossDimensionLocal();
            sendButton(4);
            return true;
        }
        // 玩家模式按钮
        if (inRect(relX, relY, 10, P_MODE_Y, 118, 16))
        {
            this.menu.togglePlayerUnlimitedLocal();
            sendButton(5);
            return true;
        }

        // EditBox 点击
        if (this.machineEdit.mouseClicked(mouseX, mouseY, button))
        {
            return true;
        }
        if (this.playerEdit.mouseClicked(mouseX, mouseY, button))
        {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (this.machineEdit.isFocused() && (keyCode == 257 || keyCode == 335))
        {
            commitRate(this.machineEdit, MODE_MACHINE);
            return true;
        }
        if (this.playerEdit.isFocused() && (keyCode == 257 || keyCode == 335))
        {
            commitRate(this.playerEdit, MODE_PLAYER);
            return true;
        }
        if (this.machineEdit.keyPressed(keyCode, scanCode, modifiers))
        {
            return true;
        }
        if (this.playerEdit.keyPressed(keyCode, scanCode, modifiers))
        {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers)
    {
        if (this.machineEdit.isFocused() && this.machineEdit.charTyped(codePoint, modifiers))
        {
            return true;
        }
        if (this.playerEdit.isFocused() && this.playerEdit.charTyped(codePoint, modifiers))
        {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void commitRate(EditBox box, int mode)
    {
        int rate;
        try
        {
            rate = Integer.parseInt(box.getValue().trim());
        }
        catch (NumberFormatException e)
        {
            return;
        }
        rate = Math.max(GodTransmitterBlockEntity.MIN_RATE,
                Math.min(GodTransmitterBlockEntity.MAX_RATE, rate));
        if (mode == MODE_MACHINE)
        {
            this.menu.setMachineRateLocal(rate);
        }
        else
        {
            this.menu.setPlayerRateLocal(rate);
        }
        box.setValue(String.valueOf(rate));
        TransmitterMessages.sendRate(rate, mode);
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
