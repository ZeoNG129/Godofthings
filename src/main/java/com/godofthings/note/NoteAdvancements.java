package com.godofthings.note;

import com.godofthings.Godofthings;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 神之便签的三个「行为成就」授予点。
 *
 * <p>这三条成就的 JSON 用的是 {@code minecraft:impossible} 触发器 —— 它永远不会自己触发，
 * 只能由代码 {@code award(..., "main")} 给，所以判定逻辑全在这里：</p>
 * <ul>
 *   <li>{@link #OPEN} —— 第一次打开记事本（服务端真正把界面开给玩家的那一刻）</li>
 *   <li>{@link #HUD} —— 第一次把悬浮窗打开（由便签状态从「关」变「开」判定）</li>
 *   <li>{@link #AUTO} —— 第一次开启自动更名</li>
 * </ul>
 * 成就缺失 / 数据包被改坏时静默跳过，绝不影响功能本身。
 */
public final class NoteAdvancements
{
    public static final String OPEN = "note_open";
    public static final String HUD = "note_hud";
    public static final String AUTO = "note_auto";

    private NoteAdvancements() {}

    /** 给玩家授一条本模组的成就（criterion 固定叫 main）；失败静默 */
    public static void award(ServerPlayer player, String path)
    {
        try
        {
            AdvancementHolder holder = player.server.getAdvancements()
                    .get(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, path));
            if (holder != null)
            {
                player.getAdvancements().award(holder, "main");
            }
        }
        catch (Throwable ignored)
        {
            // 成就只是锦上添花，任何异常都不该影响便签本身
        }
    }
}
