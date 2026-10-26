package com.godofthings.aeskill.cpu;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftingLink;
import appeng.crafting.inv.ListCraftingInventory;

import java.util.Map;

/**
 * 作业任务状态的公共契约（由 AE2 执行器的访问器 mixin 实现）。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.JobAccess}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0）。</p>
 *
 * <p><b>[本地改动] 任务表用 {@code Map<IPatternDetails, ?>} 而不是上游的
 * {@code Map<IPatternDetails, Object>}</b>：AE2 的 {@code TaskProgress} 是包私有内部类，
 * 外部无法写出精确类型；而 {@code @Accessor} 又要求签名匹配。用通配符 {@code ?} 让
 * javac 生成的擦除签名与字段一致（{@code Map}），配合 {@code @Accessor} 按名绑定即可。</p>
 */
public interface JobAccess
{
    Map<IPatternDetails, ?> god$getTasks();

    ListCraftingInventory god$getWaitingFor();

    CraftingLink god$getLink();
}
