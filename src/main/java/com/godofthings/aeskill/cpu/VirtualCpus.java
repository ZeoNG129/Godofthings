package com.godofthings.aeskill.cpu;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.crafting.CraftingLink;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.godofthings.aeskill.AeSkillEffects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * 真实 AE2 CPU 实例的虚拟池：每个子 CPU 独立接作业，但共享同一份物理容量。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.VirtualCpus}，
 * <b>逐行对照编写</b>（作者 子枫 zifeng，CC BY-NC-SA 4.0），只把效果层换成
 * {@link AeSkillEffects}、方法前缀换成 {@code god$}。</p>
 *
 * <p><b>为什么这么做</b>：AE2 一台合成 CPU 同时只能跑一个作业。上游用「克隆出真实的
 * {@link CraftingCPUCluster} 子实例」扩出多个槽位（不是伪造对象），这样每个子 CPU 都能走
 * AE2 原生作业流程，但存储/并行单元仍按物理簇统一记账。</p>
 */
public final class VirtualCpus
{
    private static final String CHILDREN = "god_virtual_cpus";
    private static final String BYTES = "god_reserved_bytes";

    private VirtualCpus() {}

    public static VirtualCpuState state(CraftingCPUCluster cpu)
    {
        return ((CpuExtension) (Object) cpu).god$state();
    }

    /** 算力超频对「并行处理单元数量」的唯一放大入口（原生路径与第三方适配器共用）。 */
    public static int scaleCoProcessors(int base)
    {
        return AeSkillEffects.scaledCpuCoProcessors(base);
    }

    /** 分裂算力对「合成存储容量」的唯一放大入口。 */
    public static long scaleStorage(long base)
    {
        return AeSkillEffects.scaledCpuStorage(base);
    }

    public static CraftingCPUCluster root(CraftingCPUCluster cpu)
    {
        CraftingCPUCluster parent = state(cpu).parent;
        return parent == null ? cpu : parent;
    }

    public static boolean occupied(CraftingCPUCluster cpu)
    {
        return cpu.craftingLogic.hasJob() || !cpu.craftingLogic.getInventory().list.isEmpty();
    }

    private static long reserved(CraftingCPUCluster cpu)
    {
        return occupied(cpu) ? state(cpu).reservedBytes : 0;
    }

    public static long availableStorage(CraftingCPUCluster cpu)
    {
        CraftingCPUCluster root = root(cpu);
        VirtualCpuState data = state(cpu);
        if (data.retired || (data.slot >= AeSkillEffects.getCpuSlotLimit() && !occupied(cpu)))
        {
            return 0;
        }
        long total = scaleStorage(((CpuExtension) (Object) root).god$baseStorage());
        if (total == Long.MAX_VALUE)
        {
            return Long.MAX_VALUE;
        }
        long remaining = Math.max(0, total - reserved(root));
        for (CraftingCPUCluster child : state(root).children)
        {
            remaining = Math.max(0, remaining - reserved(child));
        }
        return remaining;
    }

    public static int coProcessors(CraftingCPUCluster cpu)
    {
        int base = ((CpuExtension) (Object) root(cpu)).god$baseCoProcessors();
        return scaleCoProcessors(base);
    }

    public static boolean canSubmit(CraftingCPUCluster cpu, IGrid grid)
    {
        CraftingCPUCluster physical = root(cpu);
        VirtualCpuState data = state(cpu);
        if (data.externallyManaged)
        {
            return !cpu.isDestroyed() && cpu.getGrid() == grid && !occupied(cpu);
        }
        if (data.retired || state(physical).breaking || data.slot >= AeSkillEffects.getCpuSlotLimit()
                || cpu.isDestroyed() || cpu.getGrid() != grid || occupied(cpu))
        {
            return false;
        }
        int active = physical.isBusy() ? 1 : 0;
        for (CraftingCPUCluster child : state(physical).children)
        {
            if (child.isBusy())
            {
                active++;
            }
        }
        return active < AeSkillEffects.getCpuSlotLimit();
    }

