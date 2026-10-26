package com.godofthings.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.me.service.TickManagerService;
import com.godofthings.aeskill.cpu.AssemblerOverclock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让「装配超频」对<b>任意</b>装配机生效（不只是 AE2 原生分子装配室）。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.AssemblerTickMixin}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行对照编写。</p>
 *
 * <p>AE2 的 {@code unsafeTickingRequest} 里有<b>两处</b>
 * {@code IGridTickable.tickingRequest} 调用（受监控 / 不受监控两条路径），正常应匹配 2 处。</p>
 *
 * <p><b>必须 require = 0</b>（v7.16.4 教训）：若实例里同时装着原版 {@code zifeng_ae2_skills}，
 * 它在同一调用点也做了 @Redirect 且优先级相同，Mixin 会判定冲突并跳过本注入。写成
 * {@code require = 2} 时这一步会直接抛 {@code InjectionError} 崩在加载期——冲突只是
 * 「功能重复」，不该是崩溃理由。</p>
 */
@Mixin(value = TickManagerService.class, remap = false)
public abstract class AssemblerTickMixin
{
    @Redirect(method = "unsafeTickingRequest", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/ticking/IGridTickable;tickingRequest(Lappeng/api/networking/IGridNode;I)Lappeng/api/networking/ticking/TickRateModulation;"),
            require = 0)
    private TickRateModulation god$overclock(IGridTickable tickable, IGridNode node, int elapsed)
    {
        return AssemblerOverclock.tick(tickable, node, elapsed);
    }
}
