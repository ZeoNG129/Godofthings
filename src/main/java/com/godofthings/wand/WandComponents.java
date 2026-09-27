package com.godofthings.wand;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 垫片：替代上游 {@code UComponents.BeefTimeAccelerationEnabledComponent}。
 * <p>照抄的 {@code WondrousStaffAcceleration} 只在这一处用到上游组件，用一个读 CUSTOM_DATA
 * 的等价实现顶替，从而<b>不必引入上游 mod</b>。
 */
public final class WandComponents
{
    private static final String TAG = "godofthings:wand_ta_enabled";

    private WandComponents() {}

    /** 写入时间加速开关（照抄的配置界面用） */
    public static void setTimeAccelerationEnabled(ItemStack stack, boolean enabled)
    {
        if (stack == null || stack.isEmpty())
        {
            return;
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(TAG, enabled);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** 时间加速开关（默认开） */
    public static boolean timeAccelerationEnabled(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            return false;
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !tag.contains(TAG) || tag.getBoolean(TAG);
    }
}