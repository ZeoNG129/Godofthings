package com.godofthings.mixin;

import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 把 AE2 合成逻辑的 private 成员暴露给执行层。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.CpuLogicAccess}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0）。</p>
 *
 * <p><b>[本地改动] 返回类型写真实类型 {@link ExecutingCraftingJob}</b>：本工程不生成 refmap，
 * {@code @Accessor} 按「返回类型 + 字段名」精确匹配；上游写 {@code Object} 在本地会报
 * {@code InvalidAccessorException: No candidates were found matching job:Ljava/lang/Object;}
 * （v7.16.2 实测崩溃点）。</p>
 */
@Mixin(value = CraftingCpuLogic.class, remap = false)
public interface CpuLogicAccess
{
    @Accessor("job")
    ExecutingCraftingJob god$getJob();

    @Accessor("cluster")
    CraftingCPUCluster god$getCluster();
}
