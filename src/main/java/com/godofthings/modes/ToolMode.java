package com.godofthings.modes;

import net.minecraft.network.chat.Component;

/**
 * 造化垂青之杖的工具模式枚举。
 * 移植自 useless_mod 的 ToolMode（1.20.1），tooltip 键改为 godofthings 前缀。
 */
public enum ToolMode
{
    SILK_TOUCH("silk_touch", "tooltip.godofthings.silk_touch_mode", 0),
    FORTUNE("fortune", "tooltip.godofthings.fortune_mode", 1),
    CHAIN_MINING("chain_mining", "tooltip.godofthings.chain_mining_mode", 2),
    ENHANCED_CHAIN_MINING("enhanced_chain_mining", "tooltip.godofthings.enhanced_chain_mining_mode", 3),
    WRENCH_MODE("wrench_mode", "tooltip.godofthings.wrench_mode", 4),
    SCREWDRIVER_MODE("screwdriver_mode", "tooltip.godofthings.screwdriver_mode", 5),
    MALLET_MODE("mallet_mode", "tooltip.godofthings.mallet_mode", 6),
    CROWBAR_MODE("crowbar_mode", "tooltip.godofthings.crowbar_mode", 7),
    HAMMER_MODE("hammer_mode", "tooltip.godofthings.hammer_mode", 8),
    OMNITOOL_MODE("omnitool_mode", "tooltip.godofthings.omnitool_mode", 9),
    FORCE_MINING("force_mining", "tooltip.godofthings.force_mining_mode", 10),
    AE_STORAGE_PRIORITY("ae_storage_priority", "tooltip.godofthings.ae_storage_priority_mode", 11);

    private final String name;
    private final String tooltipKey;
    private final int index;

    ToolMode(String name, String tooltipKey, int index)
    {
        this.name = name;
        this.tooltipKey = tooltipKey;
        this.index = index;
    }

    public String getName()
    {
        return name;
    }

    public int getIndex()
    {
        return index;
    }

    public Component getTooltip()
    {
        return Component.translatable(tooltipKey);
    }

    public static ToolMode byIndex(int index)
    {
        for (ToolMode mode : values())
        {
            if (mode.index == index)
            {
                return mode;
            }
        }
        return SILK_TOUCH;
    }

    public static int getTotalModes()
    {
        return values().length;
    }
}
