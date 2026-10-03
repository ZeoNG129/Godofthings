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
    /**
     * 效果类型：属性型 or 需要事件驱动的机制型。
     * <p><b>v5.9.0</b>：技能表只剩 7 个开关式掉落节点（外加通用的 {@link #ATTR}），
     * 其余已删技能的机制类型一并移除。</p>
     */
    public enum EffectKind
    {
        /** 直接挂属性修饰符（通用：{@link ArmorSkillDef#attrs()} 非空时使用） */
        ATTR,
        /** 神之掉落（原「财源滚滚」）：战利品爆炸，每级掉落翻一倍 */
        LOOT_BOMB,
        /** 神之生物（原「猎魂丰收」）：生物掉落倍率 */
        MOB_DROP,
        /** 神之方块（原「点石成金」）：方块掉落倍率 */
        BLOCK_DROP,
        /** 神之经验（原「经验飞涨」）：经验倍率 */
        XP_GAIN,
        /** 神之怪蛋（原「妖魂凝卵」）：掉落刷怪蛋 */
        MOB_SPAWN_EGG,
        /** 神之头颅（原「斩首夺颅」）：掉落头颅 */
        MOB_HEAD,
        /** 神之熔炼（原「自动熔炼」）：方块掉落自动熔炼 */
        AUTO_SMELT,
        /** 神之共鸣：开关型，打开后机器才继承对应的那个增幅（默认关） */
        MACHINE_RESONANCE,
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
