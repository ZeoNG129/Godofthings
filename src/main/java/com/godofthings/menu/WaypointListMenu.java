package com.godofthings.menu;

import com.godofthings.waypoint.Waypoint;

import java.util.List;

/**
 * 传送点列表菜单的公共契约：提供点位的读写，供屏幕与网络层复用。
 * 实现者：GodRecordMenu（方块打开）、WaypointMenu（快捷键打开）。
 */
public interface WaypointListMenu
{
    List<Waypoint> getWaypoints();

    void setWaypoints(List<Waypoint> list);
}
