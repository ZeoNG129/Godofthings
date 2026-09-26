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

    // ---- 终极节点（阶段 2 第一批：属性型 / 收益型 / 常驻效果型） ----
    public static final String ULT_BLOOD = "ult_blood";
    public static final String ULT_MASTER = "ult_master";
    public static final String ULT_KB_RESIST = "ult_kb_resist";
    public static final String ULT_SWEEP = "ult_sweep";
    public static final String ULT_FAVOR = "ult_favor";
    public static final String LOOT_BOMB = "loot_bomb";
    public static final String MOB_DROP = "mob_drop";
    public static final String BLOCK_DROP = "block_drop";
    public static final String XP_GAIN = "xp_gain";

    // ---- 特殊被动（阶段 2 第一批） ----
    public static final String REACH = "reach";
    public static final String NIGHT_VISION = "night_vision";
    public static final String SATURATION = "saturation";
    public static final String WATER_BREATHING = "water_breathing";
    public static final String DARK_VISION = "dark_vision";
    public static final String VILLAGE_HERO = "village_hero";

    // ---- 终极节点（阶段 2 第二批：战斗大招 / 掉落生产） ----
    public static final String AMP_ARMOR = "amp_armor";
    public static final String ULT_GOLDEN = "ult_golden";
    public static final String ULT_REVIVE = "ult_revive";
    public static final String ULT_REAPER = "ult_reaper";
    public static final String ULT_ARCANE_BODY = "ult_arcane_body";
    public static final String ULT_VOID_BODY = "ult_void_body";
    public static final String UNBREAKABLE = "unbreakable";
    public static final String MOB_SPAWN_EGG = "mob_spawn_egg";
    public static final String MOB_HEAD = "mob_head";
    public static final String AUTO_SMELT = "auto_smelt";
    public static final String ULT_BREAK_ALL = "ult_break_all";
    public static final String ULT_UNBREAK_TAG = "ult_unbreak_tag";

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

        // ══════════ 终极节点（阶段 2 第一批） ══════════
        // 浴血奋战：常驻攻击与生命 +50%（1 级，等级无关；数值取自参考模组 bloodAttackBonus/bloodHealthBonus）
        reg(ULT_BLOOD, ArmorSkillCategory.ULTIMATE, 1, ATTR,
                mult(Attributes.ATTACK_DAMAGE, 0.5),
                mult(Attributes.MAX_HEALTH, 0.5));
        // 全能精通：全属性 +25%（1 级；数值取自参考模组 masterBonus）
        reg(ULT_MASTER, ArmorSkillCategory.ULTIMATE, 1, ATTR,
                mult(Attributes.MAX_HEALTH, 0.25),
                mult(Attributes.ARMOR, 0.25),
                mult(Attributes.ARMOR_TOUGHNESS, 0.25),
                mult(Attributes.ATTACK_DAMAGE, 0.25),
                mult(Attributes.ATTACK_SPEED, 0.25),
                mult(Attributes.MINING_EFFICIENCY, 0.25),
                mult(Attributes.MOVEMENT_SPEED, 0.25),
                mult(Attributes.LUCK, 0.25),
                mult(Attributes.JUMP_STRENGTH, 0.25),
                mult(Attributes.FLYING_SPEED, 0.25),
                mult(NeoForgeMod.SWIM_SPEED, 0.25));
        // 稳如泰山：每级 +10% 击退抗性（上限 10 级 = 100% 免疫击退）
        reg(ULT_KB_RESIST, ArmorSkillCategory.ULTIMATE, 10, ATTR,
                add(Attributes.KNOCKBACK_RESISTANCE, 0.1));
        // 横扫千军：每级 +1 格攻击距离（上限 10）
        reg(ULT_SWEEP, ArmorSkillCategory.ULTIMATE, 10, ATTR,
                add(Attributes.ENTITY_INTERACTION_RANGE, 1.0));
        // 财源滚滚：战利品爆炸（每级掉落翻一倍，上限 100）
        reg(LOOT_BOMB, ArmorSkillCategory.ULTIMATE, 100, ArmorSkillDef.EffectKind.LOOT_BOMB);
        // 猎魂丰收：生物掉落倍率（每级 +1 倍，上限 10）
        reg(MOB_DROP, ArmorSkillCategory.ULTIMATE, 10, ArmorSkillDef.EffectKind.MOB_DROP);
        // 点石成金：方块掉落倍率（每级 +1 倍，上限 10）
        reg(BLOCK_DROP, ArmorSkillCategory.ULTIMATE, 10, ArmorSkillDef.EffectKind.BLOCK_DROP);
        // 经验飞涨：经验倍率（每级 +2 倍，上限 10）
        reg(XP_GAIN, ArmorSkillCategory.ULTIMATE, 10, ArmorSkillDef.EffectKind.XP_GAIN);
        // 宇宙的青睐：真创造飞行（1 级）
        reg(ULT_FAVOR, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.FLIGHT);

        // ══════════ 特殊被动（阶段 2 第一批） ══════════
        // 长臂善舞：每级 +1 格触摸/攻击距离（上限 50）
        reg(REACH, ArmorSkillCategory.SPECIAL, 50, ATTR,
                add(Attributes.ENTITY_INTERACTION_RANGE, 1.0),
                add(Attributes.BLOCK_INTERACTION_RANGE, 1.0));
        // 星瞳夜视：夜视（1 级）
        reg(NIGHT_VISION, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.NIGHT_VISION);
        // 饱食无忧：饱食（1 级）
        reg(SATURATION, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.SATURATION);
        // 鲛人之息：水下呼吸（1 级）
        reg(WATER_BREATHING, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.WATER_BREATHING);
        // 破暗之瞳：免疫黑暗（1 级，持续清除黑暗效果）
        reg(DARK_VISION, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.DARK_VISION);
        // 万民敬仰：村庄英雄（每级 1 级效果，上限 10）
        reg(VILLAGE_HERO, ArmorSkillCategory.SPECIAL, 10, ArmorSkillDef.EffectKind.VILLAGE_HERO);

        // ══════════ 终极节点（阶段 2 第二批） ══════════
        // 金身真解：每级 +1% 物理减伤（自定义属性，上限 80 级 = 80%）
        reg(AMP_ARMOR, ArmorSkillCategory.ULTIMATE, 80, ATTR,
                add(ModAttributes.DAMAGE_REDUCTION, 0.01));
        // 不坏金身：常驻 抗性提升 X / 伤害吸收 C / 抗火 V（无限时长）
        reg(ULT_GOLDEN, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.GOLDEN);
        // 凤凰涅槃：死亡原地复活（回 50% 血、清负面、5 秒吸收盾、冷却 60 秒）
        reg(ULT_REVIVE, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.REVIVE);
        // 死神凝视：目标生命低于 15% 时 30% 概率直接处决
        reg(ULT_REAPER, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.REAPER);
        // 奥术神体：魔法伤害 -35%
        reg(ULT_ARCANE_BODY, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.ARCANE_BODY);
        // 虚空神体：免疫击退 + 免死兜底（冷却 60 秒）
        reg(ULT_VOID_BODY, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.VOID_BODY);
        // 万载不磨：工具/护甲不消耗耐久
        reg(UNBREAKABLE, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.UNBREAKABLE);
        // 妖魂凝卵：每级 10% 概率掉落刷怪蛋（上限 10 级 = 100%）
        reg(MOB_SPAWN_EGG, ArmorSkillCategory.ULTIMATE, 10, ArmorSkillDef.EffectKind.MOB_SPAWN_EGG);
        // 斩首夺颅：每级 20% 概率掉落头颅（上限 5 级 = 100%）
        reg(MOB_HEAD, ArmorSkillCategory.ULTIMATE, 5, ArmorSkillDef.EffectKind.MOB_HEAD);
        // 自动熔炼：方块掉落自动熔炼成成品
        reg(AUTO_SMELT, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.AUTO_SMELT);
        // 万物可掘：可挖基岩等不可破坏方块（需手持镐子）
        reg(ULT_BREAK_ALL, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.BREAK_ALL);
        // 不朽铭文：铁砧中用两个相同物品合成"无法破坏"工具
        reg(ULT_UNBREAK_TAG, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.UNBREAK_TAG);
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
        return category == ArmorSkillCategory.BASE || category == ArmorSkillCategory.AMPLIFY
                || category == ArmorSkillCategory.ULTIMATE || category == ArmorSkillCategory.SPECIAL;
    }
}
