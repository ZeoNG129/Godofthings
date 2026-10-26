package com.godofthings.aeskill.cpu;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.inv.ListCraftingInventory;
import com.godofthings.mixin.CpuLogicAccess;
import com.godofthings.mixin.CraftingTimeAccess;
import net.minecraft.world.level.Level;

/**
 * AE2 原生合成逻辑对 {@link CpuExecution} 契约的实现。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.NativeCpuExecution}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行照抄。</p>
 */
public record NativeCpuExecution(CraftingCpuLogic logic) implements CpuExecution
{
    private CpuLogicAccess access()
    {
        return (CpuLogicAccess) logic;
    }

    @Override
    public Object god$job()
    {
        return access().god$getJob();
    }

    @Override
    public ListCraftingInventory god$inventory()
    {
        return logic.getInventory();
    }

    @Override
    public Level god$level()
    {
        return access().god$getCluster().getLevel();
    }

    @Override
    public IGrid god$grid()
    {
        return access().god$getCluster().getGrid();
    }

    @Override
    public IActionSource god$source()
    {
        return access().god$getCluster().getSrc();
    }

    @Override
    public boolean god$active()
    {
        return access().god$getCluster().isActive();
    }

    @Override
    public boolean god$suspended()
    {
        return logic.isJobSuspended();
    }

    @Override
    public GenericStack god$finalOutput()
    {
        return logic.getFinalJobOutput();
    }

    @Override
    public long god$insert(AEKey key, long amount, Actionable mode)
    {
        return logic.insert(key, amount, mode);
    }

    @Override
    public void god$markDirty()
    {
        access().god$getCluster().markDirty();
    }

    @Override
    public void god$addMaxItems(long amount, AEKeyType type)
    {
        ((CraftingTimeAccess) logic.getElapsedTimeTracker()).god$addMaxItems(amount, type);
    }
}
