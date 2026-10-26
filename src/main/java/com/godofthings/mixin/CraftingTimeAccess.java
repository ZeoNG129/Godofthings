package com.godofthings.mixin;

import appeng.api.stacks.AEKeyType;
import appeng.crafting.execution.ElapsedTimeTracker;
import com.godofthings.aeskill.cpu.TimeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 让执行层能累加时间追踪器的「最大物品数」。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.CraftingTimeAccess}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行照抄。</p>
 */
@Mixin(value = ElapsedTimeTracker.class, remap = false)
public interface CraftingTimeAccess extends TimeAccess
{
    @Override
    @Invoker("addMaxItems")
    void god$addMaxItems(long amount, AEKeyType type);
}
