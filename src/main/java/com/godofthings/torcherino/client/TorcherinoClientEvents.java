package com.godofthings.torcherino.client;

import com.godofthings.Godofthings;
import com.godofthings.torcherino.Torcherino;
import net.minecraft.client.particle.FlameParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 加速火把客户端注册：粒子工厂。
 * 方块渲染层（cutout）已由模型 JSON 的 "render_type" 字段声明，无需代码注册。
 */
@Mod.EventBusSubscriber(modid = Godofthings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TorcherinoClientEvents
{
    private TorcherinoClientEvents() {}

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event)
    {
        event.registerSpriteSet(Torcherino.PARTICLE_FLAME.get(), FlameParticle.Provider::new);
        event.registerSpriteSet(Torcherino.PARTICLE_COMPRESSED_FLAME.get(), FlameParticle.Provider::new);
        event.registerSpriteSet(Torcherino.PARTICLE_DOUBLE_COMPRESSED_FLAME.get(), FlameParticle.Provider::new);
    }
}
