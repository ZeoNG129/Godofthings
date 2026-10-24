package com.godofthings.mept.client;

/**
 * 客户端选中物品 / 放置数量悬浮提示（照抄上游 MEPlacementToolMod.ClientForgeEvents 的静态字段部分；
 * 渲染在 ClientModEvents 注册的 GUI 图层里完成）。
 */
public final class MeptClientOverlay
{
    public static String lastSelectedText = null;
    public static long lastSelectedTime = 0L;
    public static String lastCountText = null;
    public static long lastCountTime = 0L;

    private MeptClientOverlay()
    {
    }

    public static void showCountOverlay(String text)
    {
        lastCountText = text;
        lastCountTime = System.currentTimeMillis();
    }

    public static void showSelectedOverlay(String text)
    {
        lastSelectedText = text;
        lastSelectedTime = System.currentTimeMillis();
    }
}
