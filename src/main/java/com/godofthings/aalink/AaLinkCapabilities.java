package com.godofthings.aalink;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import appeng.api.AECapabilities;

import com.godofthings.Godofthings;

/**
 * 无线连接器的 AE2 能力注册：把方块实体登记为 in-world 网格节点宿主，
 * 否则 {@code GridHelper.getNodeHost} 找不到它，放置后无法与相邻线缆自动建连。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class AaLinkCapabilities
{
    private AaLinkCapabilities()
    {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                AaLinkRegistry.WIRELESS_CONNECTOR_BE.get(), (blockEntity, side) -> blockEntity);
    }
}
