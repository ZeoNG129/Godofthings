package com.godofthings.mept;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * AE2LT 过载频率卡自动连接的软依赖桥（照抄上游 AE2LTFrequencyCardHelper）。
 * <p>
 * 与上游不同：上游直接 import AE2LT 的 WirelessLinkRegistry（编译期依赖 AE2LT 源码），
 * 本模组用反射调用，AE2LT 未安装时静默跳过，避免新增编译依赖。
 */
public final class MeptAe2ltHelper
{
    private MeptAe2ltHelper()
    {
    }

    /**
     * 为新放置的 ME 方块/部件排队 AE2LT 自动连接。AE2LT 未装或客户端调用时安全返回。
     */
    public static void queueAutoConnect(Player player, Level level, BlockPos pos, @Nullable Direction side)
    {
        if (level.isClientSide || !MeptModCompat.isAe2ltLoaded())
        {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return;
        }

        try
        {
            queueAutoConnectInternal(serverPlayer, level, pos, side);
        }
        catch (Throwable t)
        {
            // AE2LT 类不可用或 API 不匹配 —— 忽略
        }
    }

    private static void queueAutoConnectInternal(ServerPlayer player, Level level, BlockPos pos,
            @Nullable Direction side) throws Exception
    {
        // [本地改动] 上游直接调用 com.moakiee.ae2lt.grid.wirelesslink.WirelessLinkRegistry.get()
        // .queueAutoConnect(player, level.dimension(), pos, side, 2)；这里按同名反射调用。
        Class<?> registryClass = Class.forName("com.moakiee.ae2lt.grid.wirelesslink.WirelessLinkRegistry");
        var getInstance = registryClass.getMethod("get");
        Object registry = getInstance.invoke(null);
        if (registry == null)
        {
            return;
        }
        var queueMethod = registry.getClass().getMethod("queueAutoConnect",
                ServerPlayer.class, net.minecraft.resources.ResourceKey.class, BlockPos.class,
                Direction.class, int.class);
        queueMethod.invoke(registry, player, level.dimension(), pos, side, 2);
    }
}
