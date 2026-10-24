package com.godofthings.wondrous.init;

import com.godofthings.Godofthings;
import com.godofthings.wondrous.content.entity.StaffLeafRewardEntity;
import com.godofthings.wondrous.content.entity.TimeFlowEntity;
import com.godofthings.wondrous.content.entity.WondrousStaffAccelerationEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 荒辰移晷之杖的实体注册（照抄上游 ModEntities 的杖相关三个实体）。
 * 上游的玩偶/星空维度实体不移植（本模组自己的玩偶体系已在 doll 包）。
 */
public final class WondrousEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Godofthings.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WondrousStaffAccelerationEntity>> WONDROUS_STAFF_ACCELERATION =
            ENTITY_TYPES.register("wondrous_staff_acceleration", () -> EntityType.Builder
                    .<WondrousStaffAccelerationEntity>of(WondrousStaffAccelerationEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("wondrous_staff_acceleration"));

    public static final DeferredHolder<EntityType<?>, EntityType<TimeFlowEntity>> TIME_FLOW =
            ENTITY_TYPES.register("time_flow", () -> EntityType.Builder
                    .<TimeFlowEntity>of(TimeFlowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("time_flow"));

    public static final DeferredHolder<EntityType<?>, EntityType<StaffLeafRewardEntity>> STAFF_LEAF_REWARD =
            ENTITY_TYPES.register("staff_leaf_reward", () -> EntityType.Builder
                    .<StaffLeafRewardEntity>of(StaffLeafRewardEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("staff_leaf_reward"));

    private WondrousEntities()
    {
    }

    /** 由 Godofthings 构造器调用。 */
    public static void register(IEventBus modEventBus)
    {
        ENTITY_TYPES.register(modEventBus);
    }
}
