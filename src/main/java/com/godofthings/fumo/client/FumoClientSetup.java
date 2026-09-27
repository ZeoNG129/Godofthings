package com.godofthings.fumo.client;

import com.godofthings.Godofthings;
import com.godofthings.fumo.registry.ModFumos;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * 「HoYooG Fumo」的客户端接线。
 *
 * <p>对应上游 AE2LT 的 {@code client.ModEntityRenderers} 里与 fumo 有关的两段：
 * 注册方块实体渲染器、以及把物品栏模型包一层 {@link SpinningFumoBakedModel}
 * （戴在头上时绕 Y 轴自转）。上游那两段还一并处理了其它 4 个玩偶与「超维猪咪」的
 * 自定义物品渲染器，本模组只保留本玩偶所需的路径。</p>
 *
 * <p>照抄来源与许可：结构与 {@link SpinningFumoBakedModel} 同源，
 * 源码部分沿用上游的 <b>GNU LGPL 3.0</b>（见 {@code LICENSES/AE2LT-LGPL-3.0.txt}）。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FumoClientSetup {

    private FumoClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModFumos.FUMO.get(), FumoBlockRenderer::new);
    }

    @SubscribeEvent
    public static void wrapFumoItemModels(ModelEvent.ModifyBakingResult event) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "hoyoog_fumo");
        ModelResourceLocation modelId = ModelResourceLocation.inventory(id);
        event.getModels().computeIfPresent(modelId, (ignored, model) -> new SpinningFumoBakedModel(model));
    }
}
