package com.godofthings.elevator.client;

import com.godofthings.Godofthings;
import com.godofthings.elevator.ElevatorRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 电梯的客户端注册：设置界面 + 方块实体渲染器。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.neoforge.client.ClientRegistry}（MIT）。</p>
 *
 * <p><b>[本地改动]</b>：上游还注册了 {@code RegisterColorHandlersEvent.Block}（让伪装方块的染色
 * 跟随，如草方块在不同群系变色）与 {@code ModelEvent.ModifyBakingResult}（包装 BakedModel）。
 * 本版本改用方块实体渲染器直接渲染伪装方块，<b>染色由 {@code renderSingleBlock} 自己处理</b>，
 * 因此不再需要单独的 BlockColor 注册，也不包装模型。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ElevatorClientRegistry
{
    private ElevatorClientRegistry() {}

    /** 把电梯菜单接上设置界面（漏了这一步，右键会「打不开界面」）。 */
    @SubscribeEvent
    public static void onMenuScreens(RegisterMenuScreensEvent event)
    {
        event.register(ElevatorRegistry.ELEVATOR_CONTAINER.get(), ElevatorScreen::new);
    }

    /** 注册方块实体渲染器（画伪装方块与朝向箭头）。 */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(ElevatorRegistry.ELEVATOR_BLOCK_ENTITY.get(), ElevatorRenderer::new);
    }
}
