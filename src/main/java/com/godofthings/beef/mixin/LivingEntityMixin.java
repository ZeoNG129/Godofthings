package com.godofthings.beef.mixin;

import com.godofthings.beef.event.EventHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "canBeSeenByAnyone", at = @At("HEAD"), cancellable = true)
    private void godofthings$hideProtectedPlayerFromVisibilityChecks(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && EventHandler.hasBeefAdvancedStealthItem(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void godofthings$protectBeefPlayerFromDeathEvent(byte id, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (id == 3 && entity instanceof Player player && EventHandler.hasBeefInvulnerabilityItem(player)) {
            EventHandler.restoreBeefProtectedPlayer(player);
            ci.cancel();
        }
    }

    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void godofthings$protectBeefPlayerFromTickDeath(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && EventHandler.hasBeefInvulnerabilityItem(player)) {
            EventHandler.restoreBeefProtectedPlayer(player);
            ci.cancel();
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void godofthings$protectBeefPlayerFromSetHealth(float health, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if ((health <= 0.0F || Float.isNaN(health))
                && entity instanceof Player player
                && EventHandler.hasBeefInvulnerabilityItem(player)
                && !EventHandler.isRestoringBeefProtectedPlayer(player)) {
            EventHandler.restoreBeefProtectedPlayer(player);
            ci.cancel();
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void godofthings$protectBeefPlayerFromHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if ((!Float.isFinite(amount) || amount >= entity.getMaxHealth() || entity.getHealth() - amount <= 0.0F)
                && entity instanceof Player player
                && EventHandler.hasBeefInvulnerabilityItem(player)
                && !EventHandler.isRestoringBeefProtectedPlayer(player)) {
            EventHandler.restoreBeefProtectedPlayer(player);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void godofthings$protectBeefPlayerFromDie(DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player
                && EventHandler.hasBeefInvulnerabilityItem(player)
                && !EventHandler.isRestoringBeefProtectedPlayer(player)) {
            EventHandler.restoreBeefProtectedPlayer(player);
            ci.cancel();
        }
    }
}
