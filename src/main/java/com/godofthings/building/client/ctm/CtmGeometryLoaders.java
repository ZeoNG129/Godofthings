package com.godofthings.building.client.ctm;

import com.godofthings.Godofthings;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Registers the {@code godofthings:connected_texture} geometry loader (ported from AE2 Lightning Tech). */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CtmGeometryLoaders {

    private CtmGeometryLoaders() {
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "connected_texture"),
                new ConnectedTextureLoader());
    }
}
