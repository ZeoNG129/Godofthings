package com.godofthings.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 造化垂青之杖的模式标记存储（用 NBT 替代 1.21 的数据组件）。
 * 对应 useless_mod 的 UComponents 中各组件，本 mod 用物品栈 tag 保存。
 */
public final class WandModes
{
    private static final String FORCE_KILL = "WandForceKill";
    private static final String BEEF_CAPTURE = "WandCapture";
    private static final String BEEF_INVULNERABILITY = "WandInvulnerability";
    private static final String CHAIN_MINING = "WandChainMining";
    private static final String FORCE_MINING = "WandForceMining";

    private WandModes() {}

    public static boolean isForceKillEnabled(ItemStack stack)
    {
        return stack.getOrCreateTag().getBoolean(FORCE_KILL);
    }

    public static void setForceKillEnabled(ItemStack stack, boolean enabled)
    {
        stack.getOrCreateTag().putBoolean(FORCE_KILL, enabled);
    }

    public static boolean isBeefCaptureEnabled(ItemStack stack)
    {
        return stack.getOrCreateTag().getBoolean(BEEF_CAPTURE);
    }

    public static void setBeefCaptureEnabled(ItemStack stack, boolean enabled)
    {
        stack.getOrCreateTag().putBoolean(BEEF_CAPTURE, enabled);
    }

    /** 无敌标记默认开启（与原版 BeefInvulnerabilityEnabledComponent 默认 true 一致）。 */
    public static boolean isBeefInvulnerabilityEnabled(ItemStack stack)
    {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BEEF_INVULNERABILITY))
        {
            return true;
        }
        return tag.getBoolean(BEEF_INVULNERABILITY);
    }

    public static void setBeefInvulnerabilityEnabled(ItemStack stack, boolean enabled)
    {
        stack.getOrCreateTag().putBoolean(BEEF_INVULNERABILITY, enabled);
    }

    public static boolean isChainMiningEnabled(ItemStack stack)
    {
        return stack.getOrCreateTag().getBoolean(CHAIN_MINING);
    }

    public static void setChainMiningEnabled(ItemStack stack, boolean enabled)
    {
        stack.getOrCreateTag().putBoolean(CHAIN_MINING, enabled);
    }

    public static boolean isForceMiningEnabled(ItemStack stack)
    {
        return stack.getOrCreateTag().getBoolean(FORCE_MINING);
    }

    public static void setForceMiningEnabled(ItemStack stack, boolean enabled)
    {
        stack.getOrCreateTag().putBoolean(FORCE_MINING, enabled);
    }
}
