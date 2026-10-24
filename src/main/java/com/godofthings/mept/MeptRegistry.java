package com.godofthings.mept;

/**
 * ME 放置工具的接线入口（上游挂在 MEPlacementToolMod 实例上的撤销历史，
 * 本地按项目惯例放静态单例；物品注册见 {@link MeptItems}）。
 */
public final class MeptRegistry
{
    private static MeptUndoHistory undoHistory;

    private MeptRegistry()
    {
    }

    public static synchronized MeptUndoHistory undoHistory()
    {
        if (undoHistory == null)
        {
            undoHistory = new MeptUndoHistory();
        }
        return undoHistory;
    }
}
