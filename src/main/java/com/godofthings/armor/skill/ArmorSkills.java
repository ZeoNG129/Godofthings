package com.godofthings.armor.skill;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForgeMod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.godofthings.armor.skill.ArmorSkillDef.AttrEffect.add;
import static com.godofthings.armor.skill.ArmorSkillDef.AttrEffect.mult;
import static com.godofthings.armor.skill.ArmorSkillDef.EffectKind.ATTR;

/**
 * 神之套装技能表（阶段 1：基础属性 15 + 特殊增幅 15）。
 * <p>
 * <b>数值与公式移植自 Zifeng Skill Tree（子枫的百宝箱）</b>
 * （Copyright (c) 2026 zifeng, MIT License —— 见 README 的「第三方代码与许可」一節）。
 * <p>
 * <b>单位换算（重要）</b>：原 mod 有 10 倍等级压缩（1000 级 → 100 级），
 * 效果计算时 {@code effLevel = UI等级 × 10}。本移植把 ×10 直接烘进每级数值，
 * 所以表中所有数值都是 <b>每 UI 等级</b>的量，界面上显示几级就按几级算，不需要再乘。
 * 例：原 {@code BODY_HP_PER_POINT = 2.0} → 本表 {@code 20.0}/级。
 * <p>
 * 与原 mod 的差异（按需求刻意为之）：无技能点消耗、无前置需求，左键点击即解锁。
 */
public final class ArmorSkills
{
    // ---- 基础属性（纵列 1） ----
    public static final String BODY_HP = "body_hp";
    public static final String TOUGH = "tough";
    public static final String BLADE = "blade";
    public static final String ATTACK_SPEED = "attack_speed";
    public static final String MINING = "mining";
    public static final String MOVE = "move";
    public static final String REGEN = "regen";
    public static final String LUCK = "luck";
    public static final String JUMP = "jump";
    public static final String FLY = "fly";
    public static final String SWIM = "swim";
    public static final String CRIT = "crit";
    public static final String LIFESTEAL = "lifesteal";
    public static final String THORNS = "thorns";
    public static final String ARMOR_PEN = "armor_pen";

    // ---- 特殊增幅（纵列 2，与基础一一对应） ----
    public static final String AMP_HP = "amp_hp";
    public static final String AMP_TOUGH = "amp_tough";
    public static final String AMP_DAMAGE = "amp_damage";
    public static final String AMP_ATTACK_SPEED = "amp_attack_speed";
    public static final String AMP_MINING = "amp_mining";
    public static final String AMP_MOVE = "amp_move";
    public static final String AMP_REGEN = "amp_regen";
    public static final String AMP_LUCK = "amp_luck";
    public static final String AMP_JUMP = "amp_jump";
    public static final String AMP_FLY = "amp_fly";
    public static final String AMP_SWIM = "amp_swim";
    public static final String AMP_CRIT = "amp_crit";
    public static final String AMP_LIFESTEAL = "amp_lifesteal";
    public static final String AMP_THORNS = "amp_thorns";
    public static final String AMP_ARMOR_PEN = "amp_armor_pen";

    /** 基础/增幅类每项等级上限（原 mod 压缩后的上限） */
    public static final int BASE_MAX_LEVEL = 100;
    public static final int AMPLIFY_MAX_LEVEL = 50;

    /** 默认解锁等级（左键点击开启时给的等级） */
    public static final int UNLOCK_LEVEL = 1;

    private static final Map<String, ArmorSkillDef> BY_ID = new LinkedHashMap<>();
    private static final List<ArmorSkillDef> ALL = new ArrayList<>();

