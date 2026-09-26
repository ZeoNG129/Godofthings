package com.godofthings.armor.skill;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;

/**
 * 一个神之套装技能的定义。
 * <p>
 * 数值体系移植自 <b>Zifeng Skill Tree（子枫的百宝箱）</b>（Copyright (c) 2026 zifeng, MIT）。
 * 原 mod 有 10 倍"等级压缩"（1000 级 → 100 级），效果在计算时统一 ×10；
 * 本移植把该倍率**直接烘进 {@link AttrEffect#perLevel()}**，因此这里的数值都是
 * <b>「每 UI 等级」</b>的量（= 原 perPoint × 10），界面上显示几级就按几级算。
 *
 * @param id       技能 id（与原 mod 的 skillId 一致，便于对照）
 * @param category 所属分类
 * @param maxLevel 等级上限（原 mod 压缩后的上限）
 * @param attrs    属性型效果（{@link EffectKind#ATTR} 时使用）
 * @param kind     效果类型
 */
public record ArmorSkillDef(
        String id,
        ArmorSkillCategory category,
        int maxLevel,
        List<AttrEffect> attrs,
        EffectKind kind)
{
    /** 效果类型：属性型 or 需要事件驱动的机制型 */
    public enum EffectKind
    {
        /** 直接挂属性修饰符 */
        ATTR,
        /** 每秒回血（生生不息） */
        REGEN,
        /** 暴击（暴击要害 / 暴击真解） */
        CRIT,
        /** 吸血（噬血之刃 / 噬血真解） */
        LIFESTEAL,
        /** 荆棘反伤（荆棘护体 / 荆棘真解） */
        THORNS,
        /** 破甲增伤（破甲利刃 / 破甲真解） */
        ARMOR_PEN,

        /** 夜视（星瞳夜视） */
        NIGHT_VISION,
        /** 饱食（饱食无忧） */
        SATURATION,
        /** 水下呼吸（鲛人之息） */
        WATER_BREATHING,
        /** 免疫黑暗（破暗之瞳） */
        DARK_VISION,
        /** 村庄英雄（万民敬仰） */
        VILLAGE_HERO,
        /** 真创造飞行（宇宙的青睐） */
        FLIGHT,
        /** 生物掉落倍率（猎魂丰收） */
        MOB_DROP,
        /** 方块掉落倍率（点石成金） */
        BLOCK_DROP,
        /** 战利品爆炸（财源滚滚） */
        LOOT_BOMB,
        /** 经验倍率（经验飞涨） */
        XP_GAIN,
        /** 不坏金身：常驻抗性/吸收/抗火 */
        GOLDEN,
        /** 凤凰涅槃：死亡原地复活 */
        REVIVE,
        /** 死神凝视：处决低血目标 */
        REAPER,
        /** 奥术神体：魔法减伤 */
        ARCANE_BODY,
        /** 虚空神体：免疫击退 + 免死兜底 */
        VOID_BODY,
        /** 万载不磨：工具/护甲不消耗耐久 */
        UNBREAKABLE,
        /** 自动熔炼：方块掉落自动熔炼 */
        AUTO_SMELT,
        /** 万物可掘：可挖不可破坏方块 */
        BREAK_ALL,
        /** 不朽铭文：铁砧加"无法破坏"词条 */
        UNBREAK_TAG,
        /** 妖魂凝卵：掉落刷怪蛋 */
        MOB_SPAWN_EGG,
        /** 斩首夺颅：掉落头颅 */
        MOB_HEAD,
        /** 发光（附近生物发光） */
        GLOW,
        /** 烈焰不侵（常驻抗火） */
        FIRE_PROTECT,
        /** 暴食（进食瞬间完成） */
        GLUTTONY,
        /** 奥术壁垒（魔法防御值） */
        ARCANE_BULWARK,
        /** 奥术真解（追加魔法防御值） */
        ARCANE_AMP,
        /** 适应之躯（受击叠层减伤） */
        ARCANE_ADAPT,
        /** 法术抑制（弹射物/法术额外减伤） */
        SPELL_DAMPEN,
        /** 法术反射 */
        SPELL_REFLECT,
        /** 驱法破咒（周期性清除负面） */
        SPELL_PURGE,
        /** 破法之刃（按目标增益数增伤） */
        SPELLBREAK,
        /** 随机附魔（铁砧） */
        ENCHANT_RANDOM,
        /** 附魔突破（铁砧） */
        ENCHANT_BREAK,
        /** 超限附魔（铁砧） */
        ENCHANT_OVER,
        /** 无限交易 */
        UNLIMITED_TRADES,
        /** 村民大师 */
        VILLAGER_MASTER,
        /** 光环：杀戮领域（范围脉动伤害） */
        AURA_DAMAGE,
        /** 光环：修罗杀域（光环伤害增幅） */
        AURA_EMPOWER,
        /** 光环：疾攻之势（缩短光环脉动间隔） */
        AURA_SPEED,
        /** 光环：回春妙手（范围脉动治疗） */
        AURA_HEAL,
        /** 光环：汲灵之环（脉动给经验） */
        AURA_XP,
        /** 光环：吸星大法（吸取周围掉落物/经验球） */
        AURA_MAGNET,
        /** 光环：定身神域（免疫传送与击退） */
        AURA_LOCK,
        /** 光环：虚空诛灭（远处低血目标直接处决） */
        AURA_VOID,
        /** 光环：挪移术（掉落物直入绑定容器） */
        AURA_LOOT_VACUUM,
        /** 光环：搬运术（背包物品定期送入绑定容器） */
        CONTAINER_HAUL,
        /** 光环：净化领域（范围清除负面效果） */
        PURIFY_FIELD,
        /** 碧波清眸：水下/岩浆清晰视野（客户端雾效） */
        UNDERWATER_VISION,
        /** 机械共鸣：允许模拟玩家机器继承对应效果 */
        MACHINE_RESONANCE,
        /** 防护选区：范围内友好生物免疫你的伤害 */
        /** 选区放置：一键用主手方块填满范围内空位 */
    }

    /**
     * 一条属性加成。
     *
     * @param attribute 目标属性
     * @param perLevel  每 UI 等级数值
     * @param op        {@code ADD_VALUE}（加算）或 {@code ADD_MULTIPLIED_TOTAL}（乘算）
     */
    public record AttrEffect(Holder<Attribute> attribute, double perLevel, AttributeModifier.Operation op)
    {
        public static AttrEffect add(Holder<Attribute> attribute, double perLevel)
        {
            return new AttrEffect(attribute, perLevel, AttributeModifier.Operation.ADD_VALUE);
        }

        public static AttrEffect mult(Holder<Attribute> attribute, double perLevel)
        {
            return new AttrEffect(attribute, perLevel, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }

    /** 属性修饰符 id（每个技能独立，避免同属性技能互相覆盖） */
    public net.minecraft.resources.ResourceLocation modifierId()
    {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("godofthings", "armor_skill_" + id);
    }

    /** 语言文件键：名称 / 说明 */
    public String nameKey()
    {
        return "gui.godofthings.armor.skill." + id;
    }

    public String descKey()
    {
        return "gui.godofthings.armor.skill." + id + ".desc";
    }
}
