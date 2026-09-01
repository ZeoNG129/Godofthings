package com.godofthings.client.screen;

import com.godofthings.menu.GodRecordMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 神之记录界面（方块打开）：复用 AbstractWaypointScreen 的传送点列表 UI。
 */
public class GodRecordScreen extends AbstractWaypointScreen<GodRecordMenu>
{
    public GodRecordScreen(GodRecordMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
    }
}
