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
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0）。</p>
 *
 * <p><b>[本地改动] 泛型值类型必须是 {@link ExecutingCraftingJob.TaskProgress}</b>
 * （本工程无 refmap，{@code @Accessor} 精确匹配字段签名，写 {@code Object} 会匹配失败）。</p>
 */
@Mixin(value = ExecutingCraftingJob.class, remap = false)
public interface CraftingJobAccess extends JobAccess
{
    @Override
    @Accessor("tasks")
    Map<IPatternDetails, ?> god$getTasks();

    @Override
    @Accessor("waitingFor")
    ListCraftingInventory god$getWaitingFor();

    @Override
    @Accessor("link")
    CraftingLink god$getLink();
}
