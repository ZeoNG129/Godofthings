package com.godofthings.elevator.client;

import com.godofthings.Godofthings;
import com.godofthings.elevator.ElevatorRegistry;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 电梯的客户端注册：设置界面 + 方块实体渲染器 + 箭头独立模型。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.neoforge.client.ClientRegistry}（MIT）。</p>
 *
 * <p><b>[本地改动]</b>：上游还注册了 {@code RegisterColorHandlersEvent.Block}（伪装方块的染色跟随）
 * 与 {@code ModelEvent.ModifyBakingResult}（包装 BakedModel）。本版本改用方块实体渲染器直接渲染
 * 伪装方块（{@code renderSingleBlock} 自己处理染色），因此不需要单独的 BlockColor 注册，也不包装模型。</p>
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

    /**
     * 把箭头模型注册成「独立模型」。
     *
     * <p>漏了这一步，{@code getModel(standalone("godofthings:arrow"))} 只会拿到缺失模型，
     * 箭头永远画不出来。</p>
     */
    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event)
    {
        event.register(ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "arrow")));
    }
}
