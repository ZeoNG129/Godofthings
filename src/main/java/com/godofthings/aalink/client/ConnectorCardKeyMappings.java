package com.godofthings.aalink.client;

import com.godofthings.aalink.ConnectorCardItem;
import com.godofthings.aalink.ConnectorCardLinkShortcutPayload;
import com.godofthings.aalink.ConnectorCardToggleAutoPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 无线连接器卡的两枚按键（照抄上游 FrequencyCardKeyMappings；默认不绑定）：
 * 切换自动连接 / 连接注视目标。
 */
@EventBusSubscriber(modid = com.godofthings.Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ConnectorCardKeyMappings
{
    private static final String CATEGORY = "key.category.godofthings.wand";

    private static final KeyMapping TOGGLE_AUTO_CONNECT = new KeyMapping(
            "key.godofthings.aalink.card_toggle_auto",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    private static final KeyMapping TOGGLE_LINK = new KeyMapping(
            "key.godofthings.aalink.card_link",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    private ConnectorCardKeyMappings()
    {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event)
    {
        event.register(TOGGLE_AUTO_CONNECT);
        event.register(TOGGLE_LINK);
    }

    @EventBusSubscriber(modid = com.godofthings.Godofthings.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static final class RuntimeHandler
    {
        private RuntimeHandler()
        {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event)
        {
            while (TOGGLE_AUTO_CONNECT.consumeClick())
            {
                PacketDistributor.sendToServer(new ConnectorCardToggleAutoPayload());
            }
            while (TOGGLE_LINK.consumeClick())
            {
                var minecraft = Minecraft.getInstance();
                if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                        || !(minecraft.hitResult instanceof BlockHitResult hit))
                {
                    continue;
                }
                PacketDistributor.sendToServer(new ConnectorCardLinkShortcutPayload(
                        hit.getBlockPos(), hit.getDirection(), hit.getLocation()));
            }
        }
    }
}
