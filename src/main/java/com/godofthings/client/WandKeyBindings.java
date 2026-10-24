package com.godofthings.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;


/**
 * 神之系列的按键绑定（客户端）。
 * <p>
 * 原「神之工具」自创的那些按键（精准采集/时运/连锁/强制挖掘/模式轮盘等）已随该实现一并退役；
 * 照抄进来的 {@code com.godofthings.beef.core.common.KeyBindings} 自行注册属于它自己的按键。
 * 这里放的是本模组自己的功能键（传送点 / 套装 / 便签 / 手册等）。
 */
public class WandKeyBindings
{
    private static final String CATEGORY = "key.category.godofthings.wand";

    /** 打开传送点界面（默认不绑定，到「按键控制」里自己设） */
    public static final Lazy<KeyMapping> OPEN_WAYPOINT_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.open_waypoint",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    ));

    /** 神之套装技能树界面（默认不绑定）：原 O 键「套装功能开关」已按用户要求删除，只剩这一个入口 */
    public static final Lazy<KeyMapping> OPEN_ARMOR_SKILL_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.open_armor_skill",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    ));

    /** 神之便签记事本界面（默认不绑定） */
    public static final Lazy<KeyMapping> OPEN_NOTE_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.open_note",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    ));

    /** 直接进「摆放悬浮窗」编辑模式（默认不绑定）：不用先开记事本 */
    public static final Lazy<KeyMapping> EDIT_NOTE_HUD_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.edit_note_hud",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    ));

    /** 打开神之手册（默认不绑定） */
    public static final Lazy<KeyMapping> OPEN_MANUAL_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.open_manual",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    ));

    /** 打开神之背包（默认不绑定）：背包在身上任何地方（饰品栏背部槽 / 主物品栏 / 副手）都能开 */
    public static final Lazy<KeyMapping> OPEN_BACKPACK_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.open_backpack",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    ));

    /** 高级内存卡切换模式（默认不绑定；上游原本是 V 键独立分类，已并入本分类） */
    public static final Lazy<KeyMapping> AMC_MODE_SWITCH_KEY = Lazy.of(() -> new KeyMapping(
            "key.godofthings.amc_mode_switch",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    ));

    private WandKeyBindings() {}

    public static void register(RegisterKeyMappingsEvent event)
    {
        event.register(OPEN_WAYPOINT_KEY.get());
        event.register(OPEN_ARMOR_SKILL_KEY.get());
        event.register(OPEN_NOTE_KEY.get());
        event.register(EDIT_NOTE_HUD_KEY.get());
        event.register(OPEN_MANUAL_KEY.get());
        event.register(OPEN_BACKPACK_KEY.get());
        event.register(AMC_MODE_SWITCH_KEY.get());
    }
}
