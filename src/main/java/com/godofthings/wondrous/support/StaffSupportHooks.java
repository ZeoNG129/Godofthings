package com.godofthings.wondrous.support;

import net.minecraft.world.entity.player.Player;

/**
 * 上游基 mod（useless_mod）挂钩点的本地替身（[本地改动]）。
 * <p>
 * 上游杖移植代码里有三处直接调用基 mod：
 * <ol>
 *   <li>{@code UComponents.BeefTimeAccelerationEnabledComponent} —— G 菜单时间加速总开关组件；
 *       本模组用 {@code StaffComponents.STAFF_TIME_ACCELERATION}（组件名 wondrous_staff_enabled）承载，
 *       见 {@link com.godofthings.wondrous.init.StaffComponents}。</li>
 *   <li>{@code EventHandler.applyBeefToolFlightNow(player)} —— 上游造化杖退出相位模式时
 *       恢复其自管飞行；本模组没有造化杖飞行系统，这里只是按创造/旁观刷新能力包（无副作用）。</li>
 *   <li>{@code BeefTimeAcceleration}（造化杖自带的低配时间加速）—— 本模组没有该类，
 *       不需要「对杖禁用基 mod 加速」的 mixin，删除对应上游 mixin 即可。</li>
 * </ol>
 */
public final class StaffSupportHooks
{
    private StaffSupportHooks()
    {
    }

    /** 上游 EventHandler.applyBeefToolFlightNow 的本地替身：刷新玩家能力包。 */
    public static void applyFlightRefresh(Player player)
    {
        if (player != null && !player.level().isClientSide())
        {
            player.onUpdateAbilities();
        }
    }
}
