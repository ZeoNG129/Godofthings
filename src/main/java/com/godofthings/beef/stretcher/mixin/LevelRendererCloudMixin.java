package com.godofthings.beef.stretcher.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.godofthings.beef.stretcher.client.WondrousStaffCloudTime;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces only vanilla's cloud-motion clock while a dimension-wide time effect is active.
 *
 * <p><b>priority = 500 是刻意为之，不要改回默认值。</b>
 * useless_stretcher（万象担架）扩展模组自带一个与本类同样的 {@code @Redirect}
 * （打在 {@code LevelRenderer.ticks} 上）。两个 {@code @Redirect} 撞在同一个字段读取上时
 * Mixin 只留优先级高者，同优先级则先注册者赢 —— 那种「谁赢看加载顺序」的结果不确定。
 * 这里把优先级压到 1000 以下，让<b>原版扩展模组稳赢</b>。
 * 两边都写了 {@code require = 0}，所以谁被跳过都只是警告、不会崩；
 * 代价是同时装扩展模组时，本模组这把杖的云层加速不生效（纯视觉）。</p>
 */
@Mixin(value = LevelRenderer.class, priority = 500)
public abstract class LevelRendererCloudMixin {
    @Unique
    private float uselessStretcher$cloudPartialTick;

    @Inject(
            method = "renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V",
            at = @At("HEAD"),
            require = 0)
    private void uselessStretcher$captureCloudPartialTick(PoseStack poseStack, Matrix4f frustumMatrix,
                                                           Matrix4f projectionMatrix, float partialTick,
                                                           double camX, double camY, double camZ,
                                                           CallbackInfo ci) {
        this.uselessStretcher$cloudPartialTick = partialTick;
    }

    @Redirect(
            method = "renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;ticks:I",
                    ordinal = 1),
            require = 0)
    private int uselessStretcher$accelerateCloudClock(LevelRenderer renderer) {
        return WondrousStaffCloudTime.cloudTicks(renderer.getTicks(),
                this.uselessStretcher$cloudPartialTick);
    }
}
