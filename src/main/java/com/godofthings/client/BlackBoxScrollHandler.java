package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.item.BlackBoxData;
import com.godofthings.network.BlackBoxMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * 神之黑盒滚轮切换：蹲下（Shift）时滚动鼠标滚轮，翻转黑盒开关。
 * 仅支持手持黑盒（主手或副手）；切换后显示提示，取消滚轮事件避免同时切换快捷栏槽位。
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class BlackBoxScrollHandler
{
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null)
        {
            return;
        }
        if (!mc.player.isShiftKeyDown() || event.getScrollDeltaY() == 0.0)
        {
            return;
        }
        // 仅支持手持黑盒（主手或副手），不再查物品栏
        ItemStack box = BlackBoxData.findHeldBox(mc.player);
        if (box.isEmpty())
        {
            return;
        }
        boolean enabled = !BlackBoxData.isEnabled(box);
        // 乐观更新本地黑盒开关（立即反馈），服务端经 C2S 收敛
        BlackBoxData.setEnabled(box, enabled);
        BlackBoxMessages.sendToggle();
        // 提示当前开关状态
        mc.player.displayClientMessage(
                Component.translatable(enabled ? "message.godofthings.black_box.on" : "message.godofthings.black_box.off"),
                true);
        // 吞掉滚轮，避免同时切换快捷栏槽位
        event.setCanceled(true);
    }
}
