package com.godofthings.wt.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.ScreenStyle;

import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.terminals.WtEncodingMenu;

/**
 * 无线样板终端屏幕（照抄 ae2wtlib 的 {@code WETScreen}）。
 */
public class WtEncodingScreen extends PatternEncodingTermScreen<WtEncodingMenu>
        implements WtUniversalTerminalCapable
{
    private final WtScrollingUpgradesPanel upgradesPanel;

    public WtEncodingScreen(WtEncodingMenu container, Inventory playerInventory, Component title, ScreenStyle style)
    {
        super(container, playerInventory, title, style);
        if (getMenu().isWUT())
            addToLeftToolbar(cycleTerminalButton());
        upgradesPanel = addUpgradePanel(widgets, getMenu());
    }

    @Override
    public void init()
    {
        super.init();
        upgradesPanel.setMaxRows(Math.max(2, getVisibleRows()));
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
}
