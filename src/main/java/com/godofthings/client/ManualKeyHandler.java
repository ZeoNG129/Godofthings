package com.godofthings.client;

import com.godofthings.Godofthings;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 神之手册的快捷键（默认 P）：按一下开手册，再按一下（界面开着时）关掉。
 *
 * <p>和便签一样用 {@code consumeClick} 检测按下沿；界面开着时也把队列排空，避免攒着下次一起触发。</p>
 */
// 游戏总线为默认值（Bus.GAME），省略 bus 属性
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class ManualKeyHandler
{
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        while (WandKeyBindings.OPEN_MANUAL_KEY.get().consumeClick())
        {
            if (mc.screen == null)
            {
                ManualOpener.open();
            }
        }
    }
}
