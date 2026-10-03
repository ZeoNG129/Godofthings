package com.godofthings.mixin;

import com.godofthings.armor.GodArmorFeatures;
import com.godofthings.armor.GodArmorState;
import com.godofthings.handler.GodArmorHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「神之抗火」（原「火焰熔岩免疫」）开着且穿齐全套时，<b>去掉屏幕上的火焰/岩浆覆盖贴图</b>。
 * <p>
 * 之前只做了伤害与雾效的免疫，但<b>着火/浸在岩浆里时屏幕上那层橙色火焰纹理会照旧绘制</b>
 * （由 {@link ScreenEffectRenderer#renderScreenEffect} 负责），挡视野。这里在入口直接取消。
 * <p>
 * {@code require = 0}：注入失败只降级（覆盖层照旧），绝不崩游戏。
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin
{
    @Inject(method = "renderScreenEffect", at = @At("HEAD"), cancellable = true, require = 0)
    private static void godofthings$noFireOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci)
    {
        if (minecraft.player == null)
        {
            return;
        }
        if (!GodArmorFeatures.isOn(GodArmorState.getClientMask(), GodArmorFeatures.FIRE_RESIST))
        {
            return; // 没开抗火：原样显示
        }
        if (!GodArmorHandler.isFullSetWorn(minecraft.player))
        {
            return; // 没穿齐全套：原样显示
        }
        if (minecraft.player.isOnFire() || minecraft.player.isInLava()
                || minecraft.player.getRemainingFireTicks() > 0)
        {
            ci.cancel(); // 免疫了就别再用火焰贴图挡视野
        }
    }
}