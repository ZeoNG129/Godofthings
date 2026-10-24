package com.godofthings.aalink.client;

import com.godofthings.Godofthings;
import com.godofthings.aalink.ConnectorCardItem;
import com.godofthings.aalink.ConnectorCardToggleAutoPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 无线连接器卡：Shift + 鼠标滚轮切换自动连接（照抄上游 FrequencyCardScrollHandler）。
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class ConnectorCardScrollHandler
{
    private ConnectorCardScrollHandler()
    {
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event)
    {
        if (event.getScrollDeltaY() == 0.0D || !Screen.hasShiftDown())
        {
            return;
        }

        var player = Minecraft.getInstance().player;
        if (player == null)
        {
            return;
        }

        InteractionHand hand = null;
        if (player.getMainHandItem().getItem() instanceof ConnectorCardItem)
        {
            hand = InteractionHand.MAIN_HAND;
        }
        else if (player.getOffhandItem().getItem() instanceof ConnectorCardItem)
        {
            hand = InteractionHand.OFF_HAND;
        }

        if (hand == null)
        {
            return;
        }

        PacketDistributor.sendToServer(ConnectorCardToggleAutoPayload.forHand(hand));
        event.setCanceled(true);
    }
}
