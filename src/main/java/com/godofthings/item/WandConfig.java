package com.godofthings.item;

import java.util.List;
import java.util.Set;

/**
 * 造化垂青之杖的静态配置（沿用 useless_mod 的默认值）。
 * 后续如需可配置化，可将这些常量替换为 Forge 配置文件读取。
 */
public final class WandConfig
{
    private WandConfig() {}

    /** 基础挖掘速度（原 beef_tool_mining_speed 默认 10.0）。 */
    public static double getBeefToolMiningSpeed()
    {
        return 10.0;
    }

    /** 抢夺附魔等级（原 looting_level 默认 10）。 */
    public static int getLootingLevel()
    {
        return 10;
    }

    /** 时运附魔等级（原 fortune_level 默认 10）。 */
    public static int getFortuneLevel()
    {
        return 10;
    }

    /** 连锁挖掘最大方块数量（原默认 1000）。 */
    public static int getChainMiningMaxBlocks()
    {
        return 1000;
    }

    /** 连锁挖掘 X 轴范围半径（原默认 8）。 */
    public static int getChainMiningRangeX()
    {
        return 8;
    }

    /** 连锁挖掘 Y 轴范围半径（原默认 8）。 */
    public static int getChainMiningRangeY()
    {
        return 8;
    }

    /** 连锁挖掘 Z 轴范围半径（原默认 8）。 */
    public static int getChainMiningRangeZ()
    {
        return 8;
    }

    /** 强制击杀黑名单（实体 ID，分号分隔；原默认为空）。 */
    public static Set<String> getBeefToolForceKillBlacklist()
    {
        return Set.of();
    }

    /** 非生物实体强制击杀白名单（原默认 draconicevolution:guardian_crystal）。 */
    public static Set<String> getBeefToolForceKillNonLivingWhitelist()
    {
        return Set.of("draconicevolution:guardian_crystal");
    }

    /** 是否启用药水效果。 */
    public static boolean shouldEnablePotionEffects()
    {
        return true;
    }

    /** 自定义药水效果列表，格式 "modid:effect,amplifier"（原默认值）。 */
    public static List<String> getCustomPotionEffects()
    {
        return List.of(
                "minecraft:saturation,1",
                "minecraft:regeneration,6",
                "minecraft:night_vision,1",
                "minecraft:fire_resistance,1",
                "minecraft:water_breathing,1",
                "minecraft:resistance,6"
        );
    }
}
