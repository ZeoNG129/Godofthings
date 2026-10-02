package com.godofthings.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

/**
 * 神之系列的按键绑定（客户端）。
 * <p>
 * 原「神之工具」自创的那些按键（精准采集/时运/连锁/强制挖掘/模式轮盘等）已随该实现一并退役；
 * 造化杖（太初洞见之杖）的按键由照抄进来的
 * {@code com.godofthings.beef.core.common.KeyBindings} 自行注册。
 * 这里只保留不属于造化杖的功能键。
 */
public class WandKeyBindings
{
    private static final String CATEGORY = "key.category.godofthings.wand";

    /** 打开传送点界面（U） */
    public static final Lazy<KeyMapping> OPEN_WAYPOINT_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.open_waypoint",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            CATEGORY
    ));
    /** 神之剑功能面板（J） */
    public static final Lazy<KeyMapping> SWORD_MODE_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.sword_mode",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY
    ));
    /** 神之套装功能开关界面（O） */
    public static final Lazy<KeyMapping> OPEN_ARMOR_CONFIG_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.open_armor_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
    ));

    /** 神之套装技能树界面（K） */
    public static final Lazy<KeyMapping> OPEN_ARMOR_SKILL_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.open_armor_skill",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    ));

    /** 神之便签记事本界面（N） */
    public static final Lazy<KeyMapping> OPEN_NOTE_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.open_note",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY
    ));

    /** 直接进「摆放悬浮窗」编辑模式（M）：不用先开记事本 */
    public static final Lazy<KeyMapping> EDIT_NOTE_HUD_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.edit_note_hud",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    ));

    private WandKeyBindings() {}

    public static void register(RegisterKeyMappingsEvent event)
    {
        event.register(OPEN_WAYPOINT_KEY.get());
        event.register(SWORD_MODE_KEY.get());
        event.register(OPEN_ARMOR_CONFIG_KEY.get());
        event.register(OPEN_ARMOR_SKILL_KEY.get());
        event.register(OPEN_NOTE_KEY.get());
        event.register(EDIT_NOTE_HUD_KEY.get());
    }
}
