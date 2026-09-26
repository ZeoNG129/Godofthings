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

    // ---- 特殊被动（阶段 2 第三批：生存便利 / 附魔 / 交易 / 奥术防护） ----
    public static final String GLOW = "glow";
    public static final String FIRE_PROTECT = "fire_protect";
    public static final String GLUTTONY = "gluttony";
    public static final String ARCANE_BULWARK = "arcane_bulwark";
    public static final String ARCANE_AMP = "arcane_amp";
    public static final String ARCANE_ADAPT = "arcane_adapt";
    public static final String SPELL_DAMPEN = "spell_dampen";
    public static final String SPELL_REFLECT = "spell_reflect";
    public static final String SPELL_PURGE = "spell_purge";
    public static final String SPELLBREAK = "spellbreak";
    public static final String ENCHANT_RANDOM = "enchant_random";
    public static final String ENCHANT_BREAK = "enchant_break";
    public static final String ENCHANT_OVER = "enchant_over";
    public static final String UNLIMITED_TRADES = "unlimited_trades";
    public static final String VILLAGER_MASTER = "villager_master";
    public static final String UNDERWATER_VISION = "underwater_vision";

    // ---- 机械共鸣（阶段 3：机器继承开关 + 选区技能） ----
    // 注意：参考模组的 machine_star（机械之星）是"前置核心"，自身无任何效果，
    // 按用户"删除前置需求"的要求不做（做了也只是一个点了没反应的节点）。
    // ---- 光环（阶段 4）----
    public static final String AURA_DAMAGE = "aura_damage";
    public static final String AURA_EMPOWER = "aura_empower";
    public static final String AURA_SPEED = "aura_speed";
    public static final String AURA_HEAL = "aura_heal";
    public static final String AURA_XP = "aura_xp";
    public static final String AURA_MAGNET = "aura_magnet";
    public static final String AURA_LOCK = "aura_lock";
    public static final String AURA_VOID = "aura_void";
    public static final String AURA_LOOT_VACUUM = "aura_loot_vacuum";
    public static final String CONTAINER_HAUL = "container_haul";
    public static final String PURIFY_FIELD = "purify_field";

    public static final String MACHINE_LOOT_BOMB = "machine_loot_bomb";
    public static final String MACHINE_UNBREAKABLE = "machine_unbreakable";
    public static final String MACHINE_MOB_DROP = "machine_mob_drop";
    public static final String MACHINE_BLOCK_DROP = "machine_block_drop";
    public static final String MACHINE_XP_GAIN = "machine_xp_gain";
    public static final String MACHINE_SPAWN_EGG = "machine_spawn_egg";
    public static final String MACHINE_MOB_HEAD = "machine_mob_head";
    public static final String MACHINE_AUTO_SMELT = "machine_auto_smelt";

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

        // ══════════ 特殊被动（阶段 2 第三批） ══════════
        // 发光：附近生物发光（35 格）
        reg(GLOW, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.GLOW);
        // 烈焰不侵：常驻抗火
        reg(FIRE_PROTECT, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.FIRE_PROTECT);
        // 暴食：进食瞬间完成
        reg(GLUTTONY, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.GLUTTONY);
        // 奥术壁垒：每级 +6 魔法防御（减伤 = 防/(防+1200)，渐进不封顶）
        reg(ARCANE_BULWARK, ArmorSkillCategory.SPECIAL, 100, ArmorSkillDef.EffectKind.ARCANE_BULWARK);
        // 奥术真解：每级再 +4 魔法防御
        reg(ARCANE_AMP, ArmorSkillCategory.SPECIAL, 100, ArmorSkillDef.EffectKind.ARCANE_AMP);
        // 适应之躯：每次受魔法伤害 +2% 减伤（最多 60%）
        reg(ARCANE_ADAPT, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.ARCANE_ADAPT);
        // 法术抑制：弹射物/法术额外减伤（防/(防+800)，每级 +5 防御）
        reg(SPELL_DAMPEN, ArmorSkillCategory.SPECIAL, 100, ArmorSkillDef.EffectKind.SPELL_DAMPEN);
        // 法术反射：30% 概率把魔法伤害反弹给施法者
        reg(SPELL_REFLECT, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.SPELL_REFLECT);
        // 驱法破咒：每 5 秒清除自己与附近友方各一个负面效果
        reg(SPELL_PURGE, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.SPELL_PURGE);
        // 破法之刃：目标每有一个增益，对其伤害 +15%（最多 +60%）
        reg(SPELLBREAK, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.SPELLBREAK);
        // 随机附魔：铁砧 + 4 青金石 + 1 级经验 → 随机正面附魔
        reg(ENCHANT_RANDOM, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.ENCHANT_RANDOM);
        // 附魔突破：铁砧 + 2 青金石块 + 4 级经验 → 已有附魔 +1 级（上限 20）
        reg(ENCHANT_BREAK, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.ENCHANT_BREAK);
        // 超限附魔：铁砧 + 2 下界之星 + 10 级经验 → 已有附魔 +1 级（上限 100）
        reg(ENCHANT_OVER, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.ENCHANT_OVER);
        // 无限交易：村民交易不消耗次数
        reg(UNLIMITED_TRADES, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.UNLIMITED_TRADES);
        // 村民大师：右键村民使其成为大师级
        reg(VILLAGER_MASTER, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.VILLAGER_MASTER);
        // 碧波清眸：水下/岩浆清晰视野（客户端雾效完全禁用，见 FogRendererMixin）
        reg(UNDERWATER_VISION, ArmorSkillCategory.SPECIAL, 1, ArmorSkillDef.EffectKind.UNDERWATER_VISION);

        // ══════════ 光环（阶段 4）══════════
        // 脉动间隔基础 200 tick；疾攻之势每级 -10%；杀戮领域每级 +10% 伤害、修罗杀域再乘 +10%
        reg(AURA_DAMAGE, ArmorSkillCategory.AURA, 50, ArmorSkillDef.EffectKind.AURA_DAMAGE);
        reg(AURA_EMPOWER, ArmorSkillCategory.AURA, 50, ArmorSkillDef.EffectKind.AURA_EMPOWER);
        reg(AURA_SPEED, ArmorSkillCategory.AURA, 10, ArmorSkillDef.EffectKind.AURA_SPEED);
        reg(AURA_HEAL, ArmorSkillCategory.AURA, 50, ArmorSkillDef.EffectKind.AURA_HEAL);
        reg(AURA_XP, ArmorSkillCategory.AURA, 50, ArmorSkillDef.EffectKind.AURA_XP);
        reg(AURA_MAGNET, ArmorSkillCategory.AURA, 1, ArmorSkillDef.EffectKind.AURA_MAGNET);
        reg(AURA_LOCK, ArmorSkillCategory.AURA, 1, ArmorSkillDef.EffectKind.AURA_LOCK);
        reg(AURA_VOID, ArmorSkillCategory.AURA, 50, ArmorSkillDef.EffectKind.AURA_VOID);
        reg(AURA_LOOT_VACUUM, ArmorSkillCategory.AURA, 1, ArmorSkillDef.EffectKind.AURA_LOOT_VACUUM);
        reg(CONTAINER_HAUL, ArmorSkillCategory.AURA, 1, ArmorSkillDef.EffectKind.CONTAINER_HAUL);
        reg(PURIFY_FIELD, ArmorSkillCategory.AURA, 1, ArmorSkillDef.EffectKind.PURIFY_FIELD);
        // ══════════ 机械共鸣（阶段 3） ══════════
        // 八个「共鸣」开关：开启后，模拟玩家机器（FakePlayer，如数字型采矿机）才能继承对应效果；
        // 关闭立即回收（事件每次实时判定，无持久状态）。真玩家不受这些开关影响。
        reg(MACHINE_LOOT_BOMB, ArmorSkillCategory.MACHINE, 1, ArmorSkillDef.EffectKind.MACHINE_RESONANCE);
        reg(MACHINE_UNBREAKABLE, ArmorSkillCategory.MACHINE, 1, ArmorSkillDef.EffectKind.MACHINE_RESONANCE);
        reg(MACHINE_MOB_DROP, ArmorSkillCategory.MACHINE, 1, ArmorSkillDef.EffectKind.MACHINE_RESONANCE);
        reg(MACHINE_BLOCK_DROP, ArmorSkillCategory.MACHINE, 1, ArmorSkillDef.EffectKind.MACHINE_RESONANCE);
        reg(MACHINE_XP_GAIN, ArmorSkillCategory.MACHINE, 1, ArmorSkillDef.EffectKind.MACHINE_RESONANCE);
        reg(MACHINE_SPAWN_EGG, ArmorSkillCategory.MACHINE, 1, ArmorSkillDef.EffectKind.MACHINE_RESONANCE);
        reg(MACHINE_MOB_HEAD, ArmorSkillCategory.MACHINE, 1, ArmorSkillDef.EffectKind.MACHINE_RESONANCE);
        reg(MACHINE_AUTO_SMELT, ArmorSkillCategory.MACHINE, 1, ArmorSkillDef.EffectKind.MACHINE_RESONANCE);
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
                || category == ArmorSkillCategory.ULTIMATE || category == ArmorSkillCategory.SPECIAL
                || category == ArmorSkillCategory.MACHINE || category == ArmorSkillCategory.AURA;
    }
}
