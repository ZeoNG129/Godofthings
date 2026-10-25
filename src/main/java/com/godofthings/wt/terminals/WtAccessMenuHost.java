package com.godofthings.wt.terminals;

import java.util.function.BiConsumer;

import appeng.api.networking.IGridNode;
import appeng.api.storage.IPatternAccessTermMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;

import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtTerminalItem;
import net.minecraft.world.entity.player.Player;

/**
 * 无线样板管理终端宿主（照抄 ae2wtlib 的 {@code WATMenuHost}）。
 */
public class WtAccessMenuHost extends WtMenuHost implements IPatternAccessTermMenuHost
{
    public WtAccessMenuHost(WtTerminalItem item, Player player, ItemMenuHostLocator locator,
            BiConsumer<Player, ISubMenu> returnToMainMenu)
    {
        super(item, player, locator, returnToMainMenu);
    }

    @Override
    public IGridNode getGridNode()
    {
        return getActionableNode();
    }
}
