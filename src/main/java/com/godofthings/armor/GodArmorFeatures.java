package com.godofthings.armor;

/**
 * 神之套装（穿齐神之头/甲/腿/鞋）的全部功能开关位表。
 * <p>
 * 开关按玩家保存（NeoForge Data Attachment，见 {@link GodArmorState}），每个功能占 1 个 bit。
 * 默认 {@link #ALL} = 全开。
 * <p>
 * <b>v5.9.0 起按用户要求做过一次合并</b>：原来 16 个开关合并成 8 个
 * （飞行三项 → 神之飞行；四项免疫 → 神之无敌；夜视三项 → 神之视觉），
 * 「万民敬仰」（村庄英雄）改为<b>穿齐套装即生效</b>、不再占开关位。
 * 老存档里存的 16 位掩码由 {@link #migrateLegacy(int)} 迁移成新的 8 位（见 {@link GodArmorState}）。
 */
public final class GodArmorFeatures
{
    /** 开关总数（= 下面的常量个数） */
    public static final int COUNT = 8;

    /** 神之飞行：原「创造飞行 + 飞行无惯性 + 飞行挖掘不减速」 */
    public static final int FLIGHT = 0;
    /** 神之无敌：原「免疫所有伤害 + 不会死亡 + 免疫负面 + 无限氧气」 */
    public static final int INVINCIBLE = 1;
    /** 神之饱和：原「永不饥饿」 */
    public static final int SATURATION = 2;
    /** 神之抗火：原「火焰熔岩免疫」 */
    public static final int FIRE_RESIST = 3;
    /** 神之呼吸：原「水下呼吸」 */
    public static final int WATER_BREATH = 4;
    /** 神之视觉：原「无痕夜视 + 熔岩夜视 + 碧波清眸」 */
    public static final int VISION = 5;
    /** 神之透视：原「发光（附近生物发光）」 */
    public static final int SEE_THROUGH = 6;
    /** 神之贪吃：原「暴食（进食瞬间完成）」 */
    public static final int GLUTTONY = 7;

    /** 全部开启（默认值） */
    public static final int ALL = (1 << COUNT) - 1;

    /** 界面显示顺序 + 本地化键（下标 = 功能编号） */
    public static final String[] LANG_KEYS = {
            "gui.godofthings.armor.flight",
            "gui.godofthings.armor.invincible",
            "gui.godofthings.armor.saturation",
            "gui.godofthings.armor.fire_resist",
            "gui.godofthings.armor.water_breath",
            "gui.godofthings.armor.vision",
            "gui.godofthings.armor.see_through",
            "gui.godofthings.armor.gluttony",
    };

    /** 界面顶部用来说明「不占开关的常驻效果」 */
    public static final String ALWAYS_ON_LANG_KEY = "gui.godofthings.armor.always_on";

    /** 旧版（16 位）掩码里各功能的位号，迁移时按名字对照，不要改 */
    private static final int OLD_FLIGHT = 0;
    private static final int OLD_FLIGHT_INERTIA = 1;
    private static final int OLD_FLIGHT_MINING = 2;
    private static final int OLD_DAMAGE_IMMUNITY = 3;
    private static final int OLD_NO_DEATH = 4;
    private static final int OLD_DEBUFF_IMMUNITY = 5;
    private static final int OLD_NO_HUNGER = 6;
    private static final int OLD_FIRE_IMMUNITY = 7;
    private static final int OLD_WATER_BREATHING = 8;
    private static final int OLD_NIGHT_VISION = 9;
    private static final int OLD_LAVA_VISION = 10;
    private static final int OLD_OXYGEN = 11;
    private static final int OLD_GLOW = 13;
    private static final int OLD_GLUTTONY = 14;
    private static final int OLD_UNDERWATER_VISION = 15;

    private GodArmorFeatures() {}

    public static boolean isOn(int mask, int feature)
    {
        return feature >= 0 && feature < COUNT && (mask & (1 << feature)) != 0;
    }

    /** 只保留已定义的 bit，防客户端塞入未知位 */
    public static int sanitize(int mask)
    {
        return mask & ALL;
    }

    /**
     * 把 v5.8.0 及以前的 16 位掩码迁移成新的 8 位。
     *
     * <p>合并语义：只要被合并的旧开关里有<b>任意一个</b>是开的，合并后的新开关就是开的
     * （合并后的功能本来就包含了原来的每一项，取「或」才符合玩家预期）。
     * 「万民敬仰」（旧位 12）现在常驻生效，不再参与。</p>
     */
    public static int migrateLegacy(int legacy)
    {
        int out = 0;
        if (anyOn(legacy, OLD_FLIGHT, OLD_FLIGHT_INERTIA, OLD_FLIGHT_MINING))
        {
            out |= 1 << FLIGHT;
        }
        if (anyOn(legacy, OLD_DAMAGE_IMMUNITY, OLD_NO_DEATH, OLD_DEBUFF_IMMUNITY, OLD_OXYGEN))
        {
            out |= 1 << INVINCIBLE;
        }
        if (anyOn(legacy, OLD_NO_HUNGER))
        {
            out |= 1 << SATURATION;
        }
        if (anyOn(legacy, OLD_FIRE_IMMUNITY))
        {
            out |= 1 << FIRE_RESIST;
        }
        if (anyOn(legacy, OLD_WATER_BREATHING))
        {
            out |= 1 << WATER_BREATH;
        }
        if (anyOn(legacy, OLD_NIGHT_VISION, OLD_LAVA_VISION, OLD_UNDERWATER_VISION))
        {
            out |= 1 << VISION;
        }
        if (anyOn(legacy, OLD_GLOW))
        {
            out |= 1 << SEE_THROUGH;
        }
        if (anyOn(legacy, OLD_GLUTTONY))
        {
            out |= 1 << GLUTTONY;
        }
        return out;
    }

    private static boolean anyOn(int mask, int... bits)
    {
        for (int bit : bits)
        {
            if ((mask & (1 << bit)) != 0)
            {
                return true;
            }
        }
        return false;
    }
}
