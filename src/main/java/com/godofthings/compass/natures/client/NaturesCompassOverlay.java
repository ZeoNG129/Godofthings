package com.godofthings.compass.natures.client;

import com.godofthings.compass.natures.config.ConfigHandler;
import com.godofthings.compass.natures.items.NaturesCompassItem;
import com.godofthings.compass.natures.util.CompassState;
import com.godofthings.compass.natures.util.ItemUtils;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 手持自然罗盘时左上角（或右上角，看配置）显示的搜索状态 HUD
 * （移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>只画在「没开界面 / 开着聊天栏且配置允许」且「F3 调试屏没开」的时候。
 * 三种状态各一套内容：搜索中（半径）/ 已找到（坐标 + 距离）/ 未找到（半径 + 采样数）。</p>
 */
@OnlyIn(Dist.CLIENT)
public class NaturesCompassOverlay implements LayeredDraw.Layer
{
    private static final Minecraft MC = Minecraft.getInstance();

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker)
    {
        if (MC.player == null || MC.level == null || MC.options.hideGui || MC.getDebugOverlay().showDebugScreen())
        {
            return;
        }
        if (MC.screen != null && !(ConfigHandler.CLIENT.displayWithChatOpen.get() && MC.screen instanceof ChatScreen))
        {
            return;
        }
        Player player = MC.player;
        ItemStack stack = ItemUtils.getHeldNatureCompass(player);
        if (stack.isEmpty() || !(stack.getItem() instanceof NaturesCompassItem compass))
        {
            return;
        }

        CompassState state = compass.getState(stack);
        if (state == CompassState.SEARCHING)
        {
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.status"), 5, 5,
                    0xFFFFFF, 0);
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.searching"), 5, 5,
                    0xAAAAAA, 1);

            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.biome"), 5, 5,
                    0xFFFFFF, 3);
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics,
                    BiomeDisplay.getBiomeName(MC.level, compass.getBiomeKey(stack)), 5, 5, 0xAAAAAA, 4);

            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.radius"), 5, 5,
                    0xFFFFFF, 6);
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, String.valueOf(compass.getSearchRadius(stack)), 5, 5,
                    0xAAAAAA, 7);
        }
        else if (state == CompassState.FOUND)
        {
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.status"), 5, 5,
                    0xFFFFFF, 0);
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.found"), 5, 5,
                    0xAAAAAA, 1);

            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.biome"), 5, 5,
                    0xFFFFFF, 3);
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics,
                    BiomeDisplay.getBiomeName(MC.level, compass.getBiomeKey(stack)), 5, 5, 0xAAAAAA, 4);

            if (compass.shouldDisplayCoordinates(stack))
            {
                RenderUtils.drawConfiguredStringOnHUD(guiGraphics,
                        I18n.get("godofthings.natures.coordinates"), 5, 5, 0xFFFFFF, 6);
                RenderUtils.drawConfiguredStringOnHUD(guiGraphics,
                        compass.getFoundBiomeX(stack) + ", " + compass.getFoundBiomeZ(stack), 5, 5, 0xAAAAAA, 7);

                RenderUtils.drawConfiguredStringOnHUD(guiGraphics,
                        I18n.get("godofthings.natures.distance"), 5, 5, 0xFFFFFF, 9);
                RenderUtils.drawConfiguredStringOnHUD(guiGraphics,
                        String.valueOf(compass.getDistanceToBiome(player, stack)), 5, 5, 0xAAAAAA, 10);
            }
        }
        else if (state == CompassState.NOT_FOUND)
        {
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.status"), 5, 5,
                    0xFFFFFF, 0);
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.notFound"), 5, 5,
                    0xAAAAAA, 1);

            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.biome"), 5, 5,
                    0xFFFFFF, 3);
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics,
                    BiomeDisplay.getBiomeName(MC.level, compass.getBiomeKey(stack)), 5, 5, 0xAAAAAA, 4);

            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.radius"), 5, 5,
                    0xFFFFFF, 6);
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, String.valueOf(compass.getSearchRadius(stack)), 5, 5,
                    0xAAAAAA, 7);

            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, I18n.get("godofthings.natures.samples"), 5, 5,
                    0xFFFFFF, 9);
            RenderUtils.drawConfiguredStringOnHUD(guiGraphics, String.valueOf(compass.getSamples(stack)), 5, 5,
                    0xAAAAAA, 10);
        }
    }
}
