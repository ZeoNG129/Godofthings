package com.godofthings.omni.client;

import com.godofthings.omni.GodofthingsOmni;
import com.godofthings.omni.OmniConfig;
import com.godofthings.omni.OmniNetwork;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

/**
 * 三个全能升级开关的按键处理（原样移植自 Omni Upgrades 的 {@code KeyToggleHandler}）：
 * Shift+O 总开关（同步服务端）、Shift+N 顺序填满、Shift+M 对角检测。
 */
@EventBusSubscriber(modid = GodofthingsOmni.MODID, value = Dist.CLIENT)
public class OmniKeyToggleHandler {

    @SubscribeEvent
    public static void onClientTick(Post event) {
        while (OmniKeyMappings.TOGGLE_ENABLED.consumeClick()) {
            toggle(OmniConfig.ENABLED, v -> OmniConfig.isEnabled = v, "message.godofthings.omni.enabled");
            OmniNetwork.sendToServer(OmniConfig.isEnabled);
        }
        while (OmniKeyMappings.TOGGLE_SEQUENTIAL_FILL.consumeClick()) {
            toggle(OmniConfig.SEQUENTIAL_FILL_MODE, v -> OmniConfig.isSequentialFillEnabled = v,
                    "message.godofthings.omni.sequential_fill");
        }
        while (OmniKeyMappings.TOGGLE_DIAGONAL_DETECTION.consumeClick()) {
            toggle(OmniConfig.DIAGONAL_DETECTION_MODE, v -> OmniConfig.isDiagonalDetectionEnabled = v,
                    "message.godofthings.omni.diagonal_detection");
        }
    }

    @SubscribeEvent
    public static void onLoggingIn(LoggingIn event) {
        OmniNetwork.sendToServer(OmniConfig.isEnabled);
        OmniNetwork.sendBlacklistToServer(OmniConfig.getBlacklistEntries());
    }

    private static void toggle(BooleanValue configValue, Consumer<Boolean> applyStatic, String messageBase) {
        boolean newState = !configValue.get();
        configValue.set(newState);
        applyStatic.accept(newState);
        if (OmniConfig.SPEC.isLoaded()) {
            OmniConfig.SPEC.save();
        }
        Component msg = Component.translatable(messageBase + (newState ? ".enabled" : ".disabled"))
                .withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(msg, true);
        }
    }
}
