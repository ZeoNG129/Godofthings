package com.godofthings.client.screen;

import com.godofthings.menu.WaypointMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 传送点界面（U 键快捷键打开）：复用 AbstractWaypointScreen 的传送点列表 UI。
 */
public class WaypointScreen extends AbstractWaypointScreen<WaypointMenu>
{
    public WaypointScreen(WaypointMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }
}
