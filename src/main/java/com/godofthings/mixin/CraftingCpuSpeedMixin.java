package com.godofthings.mixin;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.godofthings.aeskill.cpu.CpuExtension;
import com.godofthings.aeskill.cpu.VirtualCpus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「算力超频 / 分裂算力」钩子：放大合成 CPU 的并行处理单元与存储容量。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.CraftingCpuSpeedMixin}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），<b>逐行对照编写</b>。</p>
 *
 * <p>超频是「按作业」生效的；分裂出来的 CPU 共享同一份物理存储池，
 * 所以这里统一走 {@link VirtualCpus}，保证同一台 CPU 不会因为走不同路径算出两个数字。</p>
 */
@Mixin(value = CraftingCPUCluster.class, priority = 900, remap = false)
public abstract class CraftingCpuSpeedMixin
{
    /** 并行处理单元：算力超频倍率放大。 */
    @Inject(method = "getCoProcessors", at = @At("RETURN"), cancellable = true, require = 1)
    private void god$scaleCoProcessors(CallbackInfoReturnable<Integer> ci)
    {
        // 正在读原始值（放大逻辑在读 base）→ 直接放行，防止自我递归
        if (((CpuExtension) (Object) this).god$readingBase())
        {
            return;
        }
        if (VirtualCpus.state((CraftingCPUCluster) (Object) this).externallyManaged)
        {
            return;
        }
        ci.setReturnValue(VirtualCpus.coProcessors((CraftingCPUCluster) (Object) this));
    }

    /** 合成存储容量：分裂算力倍率放大 + 虚拟 CPU 共享池扣减。 */
    @Inject(method = "getAvailableStorage", at = @At("RETURN"), cancellable = true, require = 1)
    private void god$sharedStorage(CallbackInfoReturnable<Long> ci)
    {
        if (((CpuExtension) (Object) this).god$readingBase())
        {
            return;
        }
        if (VirtualCpus.state((CraftingCPUCluster) (Object) this).externallyManaged)
        {
            return;
        }
        ci.setReturnValue(VirtualCpus.availableStorage((CraftingCPUCluster) (Object) this));
    }
}
