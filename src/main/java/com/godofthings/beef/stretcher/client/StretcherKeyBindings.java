package com.godofthings.beef.stretcher.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Client key bindings added by Useless Stretcher. */
public final class StretcherKeyBindings {
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
