package com.godofthings.aeskill.cpu;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.service.CraftingService;
import com.godofthings.aeskill.AeSkillEffects;
import com.godofthings.aeskill.TickBudget;

import java.util.ArrayList;
import java.util.List;

/**
 * 「合成之神」：合成台样板在一个 tick 内批量完成。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.InstantCrafting}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），<b>逐行对照编写</b>，方法前缀改为 {@code god$}。</p>
 *
 * <p><b>只处理合成台样板</b>：处理样板（需要外部机器）一律留给 AE2 原生执行器，
 * 因为处理样板可以声明任意产出，本模块替它批量结算等于凭空造物。</p>
 */
public final class InstantCrafting
{
    /** 正在交付「最终产物」的深度（用于区分中间产物与最终产物）。 */
    private static int finalDeliveryDepth;

    private InstantCrafting() {}

    public static boolean isDeliveringFinalOutput()
    {
        return finalDeliveryDepth > 0;
    }

    /** 样板游标跨时间切片保留，这样早期被阻塞的配方不会饿死后面的配方。 */
    public static final class Cursor
    {
        private Object job;
        private List<IPatternDetails> patterns = List.of();
        private int next;

        private void clear()
        {
            job = null;
            patterns = List.of();
            next = 0;
        }
    }

    public static void tick(CpuExecution logic, Cursor cursor, IEnergyService energy, CraftingService service)
    {
        int level = AeSkillEffects.getAssemblerInfiniteLevel();
        Object originalJob = logic.god$job();
        if (level == 0 || originalJob == null)
        {
            cursor.clear();
            return;
        }
        if (logic.god$suspended())
        {
            return;
        }
        JobAccess job = (JobAccess) originalJob;
        if (!logic.god$active() || job.god$getLink().isCanceled())
        {
            return;
        }

        long now = System.nanoTime();
        long deadline = TickBudget.deadline(now);
        if (deadline <= now)
        {
            return;
        }
        long quota = level == 5 ? Long.MAX_VALUE : AeSkillEffects.getInstantOpsPerTick();
        try
        {
            if (cursor.job != originalJob)
            {
                cursor.job = originalJob;
                cursor.patterns = new ArrayList<>(job.god$getTasks().keySet());
                cursor.next = 0;
            }
            int scanned = 0;
            boolean advanced = false;
            while (!cursor.patterns.isEmpty() && quota > 0 && System.nanoTime() < deadline)
            {
                IPatternDetails pattern = cursor.patterns.get(cursor.next);
                cursor.next = (cursor.next + 1) % cursor.patterns.size();
                Object progress = job.god$getTasks().get(pattern);
                // 处理样板能声明任意产出，绝不在这里执行。
                if (pattern instanceof IMolecularAssemblerSupportedPattern && progress instanceof TaskAccess task)
                {
                    long remaining = task.god$getRemaining();
                    long completed = remaining <= 0 ? 0 : execute(logic, job, task, pattern,
                            Math.min(quota, remaining), energy, service);
                    advanced |= completed > 0;
                    quota -= completed;
                    if (logic.god$job() != originalJob)
                    {
                        cursor.clear();
                        return;
                    }
                    if (task.god$getRemaining() == 0)
                    {
                        job.god$getTasks().remove(pattern);
                    }
                }
                if (++scanned == cursor.patterns.size())
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
            TickBudget.charge(now);
        }
    }

    private static long execute(CpuExecution logic, JobAccess job, TaskAccess task, IPatternDetails pattern,
                                long maximum, IEnergyService energy, CraftingService service)
    {
        boolean available = false;
        for (var provider : service.getProviders(pattern))
        {
            if (!provider.isBusy())
            {
                available = true;
                break;
            }
        }
        if (!available)
        {
            return 0;
        }
        ListCraftingInventory inventory = logic.god$inventory();
        KeyCounter outputs = new KeyCounter();
        KeyCounter containers = new KeyCounter();
        // AE2 会校验替换关系并确定这些输入真正的容器键。
        KeyCounter[] inputs = CraftingCpuHelper.extractPatternInputs(pattern, inventory,
                logic.god$level(), outputs, containers);
        if (inputs == null)
        {
            return 0;
        }
        boolean committed = false;
        try
        {
            KeyCounter consumed = new KeyCounter();
            for (KeyCounter input : inputs)
            {
                consumed.addAll(input);
            }
            long count = maximum;
            for (var input : consumed)
            {
                if (input.getLongValue() > 0)
                {
                    count = Math.min(count, 1 + logic.god$stored(input.getKey()) / input.getLongValue());
                }
            }
            KeyCounter produced = new KeyCounter();
            produced.addAll(outputs);
            produced.addAll(containers);
            for (var output : produced)
            {
                long each = output.getLongValue();
                if (each <= 0)
                {
                    continue;
                }
                // AE2 的计数器都是有符号 long，乘法/加法前先按 batch 边界钳制。
                long pending = job.god$getWaitingFor().list.get(output.getKey());
                count = Math.min(count, (Long.MAX_VALUE - Math.max(0, pending)) / each);
                count = Math.min(count, (Long.MAX_VALUE - Math.max(0, logic.god$stored(output.getKey()))) / each);
            }
            double perCraft = CraftingCpuHelper.calculatePatternPower(inputs);
            if (perCraft > 0 && count > 0)
            {
                double supplied = energy.extractAEPower(perCraft * count, Actionable.SIMULATE, PowerMultiplier.CONFIG);
                count = Math.min(count, (long) Math.floor((supplied + 0.001D) / perCraft));
            }
            if (count <= 0)
            {
                return 0;
            }
            // 没有任何外部机器接收这些材料：一批只需消耗一次原料。
            for (var input : consumed)
            {
                inventory.extract(input.getKey(), input.getLongValue() * (count - 1), Actionable.MODULATE);
            }
            energy.extractAEPower(perCraft * count, Actionable.MODULATE, PowerMultiplier.CONFIG);
            task.god$setRemaining(task.god$getRemaining() - count);
            committed = true;
            for (var remainder : containers)
            {
                logic.god$addMaxItems(remainder.getLongValue() * count, remainder.getKey().getType());
            }
            for (var output : produced)
            {
                job.god$getWaitingFor().insert(output.getKey(), output.getLongValue() * count, Actionable.MODULATE);
            }
            AEKey finalKey = logic.god$finalOutput().what();
            // 在最后一份产物完成并清空作业之前，先交还容器/副产物。
            for (var output : produced)
            {
                if (!output.getKey().equals(finalKey))
                {
                    deliver(logic, output.getKey(), output.getLongValue() * count);
                }
            }
            long finalAmount = produced.get(finalKey) * count;
            if (finalAmount > 0)
            {
                deliver(logic, finalKey, finalAmount);
            }
            logic.god$markDirty();
            return count;
        }
        finally
        {
            if (!committed)
            {
                CraftingCpuHelper.reinjectPatternInputs(inventory, inputs);
            }
        }
    }

    private static void deliver(CpuExecution logic, AEKey key, long amount)
    {
        boolean isFinal = key.matches(logic.god$finalOutput());
        long remaining = amount - logic.god$insert(key, amount, Actionable.MODULATE);
        // 独立终端下单没有请求方：原生 insert 即使接受完成也会返回 0。
        // 这些产出属于 ME 存储，不该落进「作业失败」分支。
        if (remaining > 0 && logic.god$grid() != null)
        {
            if (isFinal)
            {
                finalDeliveryDepth++;
            }
            try
            {
                remaining -= logic.god$grid().getStorageService().getInventory()
                        .insert(key, remaining, Actionable.MODULATE, logic.god$source());
            }
            finally
            {
                if (isFinal)
                {
                    finalDeliveryDepth--;
                }
            }
        }
        // 存储已满时必须保留产出，包括原生作业已经完成之后。
        if (remaining > 0)
        {
            logic.god$inventory().insert(key, remaining, Actionable.MODULATE);
        }
    }
}
