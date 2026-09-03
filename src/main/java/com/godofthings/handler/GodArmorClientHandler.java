package com.godofthings.handler;

import com.godofthings.Godofthings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * 神之护甲客户端效果（熔岩/火焰屏幕反馈去除）。
 * 夜视已改用服务端药水效果（GodArmorHandler 里 addEffect NIGHT_VISION，无痕参数），不再改伽马值。
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class GodArmorClientHandler
{
    // 熔岩中的橙色屏幕：改成中性的浅蓝色（像水）
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && GodArmorHandler.isFullSetWorn(mc.player) && mc.player.isInLava())
        {
            event.setRed(0.25F);
            event.setGreen(0.5F);
            event.setBlue(0.7F);
        }
    }

    // 熔岩中的浓雾：拉远雾距离，能看清（像水）
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && GodArmorHandler.isFullSetWorn(mc.player) && event.getType() == FogType.LAVA)
        {
            event.setFarPlaneDistance(1000.0F);
        }
    }
}
