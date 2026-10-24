package com.godofthings.superfusion.client;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 超级融合卡的客户端接线（软依赖 Mekanism）。
 * <p>
 * 由 {@code ClientModEvents} 在 Mekanism 存在时调用；没装 Mekanism 时这些类根本不会被加载，
 * 因此这里可以放心直接引用 {@link SuperFusionClientHandler} 等类。
 * <p>
 * 注意：这些类都**没有** {@code @EventBusSubscriber} 注解——按本工程约定，
 * 客户端专用类一律靠「调用点被 Mekanism 判断挡着」隔离，避免专用服务端加载到客户端类。
 */
public final class SuperFusionClientSetup
{
    private SuperFusionClientSetup()
    {
    }

    /** 注册事件监听（在客户端初始化阶段调用一次） */
    public static void init()
    {
        NeoForge.EVENT_BUS.addListener(SuperFusionClientHandler::onClientTick);
        NeoForge.EVENT_BUS.addListener(SuperFusionClientHandler::onInteraction);
        NeoForge.EVENT_BUS.addListener(SuperFusionClientHandler::onMouseButton);
        NeoForge.EVENT_BUS.addListener(SuperFusionClientHandler::onLeftClickBlock);
        NeoForge.EVENT_BUS.addListener(SuperFusionRenderer::onRenderLevelStage);
    }

    /** 供外部判断「当前是否需要融合卡的客户端逻辑」（手上拿卡时才需要渲染） */
    public static boolean shouldRenderSelection()
    {
        return SuperFusionClientHandler.getFirstSelectionCorner() != null;
    }

    /** 渲染阶段常量（供外部引用，避免各处硬编码字符串） */
    public static RenderLevelStageEvent.Stage renderStage()
    {
        return RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
    }
}
