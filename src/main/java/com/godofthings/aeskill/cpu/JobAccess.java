package com.godofthings.aeskill.cpu;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftingLink;
import appeng.crafting.inv.ListCraftingInventory;

import java.util.Map;

/**
 * 作业任务状态的公共契约（由 AE2 执行器的访问器 mixin 实现）。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.JobAccess}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行照抄。</p>
 */
public interface JobAccess
{
    Map<IPatternDetails, Object> god$getTasks();

    ListCraftingInventory god$getWaitingFor();

    CraftingLink god$getLink();
}
