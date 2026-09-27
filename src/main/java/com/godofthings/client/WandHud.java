package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.handler.WandFeatureHandler;
import com.godofthings.item.GodFavorWandItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之工具的「荒辰移晷」能力面板。
 *
 * <p>移植自万象担架的 {@code WondrousStaffHud}：<b>手持神之工具时在右下角常驻显示</b>
 * 本工具的全部能力与当前加速倍率 —— 这是让"神之工具已经不是原来那个工具了"
 * 一眼可见的关键（此前所有能力都藏在"潜行 + 右键"里，玩家根本看不出来）。
 *
 * <p>布局沿用参考模组：距底部 30px、右缘 4px、行高 10、底色 0xAA000000。
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class WandHud
{
    private static final int PAD = 4;
    private static final int LINE = 10;
    private static final int BOTTOM = 30;
    private static final int BG = 0xAA000000;

    private WandHud() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null)
        {
            return;
        }
        ItemStack wand = heldWand(mc);
        if (wand.isEmpty())
        {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        int speed = WandFeatureHandler.getSpeed(wand);

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("hud.godofthings.wand.title").withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("hud.godofthings.wand.accel", speed).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("hud.godofthings.wand.loot").withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("hud.godofthings.wand.mob").withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("hud.godofthings.wand.farm").withStyle(ChatFormatting.GREEN));

        int width = 0;
        for (Component line : lines)
        {
            width = Math.max(width, mc.font.width(line));
        }
        int h = lines.size() * LINE + PAD * 2;
        int x = g.guiWidth() - width - PAD * 2 - 4;
        int y = g.guiHeight() - h - BOTTOM;
        g.fill(x, y, x + width + PAD * 2, y + h, BG);
        for (int i = 0; i < lines.size(); i++)
        {
            g.drawString(mc.font, lines.get(i), x + PAD, y + PAD + i * LINE, 0xFFFFFF);
        }
    }

    private static ItemStack heldWand(Minecraft mc)
    {
        ItemStack main = mc.player.getMainHandItem();
        if (main.getItem() instanceof GodFavorWandItem)
        {
            return main;
        }
        ItemStack off = mc.player.getOffhandItem();
        return off.getItem() instanceof GodFavorWandItem ? off : ItemStack.EMPTY;
    }
}