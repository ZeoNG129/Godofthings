package com.godofthings.wondrous.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

/**
 * 荒辰移晷之杖的客户端按键（照抄上游 StretcherKeyBindings，按用户要求并入工具键位分类）。
 * [本地改动] 上游分类是独立的 key.categories.useless_stretcher，这里改用本模组
 * 「众神之物：工具」分类（key.category.godofthings.wand）；上游的
 * 键名前缀从 useless_stretcher. 改为 godofthings.。
 */
public final class StretcherKeyBindings
{
    public static final String CATEGORY = "key.category.godofthings.wand_staff";

    /** 独立传送（普通：瞬移 / 潜行：旅行锚）；上游默认不绑定。 */
    public static final KeyMapping STANDALONE_TELEPORT = new KeyMapping(
            "key.godofthings.beef_standalone_teleport",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    /** 打开杖的加速配置界面。 */
    public static final KeyMapping WONDROUS_STAFF_MODE = new KeyMapping(
            "key.godofthings.wondrous_staff_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY);
    /** 时停。 */
    public static final KeyMapping WONDROUS_STAFF_TIME_STOP = new KeyMapping(
            "key.godofthings.wondrous_staff_time_stop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY);
    /** 远程收回紬紬（复用本模组玩偶的既有远程收回载荷）。 */
    public static final KeyMapping TWO_I_REMOTE_RECLAIM = new KeyMapping(
            "key.godofthings.two_i_remote_reclaim",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY);

    private StretcherKeyBindings()
    {
    }
}
