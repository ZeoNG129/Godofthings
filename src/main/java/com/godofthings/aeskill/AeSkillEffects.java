package com.godofthings.aeskill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 神之AE 技能的效果发布层（服务端权威，执行钩子只读这里发布的原始数值）。
 *
 * <p>移植自 zifeng_ae2speed_skills（作者 子枫 zifeng，CC BY-NC-SA 4.0）的
 * {@code org.zifeng.ae2skills.Ae2Effects}，逐行对照编写，仅改包名与技能 id 前缀。
 * 自用不对外分发。</p>
 *
 * <p><b>语义（不得擅自改）</b>：多玩家各开各的技能时，按<b>每个技能取全体最高等级</b>
 * 发布到静态字段——因为 AE2 的合成 CPU 是区块级共享实体，无法绑定到单个玩家。
 * 这样同一台机器不会因为「谁在线」而算出两个不同的数字。</p>
 */
public final class AeSkillEffects
{
    /** 玩家 UUID → 五项技能等级 {装配超频, 算力超频, 合成之神, 算力之神, 神明之力} */
    private static final Map<UUID, int[]> PLAYERS = new HashMap<>();
    /** 对外发布的五项等级（索引与上面的数组一致） */
    private static final int[] LEVELS = new int[5];

    private static volatile int assemblerSpeed, cpuSpeed, assemblerInfinite, cpuInfinite, divine;

    private AeSkillEffects() {}

    // ------------------------------------------------------------------ 读取（执行钩子用）

    public static double getAssemblerSpeedMultiplier()
    {
        return 1 + assemblerSpeed;
    }

    public static double getCpuSpeedMultiplier()
    {
        return 1 + cpuSpeed;
    }

    public static int getAssemblerSpeedLevel()
    {
        return assemblerSpeed;
    }

    public static int getCpuSpeedLevel()
    {
        return cpuSpeed;
    }

    public static int getAssemblerInfiniteLevel()
    {
        return assemblerInfinite;
    }

    public static int getCpuInfiniteLevel()
    {
        return cpuInfinite;
    }

    public static int getDivineLevel()
    {
        return divine;
    }

    /** 无人开启时返回 1；否则返回该等级对应的倍率。 */
    public static int getDivineMultiplier()
    {
        return AeSkills.divineMultiplier(divine);
    }

    /** 合成之神：每 tick 可执行的合成台样板次数上限（5 级 = 无上限）。 */
    public static int getInstantOpsPerTick()
    {
        return assemblerInfinite == 0 ? 0
                : assemblerInfinite == 5 ? Integer.MAX_VALUE : 1 << assemblerInfinite;
    }

    /** 算力之神：并行 CPU 槽位上限（5 级 = 无上限）。 */
    public static int getCpuSlotLimit()
    {
        return cpuInfinite == 5 ? Integer.MAX_VALUE : 1 << cpuInfinite;
    }

    /** 并行处理单元钳制上限（防止倍率叠成天文数字撞 int 溢出）。 */
    public static int getCpuParallelCap()
    {
        return 4096;
    }

    /**
     * 算力超频对「并行处理单元数量」的唯一放大入口。
     *
     * <p>只放大加成部分、绝不把第三方 CPU 的原生性能往下降（{@code Math.max(base, ...)}）。</p>
     */
    public static int scaledCpuCoProcessors(int base)
    {
        if (cpuInfinite == 5)
        {
            return Integer.MAX_VALUE - 1;
        }
        if (cpuSpeed == 0)
        {
            return base;
        }
        long scaled = (base + 1L) * (1L + cpuSpeed) - 1;
        return (int) Math.max(base, Math.min(getCpuParallelCap(), Math.max(0, scaled)));
    }

    /** 分裂算力对「合成存储容量」的唯一放大入口。 */
    public static long scaledCpuStorage(long base)
    {
        if (cpuInfinite == 5)
        {
            return Long.MAX_VALUE;
        }
        int factor = 1 << cpuInfinite;
        return base > Long.MAX_VALUE / factor ? Long.MAX_VALUE : base * factor;
    }

    public static int[] snapshot()
    {
        return LEVELS.clone();
    }

    // ------------------------------------------------------------------ 写入（服务端技能数据变更时调用）

    public static void update(UUID player, int assemblerSpeedLevel, int cpuSpeedLevel,
                              int assemblerInfiniteLevel, int cpuInfiniteLevel, int divineLevel)
    {
        int a = Math.clamp(assemblerSpeedLevel, 0, 4);
        int c = Math.clamp(cpuSpeedLevel, 0, 4);
        int ai = Math.clamp(assemblerInfiniteLevel, 0, 5);
        int ci = Math.clamp(cpuInfiniteLevel, 0, 5);
        int dv = Math.clamp(divineLevel, 0, 5);

        int[] old = PLAYERS.get(player);
        if (a == 0 && c == 0 && ai == 0 && ci == 0 && dv == 0)
        {
            remove(player);
            return;
        }
        if (old != null && old[0] == a && old[1] == c && old[2] == ai && old[3] == ci && old[4] == dv)
        {
            return;
        }
        PLAYERS.put(player, new int[] { a, c, ai, ci, dv });
        refresh();
    }

    public static void remove(UUID player)
    {
        if (PLAYERS.remove(player) != null)
        {
            refresh();
        }
    }

    public static void clear()
    {
        PLAYERS.clear();
        refresh();
    }

    /** 重算五项发布值：每项取全体玩家的最高等级（不变则直接返回，避免无谓写入）。 */
    private static void refresh()
    {
        int a = 0, c = 0, ai = 0, ci = 0, dv = 0;
        for (int[] levels : PLAYERS.values())
        {
            a = Math.max(a, levels[0]);
            c = Math.max(c, levels[1]);
            ai = Math.max(ai, levels[2]);
            ci = Math.max(ci, levels[3]);
            dv = Math.max(dv, levels[4]);
        }
        if (a == assemblerSpeed && c == cpuSpeed && ai == assemblerInfinite
                && ci == cpuInfinite && dv == divine)
        {
            return;
        }
        assemblerSpeed = LEVELS[0] = a;
        cpuSpeed = LEVELS[1] = c;
        assemblerInfinite = LEVELS[2] = ai;
        cpuInfinite = LEVELS[3] = ci;
        divine = LEVELS[4] = dv;
    }
}
