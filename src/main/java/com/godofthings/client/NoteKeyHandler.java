package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.network.GodNoteMessages;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 神之便签的客户端每帧处理：
 * <ul>
 *   <li>进世界后主动拉一次便签数据 —— 悬浮窗要显示，进服不拉就是空的</li>
 *   <li>快捷键（默认 N）请求打开记事本</li>
 * </ul>
 */
// 游戏总线为默认值（Bus.GAME），省略 bus 属性
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class NoteKeyHandler
{
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
        {
            // 离开世界：下次进来重新拉一次
            ClientNoteCache.resetRequest();
            return;
        }
        ClientNoteCache.requestOnce();

        // consumeClick 检测按下沿：每按一次触发一次；界面开着时也要排空队列，否则会攒着
        while (WandKeyBindings.OPEN_NOTE_KEY.get().consumeClick())
        {
            if (mc.screen == null)
            {
                GodNoteMessages.sendOpen();
            }
        }
    }
}
