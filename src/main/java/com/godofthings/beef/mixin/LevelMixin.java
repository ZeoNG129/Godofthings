package com.godofthings.beef.mixin;

import com.godofthings.beef.event.EventHandler;
import com.godofthings.beef.world.dimension.UselessDimensions;
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

    // ==== 以下三个注入随「无用维度」子系统一同补回（v4.1.0）====
    // 之前只移植造化杖时被裁掉，因为那时无用维度没带进来；现在维度已照抄，
    // 这层「无用维度永远晴天」的表现也必须一起回来。
    // 注意 isUselessDimension() 只认 UselessDimensions 里那三个维度键（uselessdim/2/3），
    // 不会误伤本模组自己的超平坦 / 虚空维度。

    @Inject(
            method = "isDay",
            at = @At("HEAD"),
            cancellable = true
    )
    private void injectIsDay(CallbackInfoReturnable<Boolean> cir) {
        if (this.isUselessDimension()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "isRaining",
            at = @At("HEAD"),
            cancellable = true
    )
    private void uselessDimAlwaysClear_rain(CallbackInfoReturnable<Boolean> cir) {
        if (this.isUselessDimension()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "isThundering",
            at = @At("HEAD"),
            cancellable = true
    )
    private void uselessDimAlwaysClear_thunder(CallbackInfoReturnable<Boolean> cir) {
        if (this.isUselessDimension()) {
            cir.setReturnValue(false);
        }
    }

    private boolean isUselessDimension() {
        Level level = (Level) (Object) this;
        return UselessDimensions.isUselessDimension(level.dimension());
    }
}
