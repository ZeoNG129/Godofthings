package com.godofthings.beef.api.enums.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ToolTypeMode {
    NONE_MODE("none_mode", "tooltip.godofthings.none_mode"),
    WRENCH_MODE("wrench_mode", "tooltip.godofthings.wrench_mode"),
    SCREWDRIVER_MODE("screwdriver_mode", "tooltip.godofthings.screwdriver_mode"),
    MALLET_MODE("mallet_mode", "tooltip.godofthings.mallet_mode"),
    CROWBAR_MODE("crowbar_mode", "tooltip.godofthings.crowbar_mode"),
    HAMMER_MODE("hammer_mode", "tooltip.godofthings.hammer_mode"),
    OMNITOOL_MODE("omnitool_mode", "tooltip.godofthings.omnitool_mode");

    private final String name;
    private final String tooltipKey;

    ToolTypeMode(String name, String tooltipKey) {
        this.name = name;
        this.tooltipKey = tooltipKey;
    }

    public static int getTotal() {return values().length;}

    public String getName() {return this.name;}

    public Component getTooltip() {return Component.translatable(this.tooltipKey);}

    public Component getTooltip(ChatFormatting... styles) {
        return Component.translatable(this.tooltipKey).withStyle(styles);
    }
}