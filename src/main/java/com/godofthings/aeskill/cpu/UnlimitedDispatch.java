package com.godofthings.aeskill.cpu;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.me.service.CraftingService;
import com.godofthings.aeskill.TickBudget;

import java.util.ArrayList;
import java.util.List;

/**
 * 无限并行单元：只解除「操作次数」上限，绝不越过机器 / 原料 / 电力约束。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.UnlimitedDispatch}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），<b>逐行对照编写</b>。</p>
 */
public final class UnlimitedDispatch
{
    private UnlimitedDispatch() {}

    public static final class Cursor
    {
        private Object job;
        private List<IPatternDetails> patterns = List.of();
        private int next;

        public void clear()
        {
            job = null;
            patterns = List.of();
            next = 0;
        }
    }

    public static void tick(CpuExecution logic, Cursor cursor, CraftingService service, IEnergyService energy)
    {
        long began = System.nanoTime();
        long deadline = TickBudget.deadline(began);
        if (deadline <= began)
        {
            return;
        }
        Object original = logic.god$job();
        if (original == null)
        {
            cursor.clear();
            return;
        }
        JobAccess job = (JobAccess) original;
        if (logic.god$suspended() || job.god$getLink().isCanceled() || !logic.god$active())
        {
            return;
        }
        try
        {
            if (cursor.job != original)
            {
                cursor.job = original;
                cursor.patterns = new ArrayList<>(job.god$getTasks().keySet());
                cursor.next = 0;
            }
            int scanned = 0;
            boolean advanced = false;
            while (!cursor.patterns.isEmpty() && System.nanoTime() < deadline)
            {
                IPatternDetails pattern = cursor.patterns.get(cursor.next);
                cursor.next = (cursor.next + 1) % cursor.patterns.size();
                Object entry = job.god$getTasks().get(pattern);
                if (entry instanceof TaskAccess task && task.god$getRemaining() > 0)
                {
                    KeyCounter outputs = new KeyCounter();
                    KeyCounter containers = new KeyCounter();
                    var inputs = CraftingCpuHelper.extractPatternInputs(pattern, logic.god$inventory(),
                            logic.god$level(), outputs, containers);
                    if (inputs != null)
                    {
                        boolean pushed = false;
                        try
                        {
                            double power = CraftingCpuHelper.calculatePatternPower(inputs);
                            if (energy.extractAEPower(power, Actionable.SIMULATE, PowerMultiplier.CONFIG) >= power - 0.01D)
                            {
                                for (var provider : service.getProviders(pattern))
                                {
                                    if (System.nanoTime() >= deadline)
                                    {
                                        break;
                                    }
                                    if (provider.isBusy() || !provider.pushPattern(pattern, inputs))
                                    {
                                        continue;
                                    }
                                    pushed = true;
                                    energy.extractAEPower(power, Actionable.MODULATE, PowerMultiplier.CONFIG);
                                    for (var output : outputs)
                                    {
                                        job.god$getWaitingFor().insert(output.getKey(), output.getLongValue(), Actionable.MODULATE);
                                    }
                                    for (var remainder : containers)
                                    {
                                        job.god$getWaitingFor().insert(remainder.getKey(), remainder.getLongValue(), Actionable.MODULATE);
                                        logic.god$addMaxItems(remainder.getLongValue(), remainder.getKey().getType());
                                    }
                                    task.god$setRemaining(task.god$getRemaining() - 1);
                                    logic.god$markDirty();
                                    advanced = true;
                                    if (task.god$getRemaining() == 0)
                                    {
                                        job.god$getTasks().remove(pattern);
                                    }
                                    break;
                                }
                            }
                        }
                        finally
                        {
                            if (!pushed)
                            {
                                CraftingCpuHelper.reinjectPatternInputs(logic.god$inventory(), inputs);
                            }
                        }
                    }
                }
                if (logic.god$job() != original)
                {
                    cursor.clear();
                    return;
                }
                if (++scanned >= cursor.patterns.size())
                {
                    if (!advanced)
                    {
                        break;
                    }
                    scanned = 0;
                    advanced = false;
                }
            }
        }
        finally
        {
            TickBudget.charge(began);
        }
    }
}
