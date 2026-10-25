package com.godofthings.beef.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 龙之研究（Draconic Evolution）混沌水晶兼容的本地空壳（[本地改动]）。
 * 开发环境无 draconicevolution 编译依赖，恒返回 false（不拦截普通破坏流程）。
 */
public final class DraconicEvolutionCompat
{
    private DraconicEvolutionCompat()
    {
    }

    public static boolean isChaosCrystal(BlockState state)
    {
        return false;
    }

    public static boolean handleChaosCrystalBreak(ServerLevel level, BlockPos pos,
                                                  BlockState state, Player player)
    {
        return false;
    }
}
