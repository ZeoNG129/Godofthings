package com.godofthings.beef.stretcher.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Client key bindings added by Useless Stretcher. */
public final class StretcherKeyBindings {
    /**
     * 按键分类键。照抄自上游 {@code key.categories.useless_stretcher}，命名空间改写后就是现在这个字符串，
     * 但语言文件里原本还留着上游的显示名「万象担架」—— v5.1.4 已改成本模组自己的名字「神之物」。
     * {@code beef.core.common.KeyBindings}（造化杖的 15 个按键）也用这个分类，
     * 因此按键设置里只出现一个分组，不再冒出上游模组的名字。
     */
    public static final String CATEGORY = "key.categories.godofthings";

    /** Opens the staff acceleration configuration screen. */
    public static final KeyMapping WONDROUS_STAFF_MODE = new KeyMapping(
            "key.godofthings.wondrous_staff_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY);


    private StretcherKeyBindings() {
    }
}
