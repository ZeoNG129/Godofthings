package com.godofthings.wondrous.content.travel;

import com.godofthings.wondrous.content.entity.TimeStopManager;
import com.mojang.logging.LogUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 独立传送：服务端权威入口，不要求手持任何物品（照抄上游 StandaloneTeleport）。
 * 普通按下 = 短距闪现；潜行 + 装了 Ender IO = 旅行锚传送。
 * [本地改动] 上游先反射调用基 mod BeefTeleportHandler 的原生 helper，取不到才回落到
 * 自带 StandaloneBlink；本模组没有「基 mod」一说了，直接用 StandaloneBlink。
 */
public final class StandaloneTeleport
{
    private static final Map<ServerPlayer, Integer> NEXT_ATTEMPT = new WeakHashMap<>();
    private static boolean reportedFailure;

    private StandaloneTeleport()
    {
    }

    public static void activate(ServerPlayer player)
    {
        if (!player.isAlive() || player.isRemoved() || player.isSpectator() || player.isPassenger()
                || TimeStopManager.isPaused(player.serverLevel())
                        && !TimeStopManager.mayPlayerTickDuringPause(player)) return;
        int now = player.getServer().getTickCount();
        if (now < NEXT_ATTEMPT.getOrDefault(player, Integer.MIN_VALUE)) return;
        // 与基类工具一致的五 tick 冷却；失败请求同样限速。
        NEXT_ATTEMPT.put(player, now + 5);
        boolean enderIo = ModList.get().isLoaded("enderio");
        try
        {
            InteractionResult result = StandaloneBlink.tryShortTeleport(player.level(), player);
            if (result == InteractionResult.PASS) player.displayClientMessage(Component.translatable(
                    player.isShiftKeyDown() && !enderIo
                            ? "msg.godofthings.teleport.needs_anchor_mod" : "msg.godofthings.teleport.no_target"), true);
        }
        catch (RuntimeException | LinkageError failure)
        {
            // 原生动作结果不确定时不重试：可能已经传送。
            if (!reportedFailure)
            {
                reportedFailure = true;
                LogUtils.getLogger().warn("Standalone travel could not complete", failure);
            }
            player.displayClientMessage(Component.translatable("msg.godofthings.teleport.unavailable"), true);
        }
    }
}
