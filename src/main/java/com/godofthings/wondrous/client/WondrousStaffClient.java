package com.godofthings.wondrous.client;

import com.godofthings.Godofthings;
import com.godofthings.doll.network.TwoIDollRemoteReclaimPayload;
import com.godofthings.wondrous.content.entity.WondrousStaffAcceleration;
import com.godofthings.wondrous.init.ModItems;
import com.godofthings.wondrous.init.StaffComponents;
import com.godofthings.wondrous.network.WondrousNetwork;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 荒辰移晷之杖的客户端操控（照抄上游 WondrousStaffClient）：
 * <ul>
 *   <li>Shift + 滚轮循环倍率挡位（关 / x2 / x4 / x16 / x32 / x64 / x128 / x256 / x512 / x1024）。</li>
 *   <li>X 键打开按钮式的速度与时长配置界面。</li>
 *   <li>P 键时停；B 键远程收回紬紬。</li>
 * </ul>
 * Shift 只与滚轮和右键组合使用，单独按住不捕获。
 *
 * <p>[本地改动] 上游分类是独立分类，这里按键注册进「众神之物：工具」分类；
 * 上游 STANDALONE_TELEPORT 不移植；B 键收回紬紬复用本模组玩偶已有的
 * {@link TwoIDollRemoteReclaimPayload}（不新建网络载荷）。
 * 挡位预设沿用 JDT Extras 的 {@code TimeMultitoolSpeedMode}（MIT）思路。
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class WondrousStaffClient
{
    /** 预设倍率挡位。0 = 关。 */
    private static final int[] GEARS = {0, 2, 4, 16, 32, 64, 128, 256, 512, 1024};

    private WondrousStaffClient()
    {
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !player.isShiftKeyDown()) return;
        InteractionHand hand = findStaffHand(player);
        if (hand == null) return;
        ItemStack held = player.getItemInHand(hand);

        double delta = event.getScrollDeltaY();
        if (delta == 0) return;
        boolean up = delta > 0;

        int speed = cycleGear(WondrousStaffAcceleration.getSpeed(held), up);
        held.set(StaffComponents.WONDROUS_STAFF_SPEED.get(), speed);
        WondrousNetwork.sendWondrousStaffSpeed(speed, WondrousStaffAcceleration.getMode(held),
                WondrousStaffAcceleration.isEnabled(held), hand);
        showGearStatus(speed);
        // 否则该事件会继续走原版的快捷栏滚动。
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;
        if (StretcherKeyBindings.STANDALONE_TELEPORT.consumeClick())
        {
            WondrousNetwork.sendStandaloneTeleport();
        }
        if (StretcherKeyBindings.TWO_I_REMOTE_RECLAIM.consumeClick())
        {
            // [本地改动] 复用本模组玩偶的远程收回载荷（服务端已有完整实现）。
            PacketDistributor.sendToServer(new TwoIDollRemoteReclaimPayload());
        }
        if (StretcherKeyBindings.WONDROUS_STAFF_TIME_STOP.consumeClick())
        {
            InteractionHand stopHand = findStaffHand(player);
            if (stopHand != null)
            {
                WondrousNetwork.sendStaffTimeStop(stopHand);
                return;
            }
        }
        if (!StretcherKeyBindings.WONDROUS_STAFF_MODE.consumeClick()) return;

        InteractionHand hand = findStaffHand(player);
        if (hand != null)
        {
            WondrousNetwork.sendStaffTutorialOpened();
            StaffScreen.open(hand);
        }
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onKeyInput(InputEvent.Key event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (event.getAction() != org.lwjgl.glfw.GLFW.GLFW_PRESS || mc.screen != null) return;
        if (event.getKey() != org.lwjgl.glfw.GLFW.GLFW_KEY_X) return;
        // 裸 X 兜底只为默认 X 绑定提供冲突优先路径；玩家改绑后 X 必须让给新主人。
        if (StretcherKeyBindings.WONDROUS_STAFF_MODE.getKey().getValue()
                != org.lwjgl.glfw.GLFW.GLFW_KEY_X) return;
        LocalPlayer player = mc.player;
        InteractionHand hand = player == null ? null : findStaffHand(player);
        if (hand == null) return;
        StretcherKeyBindings.WONDROUS_STAFF_MODE.consumeClick();
        WondrousNetwork.sendStaffTutorialOpened();
        StaffScreen.open(hand);
    }

    private static int cycleGear(int current, boolean up)
    {
        int idx = indexOf(current);
        int next = up ? (idx + 1) % GEARS.length : (idx - 1 + GEARS.length) % GEARS.length;
        return GEARS[next];
    }

    private static int indexOf(int speed)
    {
        if (speed <= 0) return 0;
        for (int i = 1; i < GEARS.length; i++)
        {
            if (speed <= GEARS[i]) return i;
        }
        return GEARS.length - 1;
    }

    private static void showGearStatus(int speed)
    {
        Minecraft mc = Minecraft.getInstance();
        Component text = speed <= 0
                ? Component.translatable("gui.godofthings.speed_off").withStyle(ChatFormatting.GRAY)
                : Component.literal("x" + speed).withStyle(ChatFormatting.AQUA);
        mc.gui.setOverlayMessage(text, false);
    }

    private static InteractionHand findStaffHand(LocalPlayer player)
    {
        if (player.getMainHandItem().is(ModItems.WONDROUS_STAFF.get())) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().is(ModItems.WONDROUS_STAFF.get())) return InteractionHand.OFF_HAND;
        return null;
    }
}