    /** 在原生集合遍历之外调用；绝不会在作业归还物品的过程中触发。 */
    public static void refresh(Set<CraftingCPUCluster> cpus, CraftingService service)
    {
        int limit = AeSkillEffects.getCpuSlotLimit();
        ArrayList<CraftingCPUCluster> physical = new ArrayList<>();
        for (CraftingCPUCluster cpu : cpus)
        {
            if (!state(cpu).externallyManaged && state(cpu).parent == null && !cpu.isDestroyed())
            {
                physical.add(cpu);
            }
        }
        cpus.removeIf(cpu -> state(cpu).parent != null
                && (state(cpu).retired || !cpus.contains(state(cpu).parent) || state(cpu).parent.isDestroyed()));

        for (CraftingCPUCluster parent : physical)
        {
            VirtualCpuState data = state(parent);
            boolean spare = !occupied(parent);
            var iterator = data.children.iterator();
            while (iterator.hasNext())
            {
                CraftingCPUCluster child = iterator.next();
                boolean idle = !occupied(child);
                boolean remove = idle && (state(child).slot >= limit
                        || (limit == Integer.MAX_VALUE && spare));
                if (remove)
                {
                    state(child).retired = true;
                    cpus.remove(child);
                    iterator.remove();
                    parent.markDirty();
                }
                else if (idle)
                {
                    spare = true;
                }
            }
            if (limit == Integer.MAX_VALUE)
            {
                if (!spare)
                {
                    HashSet<Integer> used = new HashSet<>();
                    for (CraftingCPUCluster child : data.children)
                    {
                        used.add(state(child).slot);
                    }
                    int slot = 1;
                    while (used.contains(slot))
                    {
                        slot++;
                    }
                    data.children.add(create(parent, slot));
                    parent.markDirty();
                }
            }
            else
            {
                for (int slot = 1; slot < limit; slot++)
                {
                    boolean found = false;
                    for (CraftingCPUCluster child : data.children)
                    {
                        if (state(child).slot == slot)
                        {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                    {
                        data.children.add(create(parent, slot));
                        parent.markDirty();
                    }
                }
            }
            for (CraftingCPUCluster child : data.children)
            {
                if (cpus.add(child) && child.craftingLogic.getLastLink() instanceof CraftingLink link)
                {
                    service.addLink(link);
                }
            }
        }
    }

    private static CraftingCPUCluster create(CraftingCPUCluster parent, int slot)
    {
        CraftingCPUCluster child = new CraftingCPUCluster(parent.getBoundsMin(), parent.getBoundsMax());
        state(child).parent = parent;
        state(child).slot = slot;
        child.getConfigManager().putSetting(Settings.CPU_SELECTION_MODE, parent.getSelectionMode());
        return child;
    }

    public static void write(CraftingCPUCluster cpu, CompoundTag tag, HolderLookup.Provider registries)
    {
        if (state(cpu).failedRestore != null)
        {
            tag.merge(state(cpu).failedRestore.copy());
            return;
        }
        tag.putLong(BYTES, reserved(cpu));
        VirtualCpuState data = state(cpu);
        if (data.parent != null)
        {
            return;
        }
        ListTag children = new ListTag();
        for (CraftingCPUCluster child : data.children)
        {
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", state(child).slot);
            child.writeToNBT(entry, registries);
            children.add(entry);
        }
        tag.put(CHILDREN, children);
    }

    public static void read(CraftingCPUCluster cpu, CompoundTag tag, HolderLookup.Provider registries)
    {
        VirtualCpuState data = state(cpu);
        data.failedRestore = null;
        // 旧存档的作业没有记录 plan.bytes()：一直占满池子直到它们排空。
        data.reservedBytes = tag.contains(BYTES) ? Math.max(0, tag.getLong(BYTES))
                : occupied(cpu) ? Long.MAX_VALUE : 0;
        if (data.parent != null)
        {
            return;
        }
        for (CraftingCPUCluster old : data.children)
        {
            state(old).retired = true;
        }
        data.children.clear();
        ListTag children = tag.getList(CHILDREN, 10);
        HashSet<Integer> used = new HashSet<>();
        for (int i = 0; i < children.size(); i++)
        {
            CompoundTag entry = children.getCompound(i);
            int slot = entry.getInt("slot");
            if (slot < 1 || !used.add(slot))
            {
                throw new IllegalStateException("Invalid saved virtual CPU slot: " + slot);
            }
            CraftingCPUCluster child = create(cpu, slot);
            data.children.add(child);
            child.readFromNBT(entry, registries);
        }
    }

    /** 在物理方块实体走原生的取消/退款/掉落流程之前调用。 */
    public static void beforeBreak(CraftingCPUCluster parent)
    {
        VirtualCpuState data = state(parent);
        if (data.parent != null || data.breaking)
        {
            return;
        }
        data.breaking = true;
        for (CraftingCPUCluster child : data.children)
        {
            child.cancelJob();
            var inventory = child.craftingLogic.getInventory();
            for (var entry : inventory.list)
            {
                parent.craftingLogic.getInventory().insert(entry.getKey(), entry.getLongValue(), Actionable.MODULATE);
            }
            inventory.clear();
            state(child).retired = true;
        }
        data.children.clear();
    }
}
