package com.godofthings.advancedmemorycard.client.key;

import com.godofthings.advancedmemorycard.item.AmcItems;
import com.godofthings.advancedmemorycard.network.ModeSwitchPacket;
import com.godofthings.advancedmemorycard.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyEvent {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (ModKey.MODE_SWITCH_KEY.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            ItemStack offhandItem = player.getOffhandItem();
            ItemStack mainHandItem = player.getMainHandItem();
            if (mainHandItem.getItem().equals(AmcItems.ADVANCED_MEMORY_CARD.get())){
                NetworkHandler.sendToServer(new ModeSwitchPacket(InteractionHand.MAIN_HAND));
            } else if (offhandItem.getItem().equals(AmcItems.ADVANCED_MEMORY_CARD.get())){
                NetworkHandler.sendToServer(new ModeSwitchPacket(InteractionHand.OFF_HAND));
            }
        }
    }
}
