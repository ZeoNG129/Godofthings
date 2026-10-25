package com.godofthings.beef.api.enums.tool;

import net.minecraft.network.chat.Component;

public enum ConstructionWandCoreMode {
    DEFAULT("tooltip.godofthings.construction_wand_default_core"),
    ANGEL("tooltip.godofthings.construction_wand_angel_core"),
    DESTRUCTION("tooltip.godofthings.construction_wand_destruction_core");

    private final String tooltipKey;

    ConstructionWandCoreMode(String tooltipKey) {
        this.tooltipKey = tooltipKey;
    }

    public Component getTooltip() {
        return Component.translatable(this.tooltipKey);
    }
}
