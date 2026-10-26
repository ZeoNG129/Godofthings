package com.godofthings.aeskill.cpu;

/**
 * 由 AE2 合成 CPU 簇上的 mixin 实现的扩展接口。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.crafting.CpuExtension}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），逐行照抄。</p>
 */
public interface CpuExtension
{
    VirtualCpuState god$state();

    /** 未被本模组放大的原始存储容量 */
    long god$baseStorage();

    /** 未被本模组放大的原始并行处理单元数 */
    int god$baseCoProcessors();

    /** 正在读取原始值（防止放大逻辑自我递归） */
    boolean god$readingBase();
}
