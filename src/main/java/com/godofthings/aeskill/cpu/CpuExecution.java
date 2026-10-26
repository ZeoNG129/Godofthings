package com.godofthings.aeskill.cpu;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.crafting.inv.ListCraftingInventory;
import net.minecraft.world.level.Level;

/**
 * 合成 CPU 执行器的运行期适配契约（不承载任务创建、取消与持久化）。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.CpuExecution}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行照抄（方法名换成 {@code god$} 前缀）。</p>
 */
public interface CpuExecution
{
    Object god$job();

    ListCraftingInventory god$inventory();

    Level god$level();

    IGrid god$grid();

    IActionSource god$source();

    boolean god$active();

    boolean god$suspended();

    GenericStack god$finalOutput();

    long god$insert(AEKey key, long amount, Actionable mode);

    void god$markDirty();

    void god$addMaxItems(long amount, AEKeyType type);

    default long god$stored(AEKey key)
    {
        return god$inventory().list.get(key);
    }
}
