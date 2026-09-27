package com.godofthings.beef.stretcher.network;

import com.godofthings.beef.stretcher.client.WondrousStaffCloudTime;
import com.godofthings.beef.stretcher.client.RangeAccelerationHistoryScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 把服务端推送的状态转交给客户端界面。
 *
 * <p>逐字照抄 UselessStretcher 扩展模组的 {@code network.ClientStateReceiver}；
 * 只裁掉万象模具相关的两项：{@code accept(MyriadStatePayload)}（转发给
 * {@code OmniversalMyriadScreen}）与 {@code handleFullSlots}（模具槽位已满提示），
 * 它们属于本模组未移植的模具子系统。</p>
 */
public final class ClientStateReceiver {
    private ClientStateReceiver() {
    }

    public static void handleStaffTutorial() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setTitle(Component.translatable("msg.godofthings.staff_ui_hint")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        minecraft.gui.setSubtitle(Component.translatable("msg.godofthings.staff_ui_hint_subtitle")
                .withStyle(ChatFormatting.GOLD));
        minecraft.gui.setTimes(15, 80, 20);
    }

    public static void handleTimeAcceleration(Network.TimeAccelerationStatePayload payload) {
        WondrousStaffCloudTime.accept(payload.dimension(), payload.speed());
    }

    public static void handleRangeHistory(RangeNetwork.HistoryStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof RangeAccelerationHistoryScreen screen) {
            screen.onHistory(payload.fields());
        }
    }
}