    static
    {
        // ══════════ 基础属性（固定数值，ADD_VALUE） ══════════
        reg(BODY_HP, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ATTR,
                add(Attributes.MAX_HEALTH, 20.0));
        reg(TOUGH, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ATTR,
                add(Attributes.ARMOR, 20.0),
                add(Attributes.ARMOR_TOUGHNESS, 10.0));
        reg(BLADE, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ATTR,
                add(Attributes.ATTACK_DAMAGE, 10.0));
        reg(ATTACK_SPEED, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ATTR,
                add(Attributes.ATTACK_SPEED, 2.0));
        reg(MINING, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ATTR,
                add(Attributes.MINING_EFFICIENCY, 3.0));
        reg(MOVE, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ATTR,
                add(Attributes.MOVEMENT_SPEED, 0.05));
        reg(REGEN, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ArmorSkillDef.EffectKind.REGEN);
        reg(LUCK, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ATTR,
                add(Attributes.LUCK, 1.0));
        reg(JUMP, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ATTR,
                add(Attributes.JUMP_STRENGTH, 0.1));
        reg(FLY, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ATTR,
                add(Attributes.FLYING_SPEED, 0.05));
        reg(SWIM, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ATTR,
                add(NeoForgeMod.SWIM_SPEED, 0.05));
        reg(CRIT, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ArmorSkillDef.EffectKind.CRIT);
        reg(LIFESTEAL, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ArmorSkillDef.EffectKind.LIFESTEAL);
        reg(THORNS, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ArmorSkillDef.EffectKind.THORNS);
        reg(ARMOR_PEN, ArmorSkillCategory.BASE, BASE_MAX_LEVEL, ArmorSkillDef.EffectKind.ARMOR_PEN);

        // ══════════ 特殊增幅（百分比，ADD_MULTIPLIED_TOTAL） ══════════
        reg(AMP_HP, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ATTR,
                mult(Attributes.MAX_HEALTH, 1.0));
        reg(AMP_TOUGH, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ATTR,
                mult(Attributes.ARMOR, 1.0),
                mult(Attributes.ARMOR_TOUGHNESS, 1.0));
        reg(AMP_DAMAGE, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ATTR,
                mult(Attributes.ATTACK_DAMAGE, 1.0));
        reg(AMP_ATTACK_SPEED, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ATTR,
                mult(Attributes.ATTACK_SPEED, 1.0));
        reg(AMP_MINING, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ATTR,
                mult(Attributes.MINING_EFFICIENCY, 1.2));
        reg(AMP_MOVE, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ATTR,
                mult(Attributes.MOVEMENT_SPEED, 1.0));
        reg(AMP_REGEN, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ArmorSkillDef.EffectKind.REGEN);
        reg(AMP_LUCK, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ATTR,
                mult(Attributes.LUCK, 1.0));
        reg(AMP_JUMP, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ATTR,
                mult(Attributes.JUMP_STRENGTH, 1.0));
        reg(AMP_FLY, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ATTR,
                mult(Attributes.FLYING_SPEED, 1.0));
        reg(AMP_SWIM, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ATTR,
                mult(NeoForgeMod.SWIM_SPEED, 1.0));
        reg(AMP_CRIT, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ArmorSkillDef.EffectKind.CRIT);
        reg(AMP_LIFESTEAL, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ArmorSkillDef.EffectKind.LIFESTEAL);
        reg(AMP_THORNS, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ArmorSkillDef.EffectKind.THORNS);
        reg(AMP_ARMOR_PEN, ArmorSkillCategory.AMPLIFY, AMPLIFY_MAX_LEVEL, ArmorSkillDef.EffectKind.ARMOR_PEN);
    }

    private ArmorSkills()
    {
    }

    private static void reg(String id, ArmorSkillCategory category, int maxLevel,
                            ArmorSkillDef.EffectKind kind, ArmorSkillDef.AttrEffect... attrs)
    {
        ArmorSkillDef def = new ArmorSkillDef(id, category, maxLevel, List.of(attrs), kind);
        BY_ID.put(id, def);
        ALL.add(def);
    }

    public static ArmorSkillDef get(String id)
    {
        return BY_ID.get(id);
    }

    public static boolean exists(String id)
    {
        return BY_ID.containsKey(id);
    }

    public static List<ArmorSkillDef> all()
    {
        return ALL;
    }

    public static List<ArmorSkillDef> of(ArmorSkillCategory category)
    {
        List<ArmorSkillDef> out = new ArrayList<>();
        for (ArmorSkillDef def : ALL)
        {
            if (def.category() == category)
            {
                out.add(def);
            }
        }
        return out;
    }

    /** 该分类在当前阶段是否已开放（阶段 1 只有基础 / 增幅） */
    public static boolean isAvailable(ArmorSkillCategory category)
    {
        return category == ArmorSkillCategory.BASE || category == ArmorSkillCategory.AMPLIFY;
    }
}
