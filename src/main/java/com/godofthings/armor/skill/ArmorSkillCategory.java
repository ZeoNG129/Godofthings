package com.godofthings.armor.skill;

/**
 * 神之套装技能树的分类（对应技能树界面里的"列"）。
 * <p>
 * 阶段 1 只开放 {@link #BASE} 与 {@link #AMPLIFY}；其余分类按后续阶段逐个接入
 * （终极节点 / 特殊被动 / 光环 / 机械共鸣 / 魔法增幅）。
 * <p>
 * 数值与机制移植自 <b>Zifeng Skill Tree（子枫的百宝箱）</b>
 * （Copyright (c) 2026 zifeng, MIT License），已按神之套装的"无技能点、无前置、点击即解锁"重做。
 */
public enum ArmorSkillCategory
{
    /** 魔法增幅（其余模组兼容，需装对应模组才生效） */
    MAGIC("magic"),
    /** 基础属性（纯固定数值堆叠） */
    BASE("base"),
    /** 特殊增幅（百分比放大基础数值） */
    AMPLIFY("amplify"),
    /** 终极节点（成长型大招） */
    ULTIMATE("ultimate"),
    /** 特殊被动（一次性奇技） */
    SPECIAL("special"),
    /** 杀戮光环（独立系统） */
    AURA("aura"),
    /** 机械共鸣（机器继承开关） */
    MACHINE("machine");

    private final String key;

    ArmorSkillCategory(String key)
    {
        this.key = key;
    }

    /** 语言文件后缀（gui.godofthings.armor.skill.cat.<key>） */
    public String getKey()
    {
        return key;
    }

    public String getLangKey()
    {
        return "gui.godofthings.armor.skill.cat." + key;
    }
}
