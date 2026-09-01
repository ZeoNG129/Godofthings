package com.godofthings.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

/**
 * 神之工具的按键绑定（客户端）。
 */
public class WandKeyBindings
{
    private static final String CATEGORY = "key.category.godofthings.wand";

    /** 精准采集 */
    public static final Lazy<KeyMapping> SWITCH_SILK_TOUCH_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.switch_silk_touch",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_PAGE_DOWN,
            CATEGORY
    ));
    /** 时运 */
    public static final Lazy<KeyMapping> SWITCH_FORTUNE_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.switch_fortune",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_PAGE_UP,
            CATEGORY
    ));
    /** 连锁挖掘（Tab 按住） */
    public static final Lazy<KeyMapping> SWITCH_CHAIN_MINING_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.chain_mining",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            CATEGORY
    ));
    /** 增强连锁挖掘 */
    public static final Lazy<KeyMapping> SWITCH_ENHANCED_CHAIN_MINING_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.enhanced_chain_mining",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_8,
            CATEGORY
    ));
    /** 模式轮盘（G 按住） */
    public static final Lazy<KeyMapping> SWITCH_MODE_WHEEL_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.switch_mode_wheel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    ));
    /** 强制挖掘模式 */
    public static final Lazy<KeyMapping> SWITCH_FORCE_MINING_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.switch_force_mining",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_9,
            CATEGORY
    ));
    /** 触发强制破坏（R） */
    public static final Lazy<KeyMapping> TRIGGER_FORCE_MINING_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.trigger_force_mining",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    ));
    /** 连锁挖掘模式开关 */
    public static final Lazy<KeyMapping> TOGGLE_CHAIN_MODE_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.toggle_chain_mode",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    ));
    /** 强制击杀 */
    public static final Lazy<KeyMapping> TOGGLE_FORCE_KILL_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.toggle_force_kill",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    ));
    /** 捕捉模式 */
    public static final Lazy<KeyMapping> TOGGLE_CAPTURE_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.toggle_capture",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    ));
    /** 无敌模式 */
    public static final Lazy<KeyMapping> TOGGLE_INVULNERABILITY_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.toggle_invulnerability",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I,
            CATEGORY
    ));
    /** 打开传送点界面（U） */
    public static final Lazy<KeyMapping> OPEN_WAYPOINT_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.open_waypoint",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U,
            CATEGORY
    ));

    /** 跟踪连锁挖掘按键的按下状态。 */
    public static boolean SWITCH_CHAIN_MINING_KEY_WAS_DOWN = false;

    private WandKeyBindings() {}

    public static void register(RegisterKeyMappingsEvent event)
    {
        event.register(SWITCH_SILK_TOUCH_KEY.get());
        event.register(SWITCH_FORTUNE_KEY.get());
        event.register(SWITCH_CHAIN_MINING_KEY.get());
        event.register(SWITCH_ENHANCED_CHAIN_MINING_KEY.get());
        event.register(SWITCH_MODE_WHEEL_KEY.get());
        event.register(SWITCH_FORCE_MINING_KEY.get());
        event.register(TRIGGER_FORCE_MINING_KEY.get());
        event.register(TOGGLE_CHAIN_MODE_KEY.get());
        event.register(TOGGLE_FORCE_KILL_KEY.get());
        event.register(TOGGLE_CAPTURE_KEY.get());
        event.register(TOGGLE_INVULNERABILITY_KEY.get());
        event.register(OPEN_WAYPOINT_KEY.get());
    }
}
