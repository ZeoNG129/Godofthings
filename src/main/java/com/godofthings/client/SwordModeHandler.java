package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.item.GodSwordItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * 神之剑按键处理：J 键打开神之剑功能切换面板。
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class SwordModeHandler
{
    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
        {
            return;
        }

        if (event.getKey() == WandKeyBindings.SWORD_MODE_KEY.get().getKey().getValue()
                && event.getAction() == InputConstants.PRESS)
        {
            ItemStack sword = findSword(minecraft.player.getMainHandItem(), minecraft.player.getOffhandItem());
            if (sword != null && minecraft.screen == null)
            {
                SwordModeScreen.show(sword);
            }
        }
    }

    private static ItemStack findSword(ItemStack main, ItemStack off)
    {
        if (main.getItem() instanceof GodSwordItem)
        {
            return main;
        }
        if (off.getItem() instanceof GodSwordItem)
        {
            return off;
        }
        return null;
    }
}
