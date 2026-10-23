package com.godofthings.doll.client;

import com.godofthings.Godofthings;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.render.TwoIDollDisplayRenderer;
import com.godofthings.doll.render.TwoIDollHandRenderer;
import com.godofthings.doll.render.TwoIDollHeadLayer;
import com.godofthings.doll.render.TwoIDollItemRenderer;
import com.godofthings.doll.render.TwoIDollRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * 玩偶的客户端接线。
 *
 * <p>对应原 mod 的 {@code ClientSetup} / {@code UselessStretcherClient} 里与玩偶有关的部分。
 * 集中放在一个类里，避免散落到主模组的客户端事件中。</p>
 *
 * <p><b>为什么挂 {@code Dist.CLIENT}</b>：本类只被客户端引用，专用服务端不会加载它；
 * 且它<b>确实有</b> {@code @SubscribeEvent} 方法 —— 这是本仓库的硬规则：
 * 标了 {@code @EventBusSubscriber} 却没有订阅方法的类会让 NeoForge 在构造 mod 阶段
 * 直接 {@code IllegalArgumentException}，整个模组加载失败。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DollClientSetup
{
    private DollClientSetup()
    {
    }

    /** 注册玩偶实体渲染器 + 摆件方块实体渲染器 */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(DollRegistry.TWO_I_DOLL_ENTITY.get(), TwoIDollRenderer::new);
        event.registerBlockEntityRenderer(DollRegistry.TWO_I_DOLL_DISPLAY_BE.get(),
                TwoIDollDisplayRenderer::new);
    }

    /** 注册玩偶模型的 LayerDefinition（PlayerModel 那套骨骼） */
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event)
    {
        event.registerLayerDefinition(TwoIDollRenderer.LAYER, TwoIDollRenderer.Model::createLayer);
    }

    // 玩家模型的「头顶玩偶」图层挂在 ClientModEvents#onAddLayers 里（那里已有
    // addLayerToPlayerSkin 辅助方法），本类不重复订阅 AddLayers，避免同一层被加两次。

    /** 玩偶皮肤的规范化 + 眨眼贴图（reload 时重建） */
    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event)
    {
        com.godofthings.doll.render.TwoIDollSkin.registerReloadListener(event);
    }

    /** 物品栏里的玩偶图标用实体渲染 */
    public static void registerItemRenderer()
    {
        // TwoIDollItemRenderer 通过 builtin/entity 模型自动生效，这里只保留入口以便将来扩展
        TwoIDollItemRenderer.get();
    }
}
