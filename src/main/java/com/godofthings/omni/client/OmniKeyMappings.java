package com.godofthings.omni.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;

import net.minecraft.client.KeyMapping;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * 全能升级的三个按键（原样移植自 Omni Upgrades 的 {@code OmniKeyMappings}）。
 * 按主人要求改为默认不绑定（上游默认 Shift+O / Shift+N / Shift+M）。
 */
@EventBusSubscriber(modid = "godofthings", value = Dist.CLIENT)
public class OmniKeyMappings {

    public static final KeyMapping TOGGLE_ENABLED = new KeyMapping(
            "key.godofthings.omni.toggle_enabled", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.godofthings.omni");
    public static final KeyMapping TOGGLE_SEQUENTIAL_FILL = new KeyMapping(
            "key.godofthings.omni.toggle_sequential_fill", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.godofthings.omni");
    public static final KeyMapping TOGGLE_DIAGONAL_DETECTION = new KeyMapping(
            "key.godofthings.omni.toggle_diagonal_detection", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.godofthings.omni");

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_ENABLED);
        event.register(TOGGLE_SEQUENTIAL_FILL);
        event.register(TOGGLE_DIAGONAL_DETECTION);
    }
}
