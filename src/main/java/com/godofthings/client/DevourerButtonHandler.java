package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.network.DevourerMessages;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * 在背包（按 E）界面左上角添加「神之吞噬」按钮，点击打开便携吞噬界面。
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class DevourerButtonHandler
{
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event)
    {
        if (event.getScreen() instanceof InventoryScreen inv)
        {
            // 按钮放在背包界面内部左上角（避免 GUI 缩放较大时 getGuiLeft()-44 出屏不可见）
            int x = inv.getGuiLeft() + 2;
            int y = inv.getGuiTop() + 2;
            Button button = Button.builder(
                            Component.literal("吞"),
                            b -> DevourerMessages.sendOpen())
                    .bounds(x, y, 20, 20)
                    .tooltip(Tooltip.create(Component.translatable("block.godofthings.god_devourer")))
                    .build();
            event.addListener(button);
        }
    }
}
