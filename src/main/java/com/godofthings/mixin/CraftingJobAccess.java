package com.godofthings.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftingLink;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import com.godofthings.aeskill.cpu.JobAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * 把已执行作业的任务表 / 等待表 / 链接暴露给执行层。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.CraftingJobAccess}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行照抄。</p>
 */
@Mixin(value = ExecutingCraftingJob.class, remap = false)
public interface CraftingJobAccess extends JobAccess
{
    @Override
    @Accessor("tasks")
    Map<IPatternDetails, Object> god$getTasks();

    @Override
    @Accessor("waitingFor")
    ListCraftingInventory god$getWaitingFor();

    @Override
    @Accessor("link")
    CraftingLink god$getLink();
}
