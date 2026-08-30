package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.item.GodFavorWandItem;
import com.godofthings.modes.ModeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 模式轮盘处理器：G 键打开模式选择轮盘。
 */
@Mod.EventBusSubscriber(modid = Godofthings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ModeWheelHandler
{
    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
        {
            return;
        }

        if (event.getKey() == WandKeyBindings.SWITCH_MODE_WHEEL_KEY.get().getKey().getValue() && event.getAction() == 1)
        {
            ItemStack mainHandItem = minecraft.player.getMainHandItem();
            ItemStack offHandItem = minecraft.player.getOffhandItem();

            ItemStack targetItem = null;

            if (mainHandItem.getItem() instanceof GodFavorWandItem)
            {
                targetItem = mainHandItem;
            }
            else if (offHandItem.getItem() instanceof GodFavorWandItem)
            {
                targetItem = offHandItem;
            }

            if (targetItem != null && minecraft.screen == null)
            {
                ModeManager modeManager = new ModeManager();
                modeManager.loadFromStack(targetItem);
                ModeWheelScreen.show(modeManager, targetItem);
            }
        }
    }
}
