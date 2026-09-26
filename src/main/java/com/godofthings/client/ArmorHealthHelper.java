package com.godofthings.client;

import net.minecraft.world.entity.player.Player;

/**
 * 血条压缩与真实血量读取工具。
 * <p>
 * <b>移植自 Zifeng Skill Tree（子枫的百宝箱）的 {@code HealthBarHelper}</b>
 * （Copyright (c) 2026 zifeng, MIT License，见 README「第三方代码与许可」）。
 *
 * <h3>为什么需要它</h3>
 * 神之套装技能树的「血魄淬炼」（100 级 × +20 生命 = +2000）与「血魄真解」
 * （50 级 × +100% = ×51）可把最大生命推到十万量级。而原版血条是<b>每 2 点生命画 1 颗心</b>
 * → 心数可达数万颗 → 既铺满屏幕挡视野，又让每帧 blit 次数爆炸导致掉帧。
 *
 * <h3>做法：只压缩"喂给原版渲染器的数值"，不改原版渲染代码</h3>
 * 由 {@link com.godofthings.mixin.GuiHeartsCompressMixin} 把原版血条取到的
 * 当前生命 / 最大生命 / 伤害吸收等比压到 {@link #COMPRESSED_MAX} 以内，
 * 让原版自己照常画（永远最多 10 颗心 = 一行）；真实生命值完全不变（属性/伤害/存档不受影响）。
 * 真实数值由 {@link ArmorHealthHud} 以数字形式显示在血条左侧。
 *
 * <h3>安全阈值</h3>
 * 真实最大生命 ≤ {@link #COMPRESS_THRESHOLD}（原版 20 点 / 10 颗心）时<b>完全不干预</b>，
 * 原版行为 100% 保留。
 */
public final class ArmorHealthHelper
{
    private ArmorHealthHelper() {}

    /** 压缩阈值：真实最大生命超过此值才启用压缩（20.0 = 原版 10 颗心） */
    public static final float COMPRESS_THRESHOLD = 20.0f;

    /** 压缩后的最大生命（20.0 → 最多 10 颗心 = 1 行） */
    public static final float COMPRESSED_MAX = 20.0f;

    public static boolean shouldCompress(float realMaxHealth)
    {
        return realMaxHealth > COMPRESS_THRESHOLD;
    }

    /** 压缩后的"最大生命"（喂给原版渲染器用）；未超阈值时原样返回 */
    public static float compressMax(float realMaxHealth)
    {
        return shouldCompress(realMaxHealth) ? COMPRESSED_MAX : realMaxHealth;
    }

    /** 等比压缩一个"生命量"（当前生命 / 吸收量） */
    public static float compressValue(float value, float realMaxHealth)
    {
        if (!shouldCompress(realMaxHealth))
        {
            return value;
        }
        return value * (COMPRESSED_MAX / realMaxHealth);
    }

    /**
     * 压缩伤害吸收量。
     * <p>基准取 {@code max(最大生命, 吸收量)}：否则"最大生命不超阈值但吸收盾极大"时
     * 吸收心仍会爆炸成上千颗。
     */
    public static float compressAbsorption(float realAbsorption, float realMaxHealth)
    {
        float basis = Math.max(realMaxHealth, realAbsorption);
        if (!shouldCompress(basis))
        {
            return realAbsorption;
        }
        return realAbsorption * (COMPRESSED_MAX / basis);
    }

    // ---- 真实值读取（自动兼容其他模组加的生命/吸收） ----

    public static float realHealth(Player player)
    {
        return player != null ? player.getHealth() : 0.0f;
    }

    public static float realMaxHealth(Player player)
    {
        return player != null ? player.getMaxHealth() : 20.0f;
    }

    public static float realAbsorption(Player player)
    {
        return player != null ? player.getAbsorptionAmount() : 0.0f;
    }
}
