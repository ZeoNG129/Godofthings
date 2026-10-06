package com.godofthings.armor.skill;

/**
 * 一个神之套装技能的定义。
 * <p>
 * 数值体系移植自 <b>Zifeng Skill Tree（子枫的百宝箱）</b>（Copyright (c) 2026 zifeng, MIT）。
 * 原 mod 有 10 倍"等级压缩"（1000 级 → 100 级），效果在计算时统一 ×10；
 * 本移植把该倍率直接烘进各节点的事件侧换算函数（见 {@link ArmorSkillEngine}），
 * 因此这里的等级就是界面上显示几级就按几级算。
 *
 * <p><b>v5.12.1</b>：移除 {@code attrs} 组件（属性型效果列表）与 {@code modifierId()}。
 * v5.9.0 删掉全部属性类节点后该列表恒为空、属性引擎（{@code applyAll}）空转，
 * 三者一并删除 —— 现役节点全部走 {@link ArmorSkillDef.EffectKind 机制型} + 事件侧。</p>
 *
 * @param id       技能 id（与原 mod 的 skillId 一致，便于对照）
 * @param category 所属分类
 * @param maxLevel 等级上限（原 mod 压缩后的上限）
 * @param kind     效果类型
 */
public record ArmorSkillDef(
        String id,
        ArmorSkillCategory category,
        int maxLevel,
        EffectKind kind)
{
    /**
     * 效果类型：需要事件驱动的机制型。
     * <p><b>v5.9.0</b>：技能表只剩 7 个开关式掉落节点；
     * <b>v5.12.1</b>：随属性引擎删除通用类型 {@link #ATTR}。</p>
     */
    public enum EffectKind
    {
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
