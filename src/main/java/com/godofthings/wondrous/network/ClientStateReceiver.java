package com.godofthings.wondrous.network;

import com.godofthings.wondrous.client.RangeAccelerationHistoryScreen;
import com.godofthings.wondrous.client.ReclaimerScreen;
import com.godofthings.wondrous.client.WondrousStaffCloudTime;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 服务端下行状态转发到客户端（照抄上游 ClientStateReceiver，只留杖本体功能段）。
 * [本地改动] 剔除 Myriad / 胶囊 / TwoI 对话与故事 / 流体收集等未移植功能的 handler；
 * 相位碰撞沿用本地 StaffPhaseManager.clientTick。
 */
public final class ClientStateReceiver
{
    private static boolean staffPhaseEnabled;
    private static boolean staffPhaseInteraction;

    private ClientStateReceiver()
    {
    }

    public static void handleStaffTutorial()
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gui.setTitle(Component.translatable("msg.godofthings.staff_ui_hint")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        minecraft.gui.setSubtitle(Component.translatable("msg.godofthings.staff_ui_hint_subtitle")
                .withStyle(ChatFormatting.GOLD));
        minecraft.gui.setTimes(15, 80, 20);
    }

    public static void handleTimeAcceleration(WondrousNetwork.TimeAccelerationStatePayload payload)
    {
        WondrousStaffCloudTime.accept(payload.dimension(), payload.speed());
    }

    public static void handleStaffPhase(WondrousNetwork.StaffPhaseStatePayload payload)
    {
        staffPhaseEnabled = payload.phaseEnabled();
        staffPhaseInteraction = payload.interactionEnabled();
        applyStaffPhase();
    }

    /** 重生 / 换维度后重新落实本地碰撞标志。 */
    public static void tickStaffPhase()
    {
        if (Minecraft.getInstance().level == null)
        {
            staffPhaseEnabled = false;
            staffPhaseInteraction = true;
            com.godofthings.wondrous.content.entity.StaffPhaseManager.clientTick(null);
            return;
        }
        applyStaffPhase();
    }

    public static boolean staffPhaseInteractionEnabled()
    {
        return staffPhaseInteraction;
    }

    private static void applyStaffPhase()
    {
        // 移动处理独占整个原版 tick 的碰撞/飞行；这里二次套用快照会把
        // 自己刚置真位否定回来，所以只转发给 manager。
        var player = Minecraft.getInstance().player;
        com.godofthings.wondrous.content.entity.StaffPhaseManager.clientTick(player);
    }

    public static void handleTimeStop(WondrousNetwork.TimeStopStatePayload payload)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.remaining() <= 0)
        {
            minecraft.gui.setOverlayMessage(Component.translatable("msg.godofthings.time_stop.end"), false);
        }
        else
        {
            int seconds = (payload.remaining() + 19) / 20;
            minecraft.gui.setOverlayMessage(Component.translatable("msg.godofthings.time_stop.countdown", seconds), false);
        }
    }

    public static void handleRangeHistory(RangeNetwork.HistoryStatePayload payload)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof RangeAccelerationHistoryScreen screen)
        {
            screen.onHistory(payload.fields());
        }
    }

    public static void handleReclaimer(WondrousNetwork.ReclaimerStatePayload payload)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.personal())
        {
            if (minecraft.screen instanceof com.godofthings.wondrous.client.StaffScreen staff
                    && staff.cachedTab(com.godofthings.wondrous.client.StaffScreen.Tab.MACHINES)
                            instanceof ReclaimerScreen history)
                history.update(payload.entries());
            return;
        }
        if (minecraft.screen instanceof ReclaimerScreen screen && screen.personal() == payload.personal())
        {
            screen.update(payload.entries());
        }
        else
        {
            minecraft.setScreen(new ReclaimerScreen(payload.personal() ? minecraft.screen : null,
                    payload.entries(), payload.personal()));
        }
    }
}
