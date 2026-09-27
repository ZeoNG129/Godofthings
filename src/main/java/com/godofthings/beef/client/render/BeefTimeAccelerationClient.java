package com.godofthings.beef.client.render;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.init.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = UselessMod.MODID, value = Dist.CLIENT)
public final class BeefTimeAccelerationClient {
    private BeefTimeAccelerationClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BEEF_TIME_ACCELERATION.get(), BeefTimeAccelerationRenderer::new);
    }
}
