package com.godofthings.aeskill.cpu;

import appeng.api.stacks.AEKeyType;

/**
 * 时间追踪器的「最大物品数」累加契约（用于批量合成时正确显示耗时预估）。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.TimeAccess}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行照抄。</p>
 */
public interface TimeAccess
{
    void god$addMaxItems(long amount, AEKeyType type);
}
