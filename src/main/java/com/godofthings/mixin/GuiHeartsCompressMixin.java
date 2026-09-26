package com.godofthings.mixin;

import com.godofthings.client.ArmorHealthHelper;
import net.minecraft.client.gui.Gui;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 血条压缩（客户端）。
 * <p>
 * <b>移植自 Zifeng Skill Tree（子枫的百宝箱）的 {@code GuiHeartsCompressMixin}</b>
 * （Copyright (c) 2026 zifeng, MIT License，见 README「第三方代码与许可」）。
 *
 * <h3>原则（兼容性优先）</h3>
 * <b>原版血条渲染代码一个字都不改</b> —— 只把「喂给原版渲染器的数值」等比压缩，
 * 让原版自己照常画（永远最多 10 颗心 = 一行）。因此其他血条 mod 接管渲染后照常生效，
 * 真实生命值也完全不受影响（只影响显示）。
 *
 * <h3>为什么只改 renderHealthLevel 的 3 个取值点就够</h3>
 * <pre>
 *   int   i  = ceil(getHealth())                        // ① 当前生命
 *   float f  = max(getAttributeValue(MAX_HEALTH), ...)  // ② 最大生命（决定心数与行数）
 *   int   k1 = ceil(getAbsorptionAmount())              // ③ 伤害吸收
 *   → 行数 / 行距 / leftHeight（HUD 高度累加）/ renderHearts 的 4 个入参 全部由这 3 处派生
 * </pre>
 * 改掉这 3 处后，心数、行数、以及 HUD 高度累加全部自动一致（否则饥饿条会被推到屏幕外）。
 *
 * <h3>安全措施</h3>
 * <ul>
 *   <li>真实最大生命 ≤ 20（原版 10 颗心）时 {@link ArmorHealthHelper} 原样返回 → <b>零干预</b></li>
 *   <li>全部 {@code require = 0}：注入失败（版本差异 / 与其他 mod 冲突）只静默跳过，绝不崩游戏</li>
 * </ul>
 */
@Mixin(Gui.class)
public abstract class GuiHeartsCompressMixin
{
    /** ① 当前生命：等比压缩（喂给原版画心用） */
    @Redirect(
            method = "renderHealthLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getHealth()F"),
            require = 0
    )
    private float godofthings$compressHealth(Player player)
    {
        float real = player.getHealth();
        float realMax = player.getMaxHealth();
        if (!ArmorHealthHelper.shouldCompress(realMax))
        {
            return real; // 未超阈值：原版行为，零干预
        }
        return ArmorHealthHelper.compressValue(real, realMax);
    }

    /** ② 最大生命：压缩到阈内（决定心数与行数，并修正 HUD 高度累加） */
    @Redirect(
            method = "renderHealthLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"),
            require = 0
    )
    private double godofthings$compressMaxHealth(Player player, Holder<Attribute> attribute)
    {
        double real = player.getAttributeValue(attribute);
        if (!ArmorHealthHelper.shouldCompress(player.getMaxHealth()))
        {
            return real; // 未超阈值：原版行为，零干预
        }
        return ArmorHealthHelper.compressMax((float) real);
    }

    /** ③ 伤害吸收：同样压缩（基准取 max(最大生命, 吸收量)，防海量吸收盾爆炸） */
    @Redirect(
            method = "renderHealthLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getAbsorptionAmount()F"),
            require = 0
    )
    private float godofthings$compressAbsorption(Player player)
    {
        float real = player.getAbsorptionAmount();
        float realMax = player.getMaxHealth();
        if (!ArmorHealthHelper.shouldCompress(Math.max(realMax, real)))
        {
            return real; // 未超阈值：原版行为，零干预
        }
        return ArmorHealthHelper.compressAbsorption(real, realMax);
    }
}
