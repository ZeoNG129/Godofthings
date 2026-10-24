package com.godofthings.superfusion;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 超级融合卡的注册入口（软依赖 Mekanism）。
 * <p>
 * 整个 superfusion 包只在 Mekanism 存在时才会被 {@code Godofthings} 触达：
 * 物品注册走 {@code SuperFusionRegistry.SUPER_FUSION_ITEM}，而该字段只在
 * {@code Godofthings} 构造里判断过 {@code ModList.isLoaded("mekanism")} 之后才会被求值。
 * <p>
 * 本类同时负责三件事：
 * <ul>
 *   <li>网络载荷注册（融合卡动作 / 中键 / 框选）；</li>
 *   <li>物品能量能力注册；</li>
 *   <li>让 {@code Godofthings} 用一个稳定入口判断「这个子系统要不要启用」。</li>
 * </ul>
 */
@EventBusSubscriber(modid = com.godofthings.Godofthings.MODID)
public final class SuperFusionEvents
{
    private SuperFusionEvents()
    {
    }

    /** Mekanism 是否在场（所有 superfusion 相关类只应在这个判断为 true 时才被引用） */
    public static boolean isMekanismLoaded()
    {
        return net.neoforged.fml.ModList.get().isLoaded("mekanism");
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event)
    {
        if (!isMekanismLoaded())
        {
            return;
        }
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(com.godofthings.superfusion.network.FusionActionPayload.TYPE,
                com.godofthings.superfusion.network.FusionActionPayload.CODEC,
                com.godofthings.superfusion.network.FusionActionPayload::handle);
        registrar.playToServer(com.godofthings.superfusion.network.MiddleClickPayload.TYPE,
                com.godofthings.superfusion.network.MiddleClickPayload.CODEC,
                com.godofthings.superfusion.network.MiddleClickPayload::handle);
        registrar.playToServer(com.godofthings.superfusion.network.BatchSelectionPayload.TYPE,
                com.godofthings.superfusion.network.BatchSelectionPayload.CODEC,
                com.godofthings.superfusion.network.BatchSelectionPayload::handle);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event)
    {
        if (!isMekanismLoaded())
        {
            return;
        }
        SuperFusionCapabilities.register(event);
    }
}
