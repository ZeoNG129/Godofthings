package com.godofthings.aeskill.cpu;

import appeng.me.cluster.implementations.CraftingCPUCluster;

import java.util.ArrayList;
import java.util.List;

/**
 * 一台合成 CPU 的「虚拟 CPU」状态（真实 CPU 实例之间的父子/槽位关系）。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.VirtualCpuState}，
 * 逐字段照抄（作者 子枫 zifeng，CC BY-NC-SA 4.0）。</p>
 *
 * <p>由实体自己的 {@link CraftingCPUCluster} 持有（不放在静态 Map 里），
 * 这样跨存档、断连网络都不会互相泄漏。</p>
 */
public final class VirtualCpuState
{
    /** 父 CPU（物理簇）；为 null 表示自己就是物理簇 */
    public CraftingCPUCluster parent;
    /** 本物理簇派生出来的所有虚拟子 CPU */
    public final List<CraftingCPUCluster> children = new ArrayList<>();
    /** 本子 CPU 的槽位号（1 起；物理簇自身为 0） */
    public int slot;
    /** 本子 CPU 已占用的存储字节（用于在共享池里扣减） */
    public long reservedBytes;
    /** 已被回收，不再参与调度 */
    public boolean retired;
    /** 物理簇正在被拆解（拆方块前先把子 CPU 的作业退回去） */
    public boolean breaking;
    /** 该任务槽由别的模组接管（本模组不参与槽位与容量记账） */
    public boolean externallyManaged;
    /** 读档失败时原样保留的 NBT（避免丢玩家作业） */
    public net.minecraft.nbt.CompoundTag failedRestore;
}
