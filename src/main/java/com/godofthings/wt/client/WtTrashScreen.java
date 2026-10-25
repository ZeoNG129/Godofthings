package com.godofthings.wt.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;

import com.godofthings.wt.terminals.WtTrashMenu;

/**
 * 垃圾桶屏幕（照抄 ae2wtlib 的 {@code TrashScreen}）。
 */
public class WtTrashScreen extends AEBaseScreen<WtTrashMenu>
{
    public WtTrashScreen(WtTrashMenu menu, Inventory playerInventory, Component title, ScreenStyle style)
    {
        super(menu, playerInventory, title, style);
        appeng.client.gui.implementations.AESubScreen.addBackButton(menu, "back", widgets);
    }

    // Added to remove the VerticalButtonBar for this Screen
    @Override
    protected boolean shouldAddToolbar()
    {
        return false;
    }
}
