package com.godofthings.wt.core;

import java.util.HashMap;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.menu.locator.ItemMenuHostLocator;

import com.godofthings.wt.core.WtComponents;
import com.godofthings.wt.core.WtMagnetHost;
import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtTerminalDefinition;
import com.godofthings.wt.WtRegistration;

/**
 * 合成终端定位器（照抄 ae2wtlib 的 {@code CraftingTerminalHandler}，[本地改动] 不用
 * 数据附件，改用玩家 WeakHashMap 缓存——效果一致且免去附件注册）。
 */
public class WtCraftingTerminalHandler
{
    private static final java.util.WeakHashMap<Player, WtCraftingTerminalHandler> HANDLERS = new java.util.WeakHashMap<>();

    public static WtCraftingTerminalHandler get(Player player)
    {
        return HANDLERS.computeIfAbsent(player, p -> new WtCraftingTerminalHandler(p));
    }

    public static void invalidate(Player player)
    {
        HANDLERS.remove(player);
    }

    private final Player player;
    private WtMenuHost menuHost;
    private ItemMenuHostLocator locator;
    private WtMagnetHost magnetHost;
    private HashMap<net.minecraft.world.item.Item, Long> restockAbleItems = new HashMap<>();
    private boolean restockEnabled = false;

    private WtCraftingTerminalHandler(Player player)
    {
        this.player = player;
    }

    private void invalidateCache()
    {
        menuHost = null;
        locator = null;
        magnetHost = null;
        restockAbleItems.clear();
        restockEnabled = false;
    }

    public ItemStack getCraftingTerminal()
    {
        getLocator();
        if (locator != null)
        {
            return locator.locateItem(player);
        }
        return ItemStack.EMPTY;
    }

    private WtMenuHost getMenuHost()
    {
        if (menuHost != null)
        {
            if (!menuHost.isValid())
            {
                invalidateCache();
                return getMenuHost();
            }
            return menuHost;
        }

        getLocator();

        if (locator == null)
        {
            menuHost = null;
        }
        else
        {
            menuHost = locator.locate(player, WtMenuHost.class);
        }

        if (menuHost == null)
        {
            invalidateCache();
        }

        return menuHost;
    }

    public ItemMenuHostLocator getLocator()
    {
        if (locator != null && WtTerminalDefinition
                .hasTerminal(locator.locateItem(player), WtRegistration.CRAFTING()))
        {
            return locator;
        }
        boolean locatorWasNotNull = locator != null;
        locator = WtTerminalDefinition.findTerminal(player, WtRegistration.CRAFTING());

        if (locator == null)
        {
            invalidateCache();
            if (locatorWasNotNull)
            {
                return getLocator();
            }
        }
        return locator;
    }

    public IGrid getTargetGrid()
    {
        if (getMenuHost() == null)
        {
            return null;
        }
        final IGridNode n = getMenuHost().getActionableNode();

        if (n == null)
        {
            return null;
        }
        return n.getGrid();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean inRange()
    {
        if (getMenuHost() == null)
        {
            return false;
        }

        getMenuHost().updateConnectedAccessPoint();
        getMenuHost().updateLinkStatus();
        return getMenuHost().getLinkStatus().connected();
    }

    public WtMagnetHost getMagnetHost()
    {
        if (magnetHost == null)
        {
            if (getLocator() == null)
            {
                return magnetHost = null;
            }
            magnetHost = new WtMagnetHost(this);
        }
        return magnetHost;
    }

    public long getAccessibleAmount(ItemStack stack)
    {
        return stack.getCount()
                + (restockAbleItems.get(stack.getItem()) == null ? 0 : restockAbleItems.get(stack.getItem()));
    }

    public boolean isRestockAble(ItemStack stack)
    {
        return restockAbleItems.containsKey(stack.getItem());
    }

    public void setRestockAbleItems(HashMap<net.minecraft.world.item.Item, Long> items)
    {
        restockAbleItems = items;
    }

    public boolean isRestockEnabled()
    {
        return restockEnabled;
    }

    public void checkTerminal()
    {
        restockEnabled = getCraftingTerminal().getOrDefault(WtComponents.RESTOCK, false);
    }

    public Player player()
    {
        return player;
    }
}
