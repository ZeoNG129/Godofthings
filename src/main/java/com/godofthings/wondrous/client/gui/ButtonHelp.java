package com.godofthings.wondrous.client.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Shared action help; contextual axis/list controls explicitly override their generic label help. */
public final class ButtonHelp {
    private ButtonHelp() {}
    public static Component text(String id, Object... args) {
        return Component.translatable("help.godofthings." + id, args);
    }
    public static void set(AbstractWidget widget, String id, Object... args) {
        widget.setTooltip(Tooltip.create(text(id, args)));
    }
    public static void automatic(AbstractWidget widget, Component label) {
        String id = null;
        if (label.getContents() instanceof TranslatableContents translated) {
            id = switch (translated.getKey()) {
                case "gui.godofthings.back", "gui.done" -> "back";
                case "gui.godofthings.speed_off" -> "speed_off";
                case "gui.godofthings.mode_normal" -> "normal";
                case "gui.godofthings.mode_permanent" -> "permanent";
                case "gui.godofthings.mode_permanent_no_throttle" -> "no_throttle";
                case "gui.godofthings.staff_config.master" -> "master";
                case "gui.godofthings.range.open" -> "range_setup";
                case "gui.godofthings.range.history" -> "history";
                case "gui.godofthings.range.edit" -> "edit";
                case "gui.godofthings.range.reclaim" -> "reclaim";
                case "gui.godofthings.staff_config.on", "gui.godofthings.staff_config.off" -> "pause";
                case "gui.godofthings.history.machines" -> "machine_history";
                case "gui.godofthings.range.placement" -> "placement";
                case "gui.godofthings.range.marking" -> "marking";
                case "gui.godofthings.range.offset_short" -> "offset";
                case "gui.godofthings.reclaimer.category_count" -> "category";
                case "gui.godofthings.reclaimer.refresh" -> "refresh";
                case "gui.godofthings.staff_summon.open" -> "summon_open";
                case "gui.godofthings.staff_apotheosis.open" -> "apotheosis_open";
                case "gui.godofthings.staff_config.loot_refresh" -> "loot";
                case "gui.godofthings.staff_config.loot_refresh_disabled", "gui.godofthings.staff_config.summon_disabled",
                        "gui.godofthings.staff_summon.disabled",
                        "gui.godofthings.staff_apotheosis.disabled",
                        "gui.godofthings.staff_apotheosis.missing_enchanting" -> "disabled";
                case "gui.godofthings.staff_summon.mode", "gui.godofthings.staff_summon.mode_short" -> "summon_mode";
                case "gui.godofthings.staff_summon.select_visible" -> "select_visible";
                case "gui.godofthings.staff_summon.clear" -> "clear_selection";
                case "gui.godofthings.staff_summon.summon" -> "summon";
                case "gui.godofthings.staff_summon.recall" -> "recall";
                case "gui.godofthings.clear" -> "clear_patterns";
                default -> null;
            };
        } else {
            id = switch (label.getString()) {
                case "+" -> "increase";
                case "-" -> "decrease";
                case "<" -> "previous";
                case ">" -> "next";
                default -> label.getString().startsWith("x") ? "speed" : null;
            };
        }
        if (id != null) set(widget, id);
    }
}
