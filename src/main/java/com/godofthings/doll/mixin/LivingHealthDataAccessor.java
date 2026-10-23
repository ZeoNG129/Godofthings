package com.godofthings.doll.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 拿到 {@link LivingEntity} 里私有的 {@code DATA_HEALTH_ID}。
 *
 * <p>玩偶「不死」需要直接改同步血量字段，而原版只暴露 getter/setter，
 * 拿不到 {@link EntityDataAccessor} 本身。这是一个纯 Accessor Mixin，
 * 不改任何行为，只提供一个读取入口。</p>
 */
@Mixin(LivingEntity.class)
public interface LivingHealthDataAccessor
{
    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> godofthings$healthKey()
    {
        throw new AssertionError("mixin accessor");
    }
}