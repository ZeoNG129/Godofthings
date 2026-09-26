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
            int level = ArmorSkillData.effectiveLevel(levels, def.id());
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
        double base = ArmorSkillData.effectiveLevel(levels, ArmorSkills.REGEN) * 2.0;
        if (base <= 0)
        {
            return 0;
        }
        double amp = ArmorSkillData.effectiveLevel(levels, ArmorSkills.AMP_REGEN) * 1.0;
        return base * (1 + amp);
    }

    /** 暴击几率（0~1，100% 封顶）：暴击要害每 UI 级 +1% */
    public static double critChance(Map<String, Integer> levels)
    {
        return Math.min(1.0, ArmorSkillData.effectiveLevel(levels, ArmorSkills.CRIT) * 0.01);
    }

    /** 暴击伤害倍率：基础 1.5 × (1 + 暴击真解 × 50%) */
    public static double critMultiplier(Map<String, Integer> levels)
    {
        double amp = ArmorSkillData.effectiveLevel(levels, ArmorSkills.AMP_CRIT) * 0.5;
        return 1.5 * (1 + amp);
    }

    /** 吸血率：min(1, 噬血之刃 × 1%) × (1 + 噬血真解 × 40%) */
    public static double lifestealRate(Map<String, Integer> levels)
    {
        double rate = Math.min(1.0, ArmorSkillData.effectiveLevel(levels, ArmorSkills.LIFESTEAL) * 0.01);
        if (rate <= 0)
        {
            return 0;
        }
        double amp = ArmorSkillData.effectiveLevel(levels, ArmorSkills.AMP_LIFESTEAL) * 0.4;
        return rate * (1 + amp);
    }

    /** 荆棘反伤值：荆棘护体 × 0.5 × (1 + 荆棘真解 × 40%) */
    public static double thornsDamage(Map<String, Integer> levels)
    {
        double base = ArmorSkillData.effectiveLevel(levels, ArmorSkills.THORNS) * 0.5;
        if (base <= 0)
        {
            return 0;
        }
        double amp = ArmorSkillData.effectiveLevel(levels, ArmorSkills.AMP_THORNS) * 0.4;
        return base * (1 + amp);
    }

    /** 破甲增伤比例：破甲利刃 × 1.5% × (1 + 破甲真解 × 40%) */
    public static double armorPenPercent(Map<String, Integer> levels)
    {
        double base = ArmorSkillData.effectiveLevel(levels, ArmorSkills.ARMOR_PEN) * 0.015;
        if (base <= 0)
        {
            return 0;
        }
        double amp = ArmorSkillData.effectiveLevel(levels, ArmorSkills.AMP_ARMOR_PEN) * 0.4;
        return base * (1 + amp);
    }

    // ══════════ 阶段 2：收益倍率 / 常驻效果 / 飞行 ══════════

    /** 生物掉落倍率（猎魂丰收）：1 + 等级（每级 +1 倍） */
    public static double mobDropMultiplier(Map<String, Integer> levels)
    {
        return 1 + ArmorSkillData.effectiveLevel(levels, ArmorSkills.MOB_DROP);
    }

    /** 方块掉落倍率（点石成金）：1 + 等级 */
    public static double blockDropMultiplier(Map<String, Integer> levels)
    {
        return 1 + ArmorSkillData.effectiveLevel(levels, ArmorSkills.BLOCK_DROP);
    }

    /** 战利品爆炸（财源滚滚）：1 + 等级（每级掉落翻一倍） */
    public static double lootBombMultiplier(Map<String, Integer> levels)
    {
        return 1 + ArmorSkillData.effectiveLevel(levels, ArmorSkills.LOOT_BOMB);
    }

    /** 经验倍率（经验飞涨）：1 + 等级 × 2 */
    public static double xpMultiplier(Map<String, Integer> levels)
    {
        return 1 + ArmorSkillData.effectiveLevel(levels, ArmorSkills.XP_GAIN) * 2.0;
    }

    /** 真创造飞行（宇宙的青睐）：已开启即 true */
    public static boolean hasFlight(Map<String, Integer> levels)
    {
        return ArmorSkillData.isEnabled(levels, ArmorSkills.ULT_FAVOR);
    }

    /** 一条常驻药水效果 */
    public record EffectSpec(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {}

    /**
     * 常驻药水效果（阶段 2 的特殊被动）：由 tick 周期性刷新。
     * <p>「破暗之瞳」不在这里——它是"持续清除黑暗"，见 {@link #removesDarkness}。
     */
    public static java.util.List<EffectSpec> passiveEffects(Map<String, Integer> levels)
    {
        java.util.List<EffectSpec> out = new java.util.ArrayList<>();
        if (ArmorSkillData.isEnabled(levels, ArmorSkills.NIGHT_VISION))
        {
            out.add(new EffectSpec(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 0));
        }
        if (ArmorSkillData.isEnabled(levels, ArmorSkills.SATURATION))
        {
            out.add(new EffectSpec(net.minecraft.world.effect.MobEffects.SATURATION, 0));
        }
        if (ArmorSkillData.isEnabled(levels, ArmorSkills.WATER_BREATHING))
        {
            out.add(new EffectSpec(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 0));
        }
        int hero = ArmorSkillData.effectiveLevel(levels, ArmorSkills.VILLAGE_HERO);
        if (hero > 0)
        {
            out.add(new EffectSpec(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE, hero - 1));
        }
        return out;
    }

    /** 破暗之瞳：是否持续清除黑暗效果 */
    public static boolean removesDarkness(Map<String, Integer> levels)
    {
        return ArmorSkillData.isEnabled(levels, ArmorSkills.DARK_VISION);
    }

    // ══════════ 阶段 2 第二批：战斗大招 / 掉落生产 ══════════

    /** 便捷：某技能是否已开启 */
    public static boolean isOn(Map<String, Integer> levels, String skillId)
    {
        return ArmorSkillData.isEnabled(levels, skillId);
    }

    /** 死神凝视：处决血量阈值（目标生命占比低于此值才可能被处决） */
    public static final float REAPER_THRESHOLD = 0.15f;
    /** 死神凝视：触发概率 */
    public static final float REAPER_CHANCE = 0.30f;
    /** 死神凝视：处决伤害（护甲减伤后仍足以秒杀） */
    public static final float REAPER_DAMAGE = 99999.0f;
    /** 凤凰涅槃：复活后保留的生命比例 */
    public static final float REVIVE_HEALTH_RATIO = 0.5f;
    /** 凤凰涅槃 / 虚空神体：免死冷却（tick） */
    public static final int UNDYING_COOLDOWN = 1200;
    /** 奥术神体：魔法伤害减免比例 */
    public static final float ARCANE_REDUCTION = 0.35f;

    /** 妖魂凝卵：掉落刷怪蛋的概率（每级 10%） */
    public static double spawnEggChance(Map<String, Integer> levels)
    {
        return Math.min(1.0, ArmorSkillData.effectiveLevel(levels, ArmorSkills.MOB_SPAWN_EGG) * 0.10);
    }

    /** 斩首夺颅：掉落头颅的概率（每级 20%） */
    public static double headDropChance(Map<String, Integer> levels)
    {
        return Math.min(1.0, ArmorSkillData.effectiveLevel(levels, ArmorSkills.MOB_HEAD) * 0.20);
    }

    /** 该生物类型对应的头颅物品（无对应则返回 null） */
    public static net.minecraft.world.item.Item headItemFor(net.minecraft.world.entity.EntityType<?> type)
    {
        if (type == net.minecraft.world.entity.EntityType.ZOMBIE) return net.minecraft.world.item.Items.ZOMBIE_HEAD;
        if (type == net.minecraft.world.entity.EntityType.SKELETON) return net.minecraft.world.item.Items.SKELETON_SKULL;
        if (type == net.minecraft.world.entity.EntityType.WITHER_SKELETON) return net.minecraft.world.item.Items.WITHER_SKELETON_SKULL;
        if (type == net.minecraft.world.entity.EntityType.CREEPER) return net.minecraft.world.item.Items.CREEPER_HEAD;
        if (type == net.minecraft.world.entity.EntityType.ENDER_DRAGON) return net.minecraft.world.item.Items.DRAGON_HEAD;
        if (type == net.minecraft.world.entity.EntityType.PIGLIN) return net.minecraft.world.item.Items.PIGLIN_HEAD;
        if (type == net.minecraft.world.entity.EntityType.PLAYER) return net.minecraft.world.item.Items.PLAYER_HEAD;
        if (type == net.minecraft.world.entity.EntityType.ZOMBIFIED_PIGLIN) return net.minecraft.world.item.Items.ZOMBIE_HEAD;
        return null;
    }

    /** 该生物类型对应的刷怪蛋（原版 byId 查表；没有对应刷怪蛋返回 null） */
    public static net.minecraft.world.item.Item spawnEggFor(net.minecraft.world.entity.EntityType<?> type)
    {
        return net.minecraft.world.item.SpawnEggItem.byId(type);
    }

    // ══════════ 阶段 2 第三批：奥术防护 / 附魔 / 交易 ══════════

    /** 法术反射概率 */
    public static final float SPELL_REFLECT_CHANCE = 0.30f;
    /** 驱法破咒间隔（tick） */
    public static final int SPELL_PURGE_INTERVAL = 100;
    /** 破法之刃：目标每个增益的增伤，与上限 */
    public static final float SPELLBREAK_PER_BUFF = 0.15f;
    public static final float SPELLBREAK_MAX = 0.60f;
    /** 适应之躯：每层减伤与上限 */
    public static final float ADAPT_STEP = 0.02f;
    public static final float ADAPT_MAX = 0.60f;
    /** 发光半径（格） */
    public static final double GLOW_RADIUS = 35.0;

    /**
     * 防御值 → 减伤率（移植自参考模组）。
     * <p>{@code red = def / (def + k)}：渐进逼近 1、永不封顶，且有效生命 {@code EHP = 1 + def/k}
     * 随防御线性增长 —— 所以每一级的价值恒定，可以放心支持大等级上限（不会"第 5 级就封顶"）。
     */
    public static double defenseToReduction(double defense, double k)
    {
        if (defense <= 0)
        {
            return 0;
        }
        return defense / (defense + Math.max(1.0e-6, k));
    }

    /** 奥术防御值：壁垒每级 +6、奥术真解每级再 +4（已把原 mod 的 ×10 等级压缩烘进来） */
    public static double arcaneDefense(Map<String, Integer> levels)
    {
        double def = ArmorSkillData.effectiveLevel(levels, ArmorSkills.ARCANE_BULWARK) * 6.0;
        if (def <= 0)
        {
            return 0;
        }
        def += ArmorSkillData.effectiveLevel(levels, ArmorSkills.ARCANE_AMP) * 4.0;
        return def;
    }

    /**
     * 魔法减伤率：奥术壁垒公式（k=1200），并与「奥术神体」的 −35% 乘算叠加。
     * <p>上限 99.99% 仅为防除零，不是设计封顶（公式本身在有限等级内到不了 100%）。
     */
    public static double magicReduction(Map<String, Integer> levels)
    {
        double red = defenseToReduction(arcaneDefense(levels), 1200.0);
        if (red <= 0)
        {
            return 0;
        }
        if (ArmorSkillData.isEnabled(levels, ArmorSkills.ULT_ARCANE_BODY))
        {
            red = 1.0 - (1.0 - red) * (1.0 - ARCANE_REDUCTION);
        }
        return Math.min(0.9999, red);
    }

    /** 法术抑制：仅对弹射物/法术（间接伤害）生效的额外减伤（每级 +5 防御，k=800） */
    public static double dampenReduction(Map<String, Integer> levels)
    {
        double def = ArmorSkillData.effectiveLevel(levels, ArmorSkills.SPELL_DAMPEN) * 5.0;
        return defenseToReduction(def, 800.0);
    }

    /** 破法之刃：按目标身上的增益数量增伤（每层 +15%，最多 +60%） */
    public static double spellbreakBonus(int beneficialEffects)
    {
        return Math.min(SPELLBREAK_MAX, beneficialEffects * SPELLBREAK_PER_BUFF);
    }

    // ══════════ 光环（阶段 4）══════════
    /** 光环脉动基础间隔（tick） */
    public static final int AURA_BASE_INTERVAL = 200;
    /** 杀戮领域半径（格） */
    public static final double AURA_ATTACK_RADIUS = 20.0;
    /** 虚空诛灭半径（格） */
    public static final double AURA_VOID_RADIUS = 50.0;
    /** 吸星大法半径（格） */
    public static final double AURA_MAGNET_RADIUS = 20.0;
    /** 回春妙手半径（格） */
    public static final double AURA_HEAL_RADIUS = 10.0;
    /** 净化领域半径（格） */
    public static final double PURIFY_RADIUS = 8.0;
    /** 吸星大法每 tick 最多处理的实体数 */
    public static final int MAGNET_MAX_PER_TICK = 64;
    /** 净化领域间隔（tick） */
    public static final int PURIFY_INTERVAL = 60;

    /** 光环脉动间隔：基础 200 tick，疾攻之势每级 -10%，下限 20 */
    public static int auraInterval(Map<String, Integer> levels)
    {
        int lv = ArmorSkillData.effectiveLevel(levels, ArmorSkills.AURA_SPEED);
        double factor = Math.max(0.05, 1.0 - 0.10 * lv);
        return (int) Math.max(20, Math.round(AURA_BASE_INTERVAL * factor));
    }

    /** 光环伤害倍率：杀戮领域 1+10%/级，再乘 修罗杀域 1+10%/级 */
    public static double auraDamageMultiplier(Map<String, Integer> levels)
    {
        double base = 1.0 + 0.10 * ArmorSkillData.effectiveLevel(levels, ArmorSkills.AURA_DAMAGE);
        double amp = 1.0 + 0.10 * ArmorSkillData.effectiveLevel(levels, ArmorSkills.AURA_EMPOWER);
        return base * amp;
    }

    /** 回春妙手每次脉动的治疗量（最大生命的比例）：2%/级 */
    public static double auraHealRatio(Map<String, Integer> levels)
    {
        return 0.02 * ArmorSkillData.effectiveLevel(levels, ArmorSkills.AURA_HEAL);
    }

    /** 汲灵之环每次脉动给予的经验：1/级 */
    public static int auraXpPerPulse(Map<String, Integer> levels)
    {
        return ArmorSkillData.effectiveLevel(levels, ArmorSkills.AURA_XP);
    }

    /** 虚空诛灭的处决阈值（目标血量比例）：5% + 1%/级 */
    public static double voidExecuteRatio(Map<String, Integer> levels)
    {
        return 0.05 + 0.01 * ArmorSkillData.effectiveLevel(levels, ArmorSkills.AURA_VOID);
    }
}
