package com.godofthings.overloadedinterface;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import appeng.api.AECapabilities;

import com.godofthings.Godofthings;

/**
 * 过载接口的 AE2 能力注册。
 *
 * <p>两件事：</p>
 * <ul>
 *   <li>方块实体并网（IN_WORLD_GRID_NODE_HOST）——AE2 基类通常自己注册，
 *       这里显式补一次，确保 {@code GridHelper.getNodeHost} 能找到它；</li>
 *   <li><b>EJECT 输出仓</b>：把接口自身暴露成 ME_STORAGE，机器在相邻面 /
 *       无线连接面上输出时就能往里塞，{@link OverloadedInterfaceBlockEntity#getEjectStorage()}
 *       负责把塞进来的东西直接灌进 ME 网络。非 EJECT 模式返回 null，能力等于不存在。</li>
 * </ul>
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class OverloadedInterfaceCapabilities
{
    private OverloadedInterfaceCapabilities()
    {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event)
    {
        var type = OverloadedInterfaceRegistry.OVERLOADED_INTERFACE_BE.get();

        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type,
                (blockEntity, side) -> blockEntity);

        // EJECT 模式的输出仓：机器把产物塞进这个面，接口直接回 ME 网络
        event.registerBlockEntity(AECapabilities.ME_STORAGE, type,
                (blockEntity, side) -> blockEntity.getEjectStorage());
    }
}
