package com.godofthings.handler;

import com.godofthings.Godofthings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * 神之护甲客户端效果：
 * - 夜视：改伽马值（内置亮度，无药水效果、无闪烁）
 * - 熔岩/火焰：去掉橙色屏幕反馈（雾变透明、颜色中性），像在水里一样无反馈
 */
@Mod.EventBusSubscriber(modid = Godofthings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GodArmorClientHandler
{
    private static Double savedGamma = null;

    // 客户端每 tick：穿全套时把亮度设为最大（伽马 1.0），脱下恢复原值
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.side != LogicalSide.CLIENT)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.player != mc.player)
        {
            return;
        }
        boolean worn = GodArmorHandler.isFullSetWorn(mc.player);
        OptionInstance<Double> gamma = mc.options.gamma();
        if (worn)
        {
            if (savedGamma == null)
            {
                savedGamma = gamma.get();
            }
            if (gamma.get() != 1.0D)
            {
                gamma.set(1.0D); // 内置夜视：最大亮度，无闪烁
            }
        }
        else if (savedGamma != null)
        {
            gamma.set(savedGamma);
            savedGamma = null;
        }
    }

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
