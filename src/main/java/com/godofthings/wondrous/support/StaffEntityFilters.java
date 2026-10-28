package com.godofthings.wondrous.support;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * 上游 TwoIDollEntity / TimeFlowEntity 排除判定的本地替身（[本地改动]）。
 * <p>
 * 上游禁止杖加速自家玩偶与范围锚点实体；本模组的对应物是
 * {@code com.godofthings.doll.entity.TwoIDollEntity}（紬紬玩偶），
 * 加上移植过来的 {@code TimeFlowEntity}（范围锚点，随杖一同移植）。
 * 判定按实体类型注册名做，避免硬类引用。
 */
public final class StaffEntityFilters
{
    private StaffEntityFilters()
    {
    }

    public static boolean isProtectedFromAcceleration(Entity entity)
    {
        if (entity == null) return false;
        EntityType<?> type = entity.getType();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null) return false;
        // 本模组的紬紬玩偶（不可被加速 / 不可被吞）
        if ("godofthings".equals(id.getNamespace()) && id.getPath().startsWith("mugi")) return true;
        // 随杖移植的范围锚点
        if (entity instanceof com.godofthings.wondrous.content.entity.TimeFlowEntity) return true;
        // 随杖移植的加速标记 / 叶奖励实体
        if (entity instanceof com.godofthings.wondrous.content.entity.WondrousStaffAccelerationEntity) return true;
        if (entity instanceof com.godofthings.wondrous.content.entity.StaffLeafRewardEntity) return true;
        // 上游的 jdte / justdirethings 时钟排除逻辑保留在 WondrousStaffAcceleration 里
        return false;
    }
}
