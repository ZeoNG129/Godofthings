package com.godofthings.mixin;

import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 把 AE2 合成逻辑的 private 成员暴露给执行层。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.CpuLogicAccess}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行照抄。</p>
 */
@Mixin(value = CraftingCpuLogic.class, remap = false)
public interface CpuLogicAccess
{
    @Accessor("job")
    Object god$getJob();

    @Accessor("cluster")
    CraftingCPUCluster god$getCluster();
}
