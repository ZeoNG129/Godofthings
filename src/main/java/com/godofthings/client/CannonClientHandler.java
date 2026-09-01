package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.item.GodCannonItem;
import com.godofthings.network.CannonMessages;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 神之炮客户端处理：长按左键（攻击键）持续发射激光。
 * 每 tick 检测攻击键是否按住，按住且手持神之炮时按节流向服务端发发射请求。
 */
// 游戏总线为默认值（Bus.GAME），省略 bus 属性
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class CannonClientHandler
{
    /** 发射节流（tick）：每 N tick 向服务端发一次发射请求 */
    private static final int BEAM_INTERVAL_TICKS = 2;
    private static int beamCooldown = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null)
        {
            return;
        }

        if (beamCooldown > 0)
        {
            beamCooldown--;
        }

        if (mc.player.getMainHandItem().getItem() instanceof GodCannonItem
                && mc.options.keyAttack.isDown())
        {
            if (beamCooldown <= 0)
            {
                CannonMessages.sendBeam();
                beamCooldown = BEAM_INTERVAL_TICKS;
            }
        }
    }
}
