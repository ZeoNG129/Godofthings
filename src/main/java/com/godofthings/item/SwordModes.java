package com.godofthings.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * 神之剑的功能开关存储。
 * 与 {@link WandModes} 同构：用 DataComponents.CUSTOM_DATA 承载独立布尔开关，
 * 斩首 / 捕捉 / 抢劫 / 吸星 / 吸魂可单独或同时生效。
 */
public final class SwordModes
{
    /** 斩首：击杀带头颅的生物必掉头颅 */
    public static final String BEHEAD = "SwordBehead";
    /** 捕捉：击杀生物掉落对应刷怪蛋 */
    public static final String CAPTURE = "SwordCapture";
    /** 抢劫：顶级抢夺 255 */
    public static final String LOOTING = "SwordLooting";
    /** 吸星：手持神之剑吸收附近掉落物与经验 */
    public static final String STAR_ABSORB = "SwordStarAbsorb";
    /** 吸魂：手持神之剑把附近生物吸到玩家面前 */
    public static final String SOUL_ABSORB = "SwordSoulAbsorb";
    /** 吸星/吸魂的生效半径（格），可调 3~300。 */
    public static final String STAR_RANGE = "SwordStarRange";
    public static final String SOUL_RANGE = "SwordSoulRange";
    public static final int MIN_RANGE = 3;
    public static final int MAX_RANGE = 300;
    public static final int DEFAULT_RANGE = 16;

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

    public static int getInt(ItemStack stack, String key)
    {
        return getData(stack).getInt(key);
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

    public static boolean isStarAbsorbEnabled(ItemStack stack)
    {
        return getBoolean(stack, STAR_ABSORB);
    }

    public static void setStarAbsorbEnabled(ItemStack stack, boolean enabled)
    {
        setBoolean(stack, STAR_ABSORB, enabled);
    }

    public static boolean isSoulAbsorbEnabled(ItemStack stack)
    {
        return getBoolean(stack, SOUL_ABSORB);
    }

    public static void setSoulAbsorbEnabled(ItemStack stack, boolean enabled)
    {
        setBoolean(stack, SOUL_ABSORB, enabled);
    }

    public static int getStarRange(ItemStack stack)
    {
        int r = getInt(stack, STAR_RANGE);
        return r < MIN_RANGE ? DEFAULT_RANGE : r;
    }

    public static void setStarRange(ItemStack stack, int value)
    {
        update(stack, tag -> tag.putInt(STAR_RANGE, Math.max(MIN_RANGE, Math.min(MAX_RANGE, value))));
    }

    public static int getSoulRange(ItemStack stack)
    {
        int r = getInt(stack, SOUL_RANGE);
        return r < MIN_RANGE ? DEFAULT_RANGE : r;
    }

    public static void setSoulRange(ItemStack stack, int value)
    {
        update(stack, tag -> tag.putInt(SOUL_RANGE, Math.max(MIN_RANGE, Math.min(MAX_RANGE, value))));
    }
}
