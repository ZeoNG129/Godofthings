package com.godofthings.armor.skill;

/**
 * 神之套装技能树的分类。
 *
 * <p><b>v5.9.0 起按用户要求只保留一个分类</b>：原「终极节点」改名为「神之增幅」，
 * 「基础属性」「特殊增幅」「机械共鸣」「魔法增幅」四类整体删除。
 * 界面上的另一页是「神之套装」（{@code GodArmorFeatures} 的开关位），不属于技能分类。</p>
 *
 * <p>数值与机制原样移植自 Zifeng Skill Tree（子枫的百宝箱，MIT），
 * 本模组按「无技能点、无前置、点击开关」重做。</p>
 */
public enum ArmorSkillCategory
{
    /** 神之增幅（7 个开关式节点：让神之系列机器/掉落产出翻倍或附加效果） */
    ULTIMATE("ultimate"),

    /** 神之共鸣（7 个开关，**默认关**：打开后对应的神之系列机器会继承上面同名的那个增幅） */
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
