package com.godofthings.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;

/**
 * 造化垂青之杖的模式标记存储。
 * 1.21.1 无物品级 NBT，改用 DataComponents.CUSTOM_DATA（vanilla CustomData）承载，键名与 1.20.1 完全一致。
 * 对应 useless_mod 的 UComponents 中各组件。
 */
public final class WandModes
{
    public static final String FORCE_KILL = "WandForceKill";
    public static final String BEEF_CAPTURE = "WandCapture";
    public static final String BEEF_INVULNERABILITY = "WandInvulnerability";
    public static final String CHAIN_MINING = "WandChainMining";
    public static final String FORCE_MINING = "WandForceMining";
    public static final String TOOL_MODES = "ToolModes";
    public static final String SILK_TOUCH_MODE = "SilkTouchMode";
    public static final String ENHANCED_CHAIN_MINING = "EnhancedChainMining";
    public static final String CHAIN_MINING_PRESSED = "ChainMiningPressed";

    private WandModes() {}

    /** 读取整份 CUSTOM_DATA（无则返回空 tag）。 */
    public static CompoundTag getData(ItemStack stack)
    {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }

    public static boolean getBoolean(ItemStack stack, String key)
    {
        return getData(stack).getBoolean(key);
    }

    public static CompoundTag getCompound(ItemStack stack, String key)
    {
        CompoundTag tag = getData(stack);
        return tag.contains(key) ? tag.getCompound(key) : new CompoundTag();
    }

    /** 原地修改 CUSTOM_DATA（CustomData.update）。 */
    public static void update(ItemStack stack, Consumer<CompoundTag> consumer)
    {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, consumer);
    }

    public static void setBoolean(ItemStack stack, String key, boolean value)
    {
        update(stack, tag -> tag.putBoolean(key, value));
    }

    public static void setCompound(ItemStack stack, String key, CompoundTag value)
    {
        update(stack, tag -> tag.put(key, value));
    }

    public static boolean isForceKillEnabled(ItemStack stack)
    {
        return getBoolean(stack, FORCE_KILL);
    }

    public static void setForceKillEnabled(ItemStack stack, boolean enabled)
    {
        setBoolean(stack, FORCE_KILL, enabled);
    }

    public static boolean isBeefCaptureEnabled(ItemStack stack)
    {
        return getBoolean(stack, BEEF_CAPTURE);
    }

    public static void setBeefCaptureEnabled(ItemStack stack, boolean enabled)
    {
        setBoolean(stack, BEEF_CAPTURE, enabled);
    }

    /** 无敌标记默认开启（与原版 BeefInvulnerabilityEnabledComponent 默认 true 一致）。 */
    public static boolean isBeefInvulnerabilityEnabled(ItemStack stack)
    {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null)
        {
            return true;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(BEEF_INVULNERABILITY))
        {
            return true;
        }
        return tag.getBoolean(BEEF_INVULNERABILITY);
    }

    public static void setBeefInvulnerabilityEnabled(ItemStack stack, boolean enabled)
    {
        setBoolean(stack, BEEF_INVULNERABILITY, enabled);
    }

    public static boolean isChainMiningEnabled(ItemStack stack)
    {
        return getBoolean(stack, CHAIN_MINING);
    }

    public static void setChainMiningEnabled(ItemStack stack, boolean enabled)
    {
        setBoolean(stack, CHAIN_MINING, enabled);
    }

    public static boolean isForceMiningEnabled(ItemStack stack)
    {
        return getBoolean(stack, FORCE_MINING);
    }

    public static void setForceMiningEnabled(ItemStack stack, boolean enabled)
    {
        setBoolean(stack, FORCE_MINING, enabled);
    }
}
