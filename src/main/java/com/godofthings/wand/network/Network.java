package com.godofthings.wand.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.util.List;

/**
 * 垫片：替代万象担架的 {@code com.sorrowmist.useless.stretcher.network.Network}。
 *
 * <p>照抄进来的 75 个文件里调用了一批 {@code Network.xxx()} —— 那些是原 mod 自己的
 * 网络同步（把状态发给客户端界面）。这里按<b>原方法签名</b>原样留出，方法体为空，
 * 因此抄来的业务代码<b>一个字都不用改</b>就能编译；等对应的界面接好后，
 * 再把空实现换成我们自己的 {@code WandMessages} 同步即可。
 */
public final class Network
{
    private Network() {}

    public static void sendStaffTutorial(ServerPlayer player) {}

    public static void sendStaffTutorialOpened() {}

    public static void sendTimeAccelerationState(ServerLevel level, int speed) {}

    public static void sendWondrousStaffSpeed(int speed, int mode, boolean accelerationEnabled, Object... rest) {}

    public static void sendWondrousStaffFeatures(boolean summonEnabled, boolean lootRefresh, Object... rest) {}

    public static void sendWondrousStaffSummon(List<String> entityIds, InteractionHand hand) {}
}