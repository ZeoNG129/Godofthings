package com.godofthings.ae.wt;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import de.mari_023.ae2wtlib.api.AE2wtlibAPI;

/**
 * 无耗能通用终端的客户端按键处理（[本地改动] 本模组按键只负责把语义转发给上游：
 * {@code AE2wtlibAPI.cycleTerminal(reverse)} 发送上游自己的 CycleTerminalPacket——
 * 服务端对「菜单打开中」与「手持两种场景都有处理（WUTHandler.cycle），
 * 且只认 ItemWUT 子类，本模组无耗能 WUT 原生命中）。
 */
@EventBusSubscriber(modid = com.godofthings.Godofthings.MODID, value = Dist.CLIENT)
public final class AeClientHandler
{
    private AeClientHandler()
    {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        var mc = net.minecraft.client.Minecraft.getInstance();
        var player = mc.player;
        if (player == null)
        {
            return;
        }
        if (!AeKeyBindings.CYCLE_UNIVERSAL_TERMINAL.consumeClick())
        {
            return;
        }
        // 潜行 = 反向切换（与上游 Shift+滚轮同语义）；其余全部交给上游包处理
        AE2wtlibAPI.cycleTerminal(player.isShiftKeyDown());
    }
}
