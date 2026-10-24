package com.godofthings.advancedmemorycard.client.key;

import com.godofthings.Godofthings;
import com.godofthings.advancedmemorycard.item.AmcItems;
import com.godofthings.advancedmemorycard.network.ModeSwitchPacket;
import com.godofthings.advancedmemorycard.network.NetworkHandler;
import com.godofthings.client.WandKeyBindings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 高级内存卡的「切换模式」按键处理。
 *
 * <p>上游是 V 键 + 独立按键分类（{@code ModKey} 自注册）；并入后按键统一挂在
 * {@link WandKeyBindings#AMC_MODE_SWITCH_KEY}（「众神之物」分类、默认不绑定），
 * 本类只保留按下后的处理逻辑：主手或副手持有高级内存卡时向服务端发模式切换包。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class KeyEvent {

    private KeyEvent() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (WandKeyBindings.AMC_MODE_SWITCH_KEY.get().consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                continue;
            }
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offhandItem = player.getOffhandItem();
            if (mainHandItem.getItem().equals(AmcItems.ADVANCED_MEMORY_CARD.get())) {
                NetworkHandler.sendToServer(new ModeSwitchPacket(InteractionHand.MAIN_HAND));
            } else if (offhandItem.getItem().equals(AmcItems.ADVANCED_MEMORY_CARD.get())) {
                NetworkHandler.sendToServer(new ModeSwitchPacket(InteractionHand.OFF_HAND));
            }
        }
    }
}
