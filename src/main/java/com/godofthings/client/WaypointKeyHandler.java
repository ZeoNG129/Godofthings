package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.network.WaypointMessages;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * U 键打开传送点界面（快捷键）。
 */
// 游戏总线为默认值（Bus.GAME），省略 bus 属性
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class WaypointKeyHandler
{
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null)
        {
            return;
        }
        // consumeClick 检测按下沿：每按一次触发一次，不重复
        while (WandKeyBindings.OPEN_WAYPOINT_KEY.get().consumeClick())
        {
            WaypointMessages.sendOpen();
        }
    }
}
