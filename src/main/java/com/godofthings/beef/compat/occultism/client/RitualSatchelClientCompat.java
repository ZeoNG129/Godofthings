package com.godofthings.beef.compat.occultism.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 匠心仪式挎包客户端兼容的本地空壳（[本地改动]）：开发环境无 occultism 依赖，
 * 恒返回 false（客户端不识别五芒星预览，也就不会发送摆放包）。
 */
public final class RitualSatchelClientCompat
{
    private RitualSatchelClientCompat()
    {
    }

    public static boolean trySendPlacement(Level level, Player player, BlockPos pos)
    {
        return false;
    }
}
