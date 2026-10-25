package com.godofthings.ae.wt;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

/**
 * 无耗能通用终端的客户端按键（分类「众神之物：AE」）。
 *
 * <p>语义照抄上游：按下 = {@code AE2wtlibAPI.cycleTerminal(潜行?)}——发送上游自己的
 * CycleTerminalPacket，服务端在「菜单打开中」与「手持」两种场景下都会切换当前终端。
 * （ae2wtlib 自带的 Shift+滚轮也走同一个包，本模组 WUT 同样命中。）默认不绑定。</p>
 */
public final class AeKeyBindings
{
    public static final String CATEGORY = "key.category.godofthings.ae";

    /** 循环切换通用终端的当前终端（潜行 = 反向）。 */
    public static final KeyMapping CYCLE_UNIVERSAL_TERMINAL = new KeyMapping(
            "key.godofthings.cycle_universal_terminal",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY);

    private AeKeyBindings()
    {
    }
}
