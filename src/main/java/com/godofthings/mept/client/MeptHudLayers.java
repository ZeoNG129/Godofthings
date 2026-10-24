package com.godofthings.mept.client;

import com.godofthings.Godofthings;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * ME 放置工具的客户端 HUD（照抄上游 MEPlacementToolMod.ClientModEvents 的图层注册
 * 与 ClientForgeEvents 的准星隐藏）：快捷栏上方显示选中物品 / 放置数量文本，
 * 轮盘菜单打开时隐藏准星。
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MeptHudLayers
{
    private MeptHudLayers()
    {
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_overlay"),
                (graphics, partialTick) ->
                {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;

                    long now = System.currentTimeMillis();
                    int width = mc.getWindow().getGuiScaledWidth();
                    int baseY = mc.getWindow().getGuiScaledHeight() - 59; // 快捷栏上方

                    // 选中物品文本（上一行）
                    if (MeptClientOverlay.lastSelectedText != null &&
                            now - MeptClientOverlay.lastSelectedTime < 2000)
                    {
                        String text = MeptClientOverlay.lastSelectedText;
                        int x = (width - mc.font.width(text)) / 2;
                        graphics.drawString(mc.font, text, x, baseY - 12, 0xFFFFFF, true);
                    }

                    // 放置数量文本（下一行）
                    if (MeptClientOverlay.lastCountText != null &&
                            now - MeptClientOverlay.lastCountTime < 2000)
                    {
                        String text = MeptClientOverlay.lastCountText;
                        int x = (width - mc.font.width(text)) / 2;
                        graphics.drawString(mc.font, text, x, baseY, 0xFFFFFF, true);
                    }
                });
    }

    /** 轮盘菜单打开时隐藏准星（游戏总线事件）。 */
    @SubscribeEvent
    public static void onRenderCrosshair(RenderGuiLayerEvent.Pre event)
    {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR))
        {
            var screen = Minecraft.getInstance().screen;
            if (screen instanceof MeptRadialMenuScreen ||
                    screen instanceof MeptDualLayerRadialMenuScreen)
            {
                event.setCanceled(true);
            }
        }
    }
}
