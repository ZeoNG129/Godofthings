package com.godofthings.beef.init;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.content.entities.BeefTimeAccelerationEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 造化杖（太初洞见之杖）用到的实体类型。
 * <p>逐字照抄上游 {@code com.sorrowmist.useless.init.ModEntities}，只保留与工具相关的项。</p>
 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, UselessMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<BeefTimeAccelerationEntity>> BEEF_TIME_ACCELERATION =
            ENTITY_TYPES.register(
                    "beef_time_acceleration",
                    () -> EntityType.Builder.<BeefTimeAccelerationEntity>of(
                                    BeefTimeAccelerationEntity::new,
                                    MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(UselessMod.id("beef_time_acceleration").toString())
            );

    private ModEntities() {
    }
}
