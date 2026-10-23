package com.godofthings.doll.client;

import com.godofthings.Godofthings;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.render.TwoIDollCurioRenderer;
import com.godofthings.doll.render.TwoIDollDisplayRenderer;
import com.godofthings.doll.render.TwoIDollItemRenderer;
import com.godofthings.doll.render.TwoIDollRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * 玩偶的客户端接线 —— 对应源码 {@code ClientSetup} 里与玩偶有关的五段：
 * 实体渲染器、摆件方块实体渲染器、玩偶模型 LayerDefinition、皮肤重载监听（眨眼 + 手臂 UV 适配）、
 * 物品渲染器（物品栏图标 / 掉落物 / 展示框共用）。
 *
 * <p>「头顶玩偶」图层（{@code TwoIDollHeadLayer}）挂在 {@code ClientModEvents#onAddLayers}，
 * 那里已有 addLayerToPlayerSkin 辅助方法，本类不重复订阅。</p>
 *
 * <p>手持玩偶（第一 / 第三人称托在手上的姿态）由 {@code TwoIHeldDollMixin} 接管
 * {@code ItemInHandLayer}，已在 godofthings.mixins.json 登记为客户端 mixin。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class DollClientSetup
{
    private DollClientSetup()
    {
    }

    /** Curios 饰品栏渲染注册（源码同样在 FMLClientSetup 里 enqueueWork 调用） */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(TwoIDollCurioRenderer::register);
    }

    /** 玩偶模型骨骼（PlayerModel 那套，缩放与比例在 Model.createLayer 里） */
    @SubscribeEvent
    public static void registerDollLayer(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(TwoIDollRenderer.LAYER, TwoIDollRenderer.Model::createLayer);
    }

    /** 实体渲染器 + 摆件方块实体渲染器 */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(DollRegistry.TWO_I_DOLL_ENTITY.get(), TwoIDollRenderer::new);
        event.registerBlockEntityRenderer(DollRegistry.TWO_I_DOLL_DISPLAY_BE.get(),
                TwoIDollDisplayRenderer::new);
    }

    /**
     * 皮肤规范化 + 眨眼贴图 + 物品渲染器都在资源重载时重建
     * （{@code TwoIDollItemRenderer} 本身是个 ReloadListener，源码即如此）。
     */
    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener(TwoIDollItemRenderer.get());
    }
}
