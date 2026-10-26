package com.godofthings.aeskill.cpu;

/**
 * 单个样板任务的「剩余批次」读写契约。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.TaskAccess}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行照抄。</p>
 */
public interface TaskAccess
{
    long god$getRemaining();

    void god$setRemaining(long value);
}
