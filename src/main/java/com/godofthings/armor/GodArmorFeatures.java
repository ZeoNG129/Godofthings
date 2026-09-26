package com.godofthings.armor;

/**
 * 神之套装（穿齐神之头/甲/腿/鞋）的全部功能开关位表。
 * <p>
 * 开关按玩家保存（NeoForge Data Attachment，见 {@link GodArmorState}），每个功能占 1 个 bit。
 * 默认 {@link #ALL} = 全开，与加入本开关之前的旧行为完全一致（旧存档没有该附件时也走默认值）。
 * <p>
 * 开关本身只做「是否启用」判定，仍需穿齐全套才生效（{@link GodArmorState#active}）。
 */
public final class GodArmorFeatures
{
    /** 开关总数（= 下面的常量个数） */
    public static final int COUNT = 12;

    public static final int FLIGHT = 0;
    public static final int FLIGHT_INERTIA = 1;
    public static final int FLIGHT_MINING = 2;
    public static final int DAMAGE_IMMUNITY = 3;
    public static final int NO_DEATH = 4;
    public static final int DEBUFF_IMMUNITY = 5;
    public static final int NO_HUNGER = 6;
    public static final int FIRE_IMMUNITY = 7;
    public static final int WATER_BREATHING = 8;
    public static final int NIGHT_VISION = 9;
    public static final int LAVA_VISION = 10;
    public static final int OXYGEN = 11;

    /** 全部开启（默认值） */
    public static final int ALL = (1 << COUNT) - 1;

    /** 界面显示顺序 + 本地化键（下标 = 功能编号） */
    public static final String[] LANG_KEYS = {
            "gui.godofthings.armor.flight",
            "gui.godofthings.armor.flight_inertia",
            "gui.godofthings.armor.flight_mining",
            "gui.godofthings.armor.damage_immunity",
            "gui.godofthings.armor.no_death",
            "gui.godofthings.armor.debuff_immunity",
            "gui.godofthings.armor.no_hunger",
            "gui.godofthings.armor.fire_immunity",
            "gui.godofthings.armor.water_breathing",
            "gui.godofthings.armor.night_vision",
            "gui.godofthings.armor.lava_vision",
            "gui.godofthings.armor.oxygen",
    };

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
}
