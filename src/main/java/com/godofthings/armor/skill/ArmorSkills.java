package com.godofthings.armor.skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 神之增幅技能表（v5.9.0 起只剩 7 个开关式节点）。
 *
 * <p>这 7 个节点都是「让神之系列机器 / 掉落产出更强」的效果，按用户要求：</p>
 * <ul>
 *   <li>全部都是<b>开关</b>：界面上点一下开启 = 直接给到该节点的<b>最大等级</b>，再点一下关闭；</li>
 *   <li>id 沿用参考模组（Zifeng Skill Tree，MIT）的 skillId，便于对照；界面上显示的名字由语言键决定
 *       （{@code gui.godofthings.armor.skill.<id>}），所以改名不需要动代码。</li>
 * </ul>
 *
 * <p>原「基础属性 / 特殊增幅 / 机械共鸣 / 魔法增幅」四类与其余终极节点已按用户要求整体删除
 * （上游数值对照关系见 VERSIONING.md 的 5.9.0 条目）。</p>
 */
public final class ArmorSkills
{
    // ---- 神之增幅（7 个开关式节点）----
    /** 神之掉落（原「财源滚滚」）：战利品爆炸，每级掉落翻一倍 */
    public static final String LOOT_BOMB = "loot_bomb";
    /** 神之生物（原「猎魂丰收」）：生物掉落倍率 */
    public static final String MOB_DROP = "mob_drop";
    /** 神之方块（原「点石成金」）：方块掉落倍率 */
    public static final String BLOCK_DROP = "block_drop";
    /** 神之经验（原「经验飞涨」）：经验倍率 */
    public static final String XP_GAIN = "xp_gain";
    /** 神之怪蛋（原「妖魂凝卵」）：掉落刷怪蛋 */
    public static final String MOB_SPAWN_EGG = "mob_spawn_egg";
    /** 神之头颅（原「斩首夺颅」）：掉落头颅 */
    public static final String MOB_HEAD = "mob_head";
    /** 神之熔炼（原「自动熔炼」）：方块掉落自动熔炼 */
    public static final String AUTO_SMELT = "auto_smelt";

    /** 默认解锁等级（列表里"开关打开"时给的最低等级；终极节点开启时直接给满级，见界面代码） */
    public static final int UNLOCK_LEVEL = 1;

    private static final Map<String, ArmorSkillDef> BY_ID = new LinkedHashMap<>();
    private static final List<ArmorSkillDef> ALL = new ArrayList<>();

    static
    {
        reg(LOOT_BOMB, ArmorSkillCategory.ULTIMATE, 100, ArmorSkillDef.EffectKind.LOOT_BOMB);
        reg(MOB_DROP, ArmorSkillCategory.ULTIMATE, 10, ArmorSkillDef.EffectKind.MOB_DROP);
        reg(BLOCK_DROP, ArmorSkillCategory.ULTIMATE, 10, ArmorSkillDef.EffectKind.BLOCK_DROP);
        reg(XP_GAIN, ArmorSkillCategory.ULTIMATE, 10, ArmorSkillDef.EffectKind.XP_GAIN);
        reg(MOB_SPAWN_EGG, ArmorSkillCategory.ULTIMATE, 10, ArmorSkillDef.EffectKind.MOB_SPAWN_EGG);
        reg(MOB_HEAD, ArmorSkillCategory.ULTIMATE, 5, ArmorSkillDef.EffectKind.MOB_HEAD);
        reg(AUTO_SMELT, ArmorSkillCategory.ULTIMATE, 1, ArmorSkillDef.EffectKind.AUTO_SMELT);
    }

    private ArmorSkills() {}

    private static ArmorSkillDef reg(String id, ArmorSkillCategory category, int maxLevel,
                                     ArmorSkillDef.EffectKind kind)
    {
        ArmorSkillDef def = new ArmorSkillDef(id, category, maxLevel, List.of(), kind);
        BY_ID.put(id, def);
        ALL.add(def);
        return def;
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

    /** 该分类是否已开放（现在只有「神之增幅」一个分类） */
    public static boolean isAvailable(ArmorSkillCategory category)
    {
        return category == ArmorSkillCategory.ULTIMATE;
    }
}
