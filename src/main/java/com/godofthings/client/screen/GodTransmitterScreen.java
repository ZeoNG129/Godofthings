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
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.function.Predicate;

/**
 * 神之传输配置屏幕：四标签页（无线连接 / 玩家充能 / 权限 / 已绑定机器），顶部按钮切换。
 */
public class GodTransmitterScreen extends AbstractContainerScreen<GodTransmitterMenu>
{
    private static final Predicate<String> DIGITS = s -> s.isEmpty() || s.matches("\\d*");

    private static final int MODE_MACHINE = 0;
    private static final int MODE_PLAYER = 1;

    // 顶部标签页按钮
    private static final int[] TAB_X = { 8, 70, 132, 194 };
    private static final int TAB_W = 58;
    private static final int TAB_H = 16;
    private static final int TAB_Y = 8;

    // 滑块
    private static final int[] SLIDER_X = { 10, 58, 106, 154, 202 };
    private static final int SLIDER_W = 46;
    private static final int SLIDER_H = 14;

    private EditBox machineEdit;
    private EditBox playerEdit;

    private int permScroll = 0;
    private int boundScroll = 0;

    public GodTransmitterScreen(GodTransmitterMenu menu, Inventory playerInv, Component title)
    {
        super(menu, playerInv, title);
        this.imageWidth = 256;
        this.imageHeight = 200;
    }

    @Override
    protected void init()
    {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        this.machineEdit = new EditBox(this.font, x + 10, y + 66, 90, 14, Component.literal(""));
        this.machineEdit.setFilter(DIGITS);
        this.machineEdit.setMaxLength(7);
        this.machineEdit.setValue(String.valueOf(this.menu.getMachineRate()));

        this.playerEdit = new EditBox(this.font, x + 10, y + 94, 90, 14, Component.literal(""));
        this.playerEdit.setFilter(DIGITS);
        this.playerEdit.setMaxLength(7);
        this.playerEdit.setValue(String.valueOf(this.menu.getPlayerRate()));
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY)
    {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        gui.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E22);

        // 顶部标签页按钮
        String[] tabKeys = {
                "gui.godofthings.transmitter.tab_wireless",
                "gui.godofthings.transmitter.tab_player",
                "gui.godofthings.transmitter.tab_permission",
                "gui.godofthings.transmitter.tab_bound"
        };
        for (int i = 0; i < 4; i++)
        {
            boolean active = this.menu.getCurrentTab() == i;
            drawButton(gui, x + TAB_X[i], y + TAB_Y, TAB_W, TAB_H,
                    Component.translatable(tabKeys[i]), active);
        }

