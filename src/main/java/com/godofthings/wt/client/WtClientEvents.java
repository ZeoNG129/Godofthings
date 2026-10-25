package com.godofthings.wt.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.godofthings.wt.core.WtUniversalTerminalItem;
import com.godofthings.wt.network.WtNetwork;

/**
 * 通用终端客户端事件（对应上游 AE2wtlibClient + AE2wtlibForge 的 clientTick /
 * MouseScrollingEvent，[本地改动] 循环键由本模组自己注册，默认不绑定，分类
 * 「众神之物：AE」）。
 */
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME, modid = com.godofthings.Godofthings.MODID)
public final class WtClientEvents
{
    private WtClientEvents()
    {
    }

    /** 循环切换通用终端当前终端（默认不绑定；Shift+滚轮也可以切换）。 */
    public static final KeyMapping CYCLE_UNIVERSAL_TERMINAL = new KeyMapping(
            "key.godofthings.cycle_universal_terminal",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.godofthings.ae");

    public static void clientTick()
    {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null)
            return;

        while (CYCLE_UNIVERSAL_TERMINAL.consumeClick())
        {
            PacketDistributor.sendToServer(new WtNetwork.CycleTerminal(false));
        }
    }

    public static void mouseScroll(InputEvent.MouseScrollingEvent event)
    {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.screen != null || !net.minecraft.client.gui.screens.Screen.hasShiftDown()
                || event.getScrollDeltaY() == 0)
            return;

        if (!(player.getMainHandItem().getItem() instanceof WtUniversalTerminalItem)
                && !(player.getOffhandItem().getItem() instanceof WtUniversalTerminalItem))
            return;

        PacketDistributor.sendToServer(new WtNetwork.CycleTerminal(event.getScrollDeltaY() < 0));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void handle(ClientTickEvent.Post event)
    {
        clientTick();
    }

    @SubscribeEvent
    public static void handle(InputEvent.MouseScrollingEvent event)
    {
        mouseScroll(event);
    }
}
