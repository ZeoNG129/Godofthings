package com.godofthings.armor.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.Map;

/**
 * 神之套装技能的效果引擎。
 * <p>
 * <b>统一公式</b>（移植自 Zifeng Skill Tree，Copyright (c) 2026 zifeng, MIT）：
 * <pre>
 *   最终属性 = (基础值 + Σ基础固定数值) × (1 + Σ增幅百分比)
 * </pre>
 * 利用原版 AttributeModifier 的计算顺序天然实现：基础类用 {@code ADD_VALUE}、
 * 增幅类用 {@code ADD_MULTIPLIED_TOTAL}，与原 mod 的 {@code SkillEffects.applyAll} 等价。
 * <p>
 * 本类所有"每级"数值都是<b>每 UI 等级</b>（原 mod 的 10 倍等级压缩已烘进 {@link ArmorSkills}）。
 */
public final class ArmorSkillEngine
{
    private ArmorSkillEngine() {}

    /**
     * 重新应用全部属性修饰符（幂等，可安全重挂）。
     * <p>
     * 遍历**全部**已定义技能（不只是已开启的），未开启/等级 0 的技能会把对应修饰符移除，
     * 因此关闭技能会立即生效，不会残留。
     */
    public static void applyAll(ServerPlayer player, Map<String, Integer> levels)
    {
        for (ArmorSkillDef def : ArmorSkills.all())
        {
            int level = ArmorSkillData.level(levels, def.id());
            for (ArmorSkillDef.AttrEffect effect : def.attrs())
            {
                double amount = level * effect.perLevel();
                apply(player, def, effect, amount);
            }
        }
    }

    /** 移除本模组挂上的全部技能修饰符（玩家失去全部技能时用）。 */
    public static void removeAll(ServerPlayer player)
    {
        for (ArmorSkillDef def : ArmorSkills.all())
        {
            for (ArmorSkillDef.AttrEffect effect : def.attrs())
            {
                apply(player, def, effect, 0);
            }
        }
    }

    private static void apply(ServerPlayer player, ArmorSkillDef def,
                              ArmorSkillDef.AttrEffect effect, double amount)
    {
        AttributeInstance instance = player.getAttribute(effect.attribute());
        if (instance == null)
        {
            return;
        }
        instance.removeModifier(def.modifierId());
        if (amount != 0)
        {
            instance.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    def.modifierId(), amount, effect.op()));
        }
    }

    // ══════════ 机制型数值（事件驱动，非属性） ══════════

    /** 每秒回血（生生不息 × (1 + 生生真解)）；每 UI 级基础 +2/秒，增幅每 UI 级 +100% */
    public static double regenPerSecond(Map<String, Integer> levels)
    {
        double base = ArmorSkillData.level(levels, ArmorSkills.REGEN) * 2.0;
        if (base <= 0)
        {
            return 0;
        }
        double amp = ArmorSkillData.level(levels, ArmorSkills.AMP_REGEN) * 1.0;
        return base * (1 + amp);
    }

    /** 暴击几率（0~1，100% 封顶）：暴击要害每 UI 级 +1% */
    public static double critChance(Map<String, Integer> levels)
    {
        return Math.min(1.0, ArmorSkillData.level(levels, ArmorSkills.CRIT) * 0.01);
    }

    /** 暴击伤害倍率：基础 1.5 × (1 + 暴击真解 × 50%) */
    public static double critMultiplier(Map<String, Integer> levels)
    {
        double amp = ArmorSkillData.level(levels, ArmorSkills.AMP_CRIT) * 0.5;
        return 1.5 * (1 + amp);
    }

    /** 吸血率：min(1, 噬血之刃 × 1%) × (1 + 噬血真解 × 40%) */
    public static double lifestealRate(Map<String, Integer> levels)
    {
        double rate = Math.min(1.0, ArmorSkillData.level(levels, ArmorSkills.LIFESTEAL) * 0.01);
        if (rate <= 0)
        {
            return 0;
        }
        double amp = ArmorSkillData.level(levels, ArmorSkills.AMP_LIFESTEAL) * 0.4;
        return rate * (1 + amp);
    }

    /** 荆棘反伤值：荆棘护体 × 0.5 × (1 + 荆棘真解 × 40%) */
    public static double thornsDamage(Map<String, Integer> levels)
    {
        double base = ArmorSkillData.level(levels, ArmorSkills.THORNS) * 0.5;
        if (base <= 0)
        {
            return 0;
        }
        double amp = ArmorSkillData.level(levels, ArmorSkills.AMP_THORNS) * 0.4;
        return base * (1 + amp);
    }

    /** 破甲增伤比例：破甲利刃 × 1.5% × (1 + 破甲真解 × 40%) */
    public static double armorPenPercent(Map<String, Integer> levels)
    {
        double base = ArmorSkillData.level(levels, ArmorSkills.ARMOR_PEN) * 0.015;
        if (base <= 0)
        {
            return 0;
        }
        double amp = ArmorSkillData.level(levels, ArmorSkills.AMP_ARMOR_PEN) * 0.4;
        return base * (1 + amp);
    }
}