        int tab = this.menu.getCurrentTab();
        if (tab == GodTransmitterMenu.TAB_WIRELESS)
        {
            renderWireless(gui, x, y);
        }
        else if (tab == GodTransmitterMenu.TAB_PLAYER)
        {
            renderPlayer(gui, x, y);
        }
        else if (tab == GodTransmitterMenu.TAB_PERMISSION)
        {
            renderPermission(gui, x, y);
        }
        else
        {
            renderBound(gui, x, y);
        }
    }

    private void renderWireless(GuiGraphics gui, int x, int y)
    {
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.rate",
                NumberText.format(this.menu.getMachineRate())), x + 10, y + 34, 0xFFFFFF);
        drawSlider(gui, x, y + 48, this.menu.getMachineRate());
        drawButton(gui, x + 10, y + 84, 118, 16,
                this.menu.isMachineUnlimited()
                        ? Component.translatable("gui.godofthings.transmitter.unlimited")
                        : Component.translatable("gui.godofthings.transmitter.rate_limit"),
                this.menu.isMachineUnlimited());
        drawButton(gui, x + 132, y + 84, 114, 16,
                this.menu.isMachineCrossDimension()
                        ? Component.translatable("gui.godofthings.transmitter.cross_on")
                        : Component.translatable("gui.godofthings.transmitter.cross_off"),
                this.menu.isMachineCrossDimension());
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.bound_count",
                this.menu.getBoundCount()), x + 10, y + 106, 0xCCCCCC);
        drawButton(gui, x + 150, y + 120, 96, 16,
                Component.translatable("gui.godofthings.transmitter.clear_bindings"), false);
    }

    private void renderPlayer(GuiGraphics gui, int x, int y)
    {
        drawButton(gui, x + 10, y + 40, 110, 16,
                this.menu.isPlayerEnabled()
                        ? Component.translatable("gui.godofthings.transmitter.player_on")
                        : Component.translatable("gui.godofthings.transmitter.player_off"),
                this.menu.isPlayerEnabled());
        drawButton(gui, x + 124, y + 40, 122, 16,
                this.menu.isPlayerCrossDimension()
                        ? Component.translatable("gui.godofthings.transmitter.player_cross_on")
                        : Component.translatable("gui.godofthings.transmitter.player_cross_off"),
                this.menu.isPlayerCrossDimension());
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.rate",
                NumberText.format(this.menu.getPlayerRate())), x + 10, y + 62, 0xFFFFFF);
        drawSlider(gui, x, y + 76, this.menu.getPlayerRate());
        drawButton(gui, x + 10, y + 112, 118, 16,
                this.menu.isPlayerUnlimited()
                        ? Component.translatable("gui.godofthings.transmitter.unlimited")
                        : Component.translatable("gui.godofthings.transmitter.rate_limit"),
                this.menu.isPlayerUnlimited());
    }

    private void renderPermission(GuiGraphics gui, int x, int y)
    {
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.online_players"),
                x + 10, y + 32, 0xFFFF55);
        List<String> online = this.menu.getOnline();
        List<String> bound = this.menu.getBoundPlayers();
        int listTop = y + 46;
        int maxRows = (this.imageHeight - 50) / 14;
        for (int i = 0; i < online.size(); i++)
        {
            int row = i - permScroll;
            if (row < 0 || row >= maxRows)
            {
                continue;
            }
            int ry = listTop + row * 14;
            String name = online.get(i);
            boolean isBound = bound.contains(name);
            gui.drawString(this.font, Component.literal(name), x + 10, ry, isBound ? 0x55FF55 : 0xCCCCCC);
            drawButton(gui, x + 150, ry - 1, 96, 12,
                    isBound ? Component.translatable("gui.godofthings.transmitter.unbind")
                            : Component.translatable("gui.godofthings.transmitter.bind"),
                    isBound);
        }
    }

    private void renderBound(GuiGraphics gui, int x, int y)
    {
        gui.drawString(this.font, Component.translatable("gui.godofthings.transmitter.bound_machines"),
                x + 10, y + 32, 0x55FFFF);
        List<String> machines = this.menu.getBoundMachines();
        int listTop = y + 46;
        int maxRows = (this.imageHeight - 50) / 12;
        for (int i = 0; i < machines.size(); i++)
        {
            int row = i - boundScroll;
            if (row < 0 || row >= maxRows)
            {
                continue;
            }
            int ry = listTop + row * 12;
            gui.drawString(this.font, Component.literal(machines.get(i)), x + 10, ry, 0xCCCCCC);
        }
    }

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
        int tab = this.menu.getCurrentTab();
        if (tab == GodTransmitterMenu.TAB_WIRELESS)
        {
            this.machineEdit.render(gui, mouseX, mouseY, partialTick);
        }
        else if (tab == GodTransmitterMenu.TAB_PLAYER)
        {
            this.playerEdit.render(gui, mouseX, mouseY, partialTick);
        }
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        int tab = this.menu.getCurrentTab();

        // 顶部标签页
        for (int i = 0; i < 4; i++)
        {
            if (inRect(relX, relY, TAB_X[i], TAB_Y, TAB_W, TAB_H))
            {
                this.menu.setCurrentTab(i);
                return true;
            }
        }

        if (tab == GodTransmitterMenu.TAB_WIRELESS)
        {
            for (int i = 0; i < GodTransmitterBlockEntity.RATE_PRESETS.length; i++)
            {
                if (inRect(relX, relY, SLIDER_X[i], 48, SLIDER_W, SLIDER_H))
                {
                    int rate = GodTransmitterBlockEntity.RATE_PRESETS[i];
                    this.menu.setMachineRateLocal(rate);
                    this.machineEdit.setValue(String.valueOf(rate));
                    TransmitterMessages.sendRate(rate, MODE_MACHINE);
                    return true;
                }
            }
            if (inRect(relX, relY, 10, 84, 118, 16))
            {
                this.menu.toggleMachineUnlimitedLocal();
                sendButton(0);
                return true;
            }
            if (inRect(relX, relY, 132, 84, 114, 16))
            {
                this.menu.toggleMachineCrossDimensionLocal();
                sendButton(1);
                return true;
            }
            if (inRect(relX, relY, 150, 120, 96, 16))
            {
                sendButton(2);
                return true;
            }
        }
        else if (tab == GodTransmitterMenu.TAB_PLAYER)
        {
            for (int i = 0; i < GodTransmitterBlockEntity.RATE_PRESETS.length; i++)
            {
                if (inRect(relX, relY, SLIDER_X[i], 76, SLIDER_W, SLIDER_H))
                {
                    int rate = GodTransmitterBlockEntity.RATE_PRESETS[i];
                    this.menu.setPlayerRateLocal(rate);
                    this.playerEdit.setValue(String.valueOf(rate));
                    TransmitterMessages.sendRate(rate, MODE_PLAYER);
                    return true;
                }
            }
            if (inRect(relX, relY, 10, 40, 110, 16))
            {
                this.menu.togglePlayerEnabledLocal();
                sendButton(3);
                return true;
            }
            if (inRect(relX, relY, 124, 40, 122, 16))
            {
                this.menu.togglePlayerCrossDimensionLocal();
                sendButton(4);
                return true;
            }
            if (inRect(relX, relY, 10, 112, 118, 16))
            {
                this.menu.togglePlayerUnlimitedLocal();
                sendButton(5);
                return true;
            }
        }
        else if (tab == GodTransmitterMenu.TAB_PERMISSION)
        {
            List<String> online = this.menu.getOnline();
            List<String> bound = this.menu.getBoundPlayers();
            int listTop = 46;
            int maxRows = (this.imageHeight - 50) / 14;
            for (int i = 0; i < online.size(); i++)
            {
                int row = i - permScroll;
                if (row < 0 || row >= maxRows)
                {
                    continue;
                }
                int ry = listTop + row * 14;
                if (inRect(relX, relY, 150, ry - 1, 96, 12))
                {
                    String name = online.get(i);
                    boolean isBound = bound.contains(name);
                    TransmitterMessages.sendBindPlayer(name, !isBound);
                    return true;
                }
            }
        }

        // EditBox 点击
        if (tab == GodTransmitterMenu.TAB_WIRELESS && this.machineEdit.mouseClicked(mouseX, mouseY, button))
        {
            return true;
        }
        if (tab == GodTransmitterMenu.TAB_PLAYER && this.playerEdit.mouseClicked(mouseX, mouseY, button))
        {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        int tab = this.menu.getCurrentTab();
        if (tab == GodTransmitterMenu.TAB_PERMISSION)
        {
            this.permScroll = clampScroll(this.permScroll - (int) scrollY,
                    Math.max(0, this.menu.getOnline().size() - (this.imageHeight - 50) / 14));
            return true;
        }
        if (tab == GodTransmitterMenu.TAB_BOUND)
        {
            this.boundScroll = clampScroll(this.boundScroll - (int) scrollY,
                    Math.max(0, this.menu.getBoundMachines().size() - (this.imageHeight - 50) / 12));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static int clampScroll(int value, int max)
    {
        return Math.max(0, Math.min(max, value));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        int tab = this.menu.getCurrentTab();
        if (tab == GodTransmitterMenu.TAB_WIRELESS && this.machineEdit.isFocused()
                && (keyCode == 257 || keyCode == 335))
        {
            commitRate(this.machineEdit, MODE_MACHINE);
            return true;
        }
        if (tab == GodTransmitterMenu.TAB_PLAYER && this.playerEdit.isFocused()
                && (keyCode == 257 || keyCode == 335))
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
