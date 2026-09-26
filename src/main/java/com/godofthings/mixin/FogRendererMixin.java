package com.godofthings.mixin;

import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkills;
import com.godofthings.armor.GodArmorFeatures;
import com.godofthings.armor.GodArmorState;
import com.godofthings.handler.GodArmorHandler;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 碧波清眸（套装功能「碧波清眸」）：水底 / 岩浆中拥有清晰视野。
 * <p>
 * <b>移植自 Zifeng Skill Tree（子枫的百宝箱）的 {@code FogRendererMixin}</b>
 * （Copyright (c) 2026 zifeng, MIT License，见 README「第三方代码与许可」）。
 *
 * <h3>为什么必须用 Mixin，而不是 ViewportEvent</h3>
 * 参考模组最初用 {@code ViewportEvent.RenderFog} 拉远雾距，但<b>对水下无效</b>：
 * 该事件在 {@code setupFog} 内部设置完 shader 之后才触发，而水下是<b>球体指数雾</b>，
 * 改 start/end 玩家仍只能看一两格。最终改为在 {@code setupFog} <b>入口直接拦截</b>：
 * 技能开启时等价于原版 {@code setupNoFog}（雾 start/end 设为 {@code Float.MAX_VALUE} → 雾完全禁用），
 * 视野与空气中一致。
 *
 * <h3>生效条件（全部满足才拦截）</h3>
 * <ul>
 *   <li>相机浸没在水或岩浆里（其余情况只多一次 FogType 判断，零开销）</li>
 *   <li>相机实体是<b>本地玩家</b>（多人游戏里不影响别人视角）</li>
 *   <li>碧波清眸已开启（客户端镜像）且<b>穿齐全套神之护甲</b></li>
 * </ul>
 * {@code require = 0}：注入失败只降级（雾照旧），绝不崩游戏。
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin
{
    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true, require = 0)
    private static void godofthings$clearFluidFog(Camera camera, FogRenderer.FogMode mode,
                                                  float renderDistance, boolean foggy, float partialTick,
                                                  CallbackInfo ci)
    {
        FogType type = camera.getFluidInCamera();
        if (type != FogType.WATER && type != FogType.LAVA)
        {
            return; // 非水/岩浆：走原逻辑，零开销
        }
        Entity entity = camera.getEntity();
        if (!(entity instanceof LocalPlayer player))
        {
            return; // 仅本地玩家视角
        }
        if (!GodArmorFeatures.isOn(GodArmorState.getClientMask(), GodArmorFeatures.UNDERWATER_VISION)
                || !GodArmorHandler.isFullSetWorn(player))
        {
            return; // 技能未开启 或 未穿齐全套
        }
        // 完全禁用雾（等价原版 setupNoFog）
        RenderSystem.setShaderFogStart(Float.MAX_VALUE);
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
        RenderSystem.setShaderFogShape(FogShape.CYLINDER);
        ci.cancel();
    }
}
