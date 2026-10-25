package com.godofthings.wt.client;

import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import appeng.client.gui.me.items.CraftingTermScreen;
import appeng.client.gui.style.ScreenStyle;

import com.godofthings.wt.core.WtMagnetMode;
import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.terminals.WtCraftingMenu;

/**
 * 无线合成终端屏幕（照抄 ae2wtlib 的 {@code WCTScreen}，[本地改动] 图标按钮/设置屏幕/
 * 槽位渲染换本模组实现）。
 */
public class WtCraftingScreen extends CraftingTermScreen<WtCraftingMenu> implements WtUniversalTerminalCapable
{
    private final WtIconButton magnetCardMenuButton;
    private final WtScrollingUpgradesPanel upgradesPanel;

    public WtCraftingScreen(WtCraftingMenu container, Inventory playerInventory, Component title, ScreenStyle style)
    {
        super(container, playerInventory, title, style);
        if (getMenu().isWUT())
            addToLeftToolbar(cycleTerminalButton());

        WtIconButton wirelessTerminalSettingsButton = new WtIconButton(
                btn -> switchToScreen(new WtTerminalSettingsScreen(this)), WtIcon.TERMINAL_SETTINGS_AE2);
        widgets.add("wirelessTerminalSettingsButton", wirelessTerminalSettingsButton);
        wirelessTerminalSettingsButton.setMessage(Component.translatable(
                "gui.godofthings.wireless_terminal_settings_title"));

        magnetCardMenuButton = new WtIconButton(btn -> getMenu().openMagnetMenu(), WtIcon.MAGNET);
        widgets.add("magnetCardMenuButton", magnetCardMenuButton);
        magnetCardMenuButton.setMessage(Component.translatable("gui.godofthings.magnet"));

        WtIconButton trashButton = new WtIconButton(btn -> getMenu().openTrashMenu(), WtIcon.TRASH);
        widgets.add("trashButton", trashButton);
        trashButton.setMessage(Component.translatable("gui.godofthings.trash"));

        widgets.add("player", new WtPlayerEntityWidget(Objects.requireNonNull(Minecraft.getInstance().player)));
        upgradesPanel = addUpgradePanel(widgets, getMenu());
    }

    @Override
    public void init()
    {
        super.init();
        upgradesPanel.setMaxRows(Math.max(2, getVisibleRows()));
    }

    private void setMagnetModeText()
    {
        magnetCardMenuButton.setVisibility(
                switch (getMenu().getMagnetMode())
                {
                    case INVALID, NO_CARD -> false;
                    case OFF, PICKUP_ME, PICKUP_INVENTORY, PICKUP_ME_NO_MAGNET -> true;
                });
    }

    @Override
    protected void updateBeforeRender()
    {
        super.updateBeforeRender();
        setMagnetModeText();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int keyPressed)
    {
        boolean value = super.keyPressed(keyCode, scanCode, keyPressed);
        if (!value)
            return checkForTerminalKeys(keyCode, scanCode);
        return true;
    }

    @Override
    public WtMenuHost getHost()
    {
        return (WtMenuHost) getMenu().getHost();
    }

    @Override
    public boolean isHandlingRightClick()
    {
        return super.isHandlingRightClick();
    }

    @Override
    public void storeState()
    {
        super.storeState();
    }

    /**
     * This overrides the base-class method through some access transformer hackery...
     */
    @Override
    public void renderSlot(GuiGraphics guiGraphics, Slot s)
    {
        if (s instanceof WtArmorSlot armorSlot)
        {
            renderArmorSlot(guiGraphics, armorSlot);
        }
        else
        {
            super.renderSlot(guiGraphics, s);
        }
    }

    private void renderArmorSlot(GuiGraphics guiGraphics, WtArmorSlot s)
    {
        var is = s.getItem();

        if (is.isEmpty() && s.isSlotEnabled())
        {
            s.icon().getBlitter()
                    .dest(s.x, s.y)
                    .opacity(s.getOpacityOfIcon())
                    .blit(guiGraphics);
        }

        super.renderSlot(guiGraphics, s);
    }
}
