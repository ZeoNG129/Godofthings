package com.godofthings.client;

import com.godofthings.Godofthings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 真实血量数字显示（HUD 层）。
 * <p>
 * <b>移植自 Zifeng Skill Tree（子枫的百宝箱）的 {@code HealthNumberRenderer}</b>
 * （Copyright (c) 2026 zifeng, MIT License，见 README「第三方代码与许可」）。
 *
 * <h3>要点</h3>
 * <ul>
 *   <li><b>读原版真实值</b>（{@code getHealth()} / {@code getMaxHealth()}），
 *       因此自动兼容任何其他模组加的生命值</li>
 *   <li><b>完全独立于血条渲染</b>：注册为独立 GUI 层，不做任何血条 Mixin →
 *       与其他血条 mod（ColorfulHearts / OverloadedArmorBar 等）零冲突，数字不会被覆盖</li>
 *   <li>层级选在 {@link VanillaGuiLayers#EXPERIENCE_LEVEL} 之上：位于所有血条相关层之后，
 *       又远低于聊天栏，不会盖住聊天</li>
 *   <li>创造/旁观模式不显示（与原版"不画血条"一致）</li>
 *   <li><b>只在真实最大生命超过原版 20 点时显示</b>：普通玩家零干扰</li>
 * </ul>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArmorHealthHud
{
    private ArmorHealthHud() {}

    /** 心形条左侧留白（像素） */
    private static final int GAP = 3;

    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "health_number");

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, LAYER_ID,
                (guiGraphics, deltaTracker) -> render(guiGraphics));
    }

    private static void render(GuiGraphics gui)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null)
        {
            return;
        }
        if (mc.gameMode == null || !mc.gameMode.canHurtPlayer())
        {
            return; // 创造/旁观：原版不画血条，我们也不画数字
        }
        float realMax = ArmorHealthHelper.realMaxHealth(mc.player);
        if (!ArmorHealthHelper.shouldCompress(realMax))
        {
            return; // 未超过原版 20 点：完全走原版表现
        }

        // 与原版血条布局对齐：心形条左缘 x = 屏宽/2 - 91；生命行 y = 屏高 - 39
        int heartsLeft = gui.guiWidth() / 2 - 91;
        int healthRowY = gui.guiHeight() - 39;
        int absorptionRowY = healthRowY - 10; // 原版吸收行在生命行上方一行

        float realHealth = ArmorHealthHelper.realHealth(mc.player);
        float realAbsorption = ArmorHealthHelper.realAbsorption(mc.player);

        // ① 伤害吸收（上一行，仅在存在时显示）
        if (realAbsorption > 0.0f)
        {
            drawRightAligned(gui, mc,
                    Component.translatable("hud.godofthings.health.absorption", fmt(realAbsorption)).getString(),
                    heartsLeft - GAP, absorptionRowY, 0xFFFFD700);
        }
        // ② 普通生命（下一行：当前 / 上限）
        drawRightAligned(gui, mc,
                Component.translatable("hud.godofthings.health.value", fmt(realHealth), fmt(realMax)).getString(),
                heartsLeft - GAP, healthRowY, 0xFFFF5555);
    }

    /** 1 位小数（真实血量可能带小数，如 19.5；大数值自动只显示大单位） */
    private static String fmt(float v)
    {
        if (v >= 100000.0f)
        {
            return String.format("%.0f", v);
        }
        return v >= 1000.0f ? String.format("%.0f", v) : String.format("%.1f", v);
    }

    /** 右对齐绘制（文字右端对齐到 rightEdge），带阴影保证可读性 */
    private static void drawRightAligned(GuiGraphics gui, Minecraft mc, String text, int rightEdge, int y, int color)
    {
        gui.drawString(mc.font, text, rightEdge - mc.font.width(text), y, color, true);
    }
}
