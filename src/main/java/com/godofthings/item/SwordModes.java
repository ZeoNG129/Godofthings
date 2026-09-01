package com.godofthings.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * 神之剑的功能开关存储。
 * 与 {@link WandModes} 同构：用 DataComponents.CUSTOM_DATA 承载三个独立布尔开关，
 * 斩首 / 捕捉 / 抢劫可单独或同时生效。
 */
public final class SwordModes
{
    /** 斩首：击杀带头颅的生物必掉头颅 */
    public static final String BEHEAD = "SwordBehead";
    /** 捕捉：击杀生物掉落对应刷怪蛋 */
    public static final String CAPTURE = "SwordCapture";
    /** 抢劫：顶级抢夺 255 */
    public static final String LOOTING = "SwordLooting";

    private SwordModes() {}

    public static CompoundTag getData(ItemStack stack)
    {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }

    public static boolean getBoolean(ItemStack stack, String key)
    {
        return getData(stack).getBoolean(key);
    }

    public static void update(ItemStack stack, Consumer<CompoundTag> consumer)
    {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, consumer);
    }

    public static void setBoolean(ItemStack stack, String key, boolean value)
    {
        update(stack, tag -> tag.putBoolean(key, value));
    }

    public static boolean isBeheadEnabled(ItemStack stack)
    {
        return getBoolean(stack, BEHEAD);
    }

    public static void setBeheadEnabled(ItemStack stack, boolean enabled)
    {
        setBoolean(stack, BEHEAD, enabled);
    }

    public static boolean isCaptureEnabled(ItemStack stack)
    {
        return getBoolean(stack, CAPTURE);
    }

    public static void setCaptureEnabled(ItemStack stack, boolean enabled)
    {
        setBoolean(stack, CAPTURE, enabled);
    }

    public static boolean isLootingEnabled(ItemStack stack)
    {
        return getBoolean(stack, LOOTING);
    }

    public static void setLootingEnabled(ItemStack stack, boolean enabled)
    {
        setBoolean(stack, LOOTING, enabled);
    }
}
