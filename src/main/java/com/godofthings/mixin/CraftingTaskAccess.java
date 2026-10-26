package com.godofthings.mixin;

import appeng.crafting.execution.ExecutingCraftingJob;
import com.godofthings.aeskill.cpu.TaskAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读写单个样板任务的剩余批次。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.CraftingTaskAccess}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行照抄。</p>
 */
@Mixin(targets = "appeng.crafting.execution.ExecutingCraftingJob$TaskProgress", remap = false)
public interface CraftingTaskAccess extends TaskAccess
{
    @Override
    @Accessor("value")
    long god$getRemaining();

    @Override
    @Accessor("value")
    void god$setRemaining(long value);
}
