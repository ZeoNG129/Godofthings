package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.waypoint.Waypoint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之记录菜单：无物品槽，仅承载传送点列表。
 * 列表由客户端屏幕打开后经 WaypointMessages.requestList() 主动请求下发，
 * 避免 openScreen 包与列表包到达时序颠倒导致首次不同步。
 */
public class GodRecordMenu extends AbstractContainerMenu
{
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    /** 客户端缓存的点位列表（服务端不依赖此字段） */
    private List<Waypoint> waypoints = new ArrayList<>();

    public GodRecordMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv, extraData.readBlockPos());
    }

    public GodRecordMenu(int containerId, Inventory playerInv, BlockPos pos)
    {
        super(Godofthings.GOD_RECORD_MENU.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);
    }

    public void setWaypoints(List<Waypoint> list)
    {
        this.waypoints = list;
    }

    public List<Waypoint> getWaypoints()
    {
        return waypoints;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, Godofthings.GOD_RECORD.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
