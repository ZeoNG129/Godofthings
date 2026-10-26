package com.godofthings.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.CraftingSubmitResult;
import appeng.me.service.CraftingService;
import com.godofthings.aeskill.AeSkillEffects;
import com.godofthings.aeskill.cpu.CpuExecution;
import com.godofthings.aeskill.cpu.InstantCrafting;
import com.godofthings.aeskill.cpu.NativeCpuExecution;
import com.godofthings.aeskill.cpu.UnlimitedDispatch;
import com.godofthings.aeskill.cpu.VirtualCpus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「合成之神 / 算力之神」的驱动入口：把瞬时合成、无限派发、CPU 槽位守卫接到 AE2 的 tick 上。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.AeInstantCpuMixin}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），<b>逐行对照编写</b>。</p>
 *
 * <p>顺序很重要：在 AE2 原生 {@code tickCraftingLogic} 之前先跑我们的批量执行，
 * 这样同一 tick 内原生逻辑再补一次常规推进，两者不会互相覆盖「剩余批次」。</p>
 */
@Mixin(value = CraftingCpuLogic.class, remap = false)
public abstract class AeInstantCpuMixin
{
    @Unique
    private final InstantCrafting.Cursor god$instantCursor = new InstantCrafting.Cursor();

    @Unique
    private final UnlimitedDispatch.Cursor god$unlimitedCursor = new UnlimitedDispatch.Cursor();

    @Inject(method = "tickCraftingLogic", at = @At("HEAD"), require = 1)
    private void god$skillTick(IEnergyService energy, CraftingService service, CallbackInfo ci)
    {
        CraftingCpuLogic self = (CraftingCpuLogic) (Object) this;
        CpuExecution execution = new NativeCpuExecution(self);
        // 合成之神：合成台样板在一个 tick 内批量完成
        InstantCrafting.tick(execution, god$instantCursor, energy, service);
        // 算力之神：处理样板无限并行派发（受同一条 tick 预算约束）
        if (AeSkillEffects.getCpuInfiniteLevel() > 0)
        {
            UnlimitedDispatch.tick(execution, god$unlimitedCursor, service, energy);
        }
    }

    @Inject(method = "trySubmitJob", at = @At("HEAD"), cancellable = true, require = 1)
    private void god$guardSlot(IGrid grid, ICraftingPlan plan, IActionSource source,
                              ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> ci)
    {
        var cluster = ((CpuLogicAccess) this).god$getCluster();
        if (!VirtualCpus.canSubmit(cluster, grid))
        {
            ci.setReturnValue(CraftingSubmitResult.CPU_BUSY);
        }
    }

    @Inject(method = "trySubmitJob", at = @At("RETURN"), require = 1)
    private void god$reserveCapacity(IGrid grid, ICraftingPlan plan, IActionSource source,
                                    ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> ci)
    {
        if (ci.getReturnValue().successful())
        {
            var cluster = ((CpuLogicAccess) this).god$getCluster();
            VirtualCpus.state(cluster).reservedBytes = Math.max(0, plan.bytes());
            cluster.markDirty();
        }
    }
}
