package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.client.screen.GodArmorConfigScreen;
import com.godofthings.client.screen.GodArmorSkillScreen;
import com.godofthings.network.ArmorMessages;
import com.godofthings.network.ArmorSkillMessages;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 神之套装界面快捷键：
 * <ul>
 *   <li><b>O</b>：功能开关界面（12 项套装功能）</li>
 *   <li><b>K</b>：技能树界面（基础属性 / 特殊增幅 …）</li>
 * </ul>
 * 打开前先向服务端拉一次权威值，避免界面显示的是过期镜像。
 * 不要求穿齐全套也能打开（可以先配置，穿上后生效）。
 */
// 游戏总线为默认值（Bus.GAME），省略 bus 属性
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class ArmorKeyHandler
{
    private ArmorKeyHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null)
        {
            return;
        }
        // consumeClick 检测按下沿：每按一次触发一次
        while (WandKeyBindings.OPEN_ARMOR_CONFIG_KEY.get().consumeClick())
        {
            ArmorMessages.requestSync();
            mc.setScreen(new GodArmorConfigScreen());
        }
        while (WandKeyBindings.OPEN_ARMOR_SKILL_KEY.get().consumeClick())
        {
            ArmorSkillMessages.requestSync();
            mc.setScreen(new GodArmorSkillScreen());
        }
    }
}
