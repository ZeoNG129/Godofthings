package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.client.screen.GodArmorSkillScreen;
import com.godofthings.network.ArmorMessages;
import com.godofthings.network.ArmorSkillMessages;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 神之套装界面快捷键（技能树与套装功能已合并为同一个界面，用标签页切换）：
 * K 键打开配置界面（三页标签自选）。原 O 键「套装功能开关」独立入口已按用户要求删除，
 * 套装功能页仍在界面里，用 K 打开后切标签即可。
 * 打开前先向服务端拉一次权威值（技能表 + 功能位图），避免界面显示的是过期镜像。
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
        // K 键 → 神之套装界面（默认「神之增幅」页，其余两页界面里切）
        while (WandKeyBindings.OPEN_ARMOR_SKILL_KEY.get().consumeClick())
        {
            requestAll();
            mc.setScreen(new GodArmorSkillScreen(GodArmorSkillScreen.TAB_ULTIMATE));
        }
    }

    private static void requestAll()
    {
        ArmorMessages.requestSync();
        ArmorSkillMessages.requestSync();
    }
}
