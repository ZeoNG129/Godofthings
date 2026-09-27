package com.godofthings.beef.mixin;

import com.godofthings.beef.event.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mixin(value = Level.class)
public class LevelMixin {
    @Inject(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private <T extends Entity> void godofthings$filterBeefProtectedPlayers(EntityTypeTest<Entity, T> entityTypeTest, AABB area, Predicate<? super T> predicate, CallbackInfoReturnable<List<T>> cir) {
        List<T> filtered = godofthings$filterBeefProtectedPlayers(cir.getReturnValue());
        if (filtered != null) {
            cir.setReturnValue(filtered);
        }
    }

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void godofthings$filterBeefProtectedPlayersFromEntityQuery(Entity except, AABB area, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
        List<Entity> filtered = godofthings$filterBeefProtectedPlayers(cir.getReturnValue());
        if (filtered != null) {
            cir.setReturnValue(filtered);
        }
    }

    private static <T extends Entity> List<T> godofthings$filterBeefProtectedPlayers(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }

        List<T> filtered = null;
        for (int i = 0; i < entities.size(); i++) {
            T entity = entities.get(i);
            if (entity instanceof Player player && EventHandler.hasBeefAdvancedStealthItem(player)) {
                if (filtered == null) {
                    filtered = new ArrayList<>(entities.size());
                    for (T previous : entities.subList(0, i)) {
                        filtered.add(previous);
                    }
                }
                continue;
            }

            if (filtered != null) {
                filtered.add(entity);
            }
        }

        return filtered;
    }

    // 上游此处还有 isDay / isRaining / isThundering 三个注入，用来让「无用维度」永远晴天。
    // 那套维度属于上游的维度子系统，未随本次造化杖照抄带入；若保留，改成 godofthings
    // 命名空间后会反过来把本模组自己的超平坦 / 虚空维度变成永昼无雨，故一并裁掉。
}
