package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.waypoint.Waypoint;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 快捷键（U 键）打开的传送点菜单：无方块依赖，stillValid 恒真，
 * 与神之记录方块界面共用同一套 UI 与网络逻辑。
 */
public class WaypointMenu extends AbstractContainerMenu implements WaypointListMenu
{
    /** 客户端缓存的点位列表 */
    private List<Waypoint> waypoints = new ArrayList<>();

    public WaypointMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv);
    }

    public WaypointMenu(int containerId, Inventory playerInv)
    {
        super(Godofthings.WAYPOINT_MENU.get(), containerId);
    }

    @Override
    public List<Waypoint> getWaypoints()
    {
        return waypoints;
    }

    @Override
    public void setWaypoints(List<Waypoint> list)
    {
        this.waypoints = list;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
