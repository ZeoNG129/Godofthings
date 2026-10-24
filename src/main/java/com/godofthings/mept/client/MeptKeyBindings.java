package com.godofthings.mept.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * ME 放置工具的按键（照抄上游 ModKeyBindings，键 id 改 godofthings 命名空间，
 * 分类为「众神之物：ME 放置工具」）。
 * [本地改动] 四键默认全部不绑定（上游默认 G/G/Ctrl/A），到「按键控制」里自己设。
 */
public final class MeptKeyBindings
{
    public static final String CATEGORY = "key.categories.godofthings.mept";

    public static final KeyMapping OPEN_RADIAL_MENU = new KeyMapping(
            "key.godofthings.mept.radial_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    public static final KeyMapping UNDO_MODIFIER = new KeyMapping(
            "key.godofthings.mept.undo",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    public static final KeyMapping OPEN_CABLE_TOOL_GUI = new KeyMapping(
            "key.godofthings.mept.open_cable_tool_gui",
            KeyConflictContext.UNIVERSAL,  // 游戏内和界面里都生效
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    public static final KeyMapping MARK_COLOR_SHORTCUT = new KeyMapping(
            "key.godofthings.mept.mark_color",
            KeyConflictContext.GUI,  // 只在界面里生效
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );

    private MeptKeyBindings()
    {
    }
}
