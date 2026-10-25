package com.godofthings.wt.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import appeng.menu.locator.ItemMenuHostLocator;

import com.godofthings.wt.terminals.WtAccessMenu;
import com.godofthings.wt.terminals.WtEncodingMenu;

/**
 * 无线样板终端物品（照抄 ae2wtlib 的 {@code ItemWET}）。
 */
public class WtEncodingItem extends WtTerminalItem
{
    @Override
    public MenuType<?> getMenuType(ItemMenuHostLocator locator, Player player)
    {
        return WtEncodingMenu.TYPE;
    }
}
