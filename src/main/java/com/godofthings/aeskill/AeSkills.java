package com.godofthings.aeskill;

import java.util.List;

/**
 * 神之AE 技能定义表。
 *
 * <p>移植自 zifeng_ae2speed_skills（作者 子枫 zifeng，CC BY-NC-SA 4.0）的
 * {@code org.zifeng.ae2skills.SkillDefinitions}，逐行对照编写：**技能 id 保留上游原样**
 * （它们是存档契约），只把加成/费用换成「无技能点、点击开关」的本模组语义；
 * 图标由上游的 {@code Items.*} 换成本模组 / AE2 的实际物品。</p>
 */
public final class AeSkills
{
    /** 装配超频（上游 {@code ae_assembler_speed}）：分子装配室每 tick 进度倍率，最大 4 级 */
    public static final String ASSEMBLER_SPEED = "ae_assembler_speed";
    /** 算力超频（上游 {@code ae_cpu_speed}）：合成 CPU 并行处理单元倍率，最大 4 级 */
    public static final String CPU_SPEED = "ae_cpu_speed";
    /** 合成之神（上游 {@code ae_assembler_infinite}）：合成台样板瞬时批量执行，最大 5 级 */
    public static final String ASSEMBLER_INFINITE = "ae_assembler_infinite";
    /** 算力之神（上游 {@code ae_cpu_infinite}）：并行 CPU 槽位 + 存储无限，最大 5 级 */
    public static final String CPU_INFINITE = "ae_cpu_infinite";
    /** 神明之力（上游 {@code ae_divine_machine_grace}）：AE 任务激活时给整条产线加速，最大 5 级 */
    public static final String DIVINE_MACHINE = "ae_divine_machine_grace";

    /** 全部 5 个技能（顺序 = 界面显示顺序） */
    public static final List<String> IDS = List.of(
            ASSEMBLER_SPEED, CPU_SPEED, ASSEMBLER_INFINITE, CPU_INFINITE, DIVINE_MACHINE);

    /** 五级倍率：索引即等级。1→×2 / 2→×8 / 3→×16 / 4→×128 / 5→×1024。 */
    private static final int[] DIVINE_MULTIPLIERS = { 1, 2, 8, 16, 128, 1024 };

    private AeSkills() {}

    public static boolean contains(String id)
    {
        return id != null && IDS.contains(id);
    }

    /** 是否属于「神明之力」（只有它走独立的倍率语义）。 */
    public static boolean isDivine(String id)
    {
        return DIVINE_MACHINE.equals(id);
    }

    /** 满级对应成就后缀；没有成就的技能返回 {@code null}。 */
    public static String achievementPath(String id)
    {
        return switch (id)
        {
            case DIVINE_MACHINE -> "divine_machine_grace";
            case ASSEMBLER_INFINITE -> "divine_assembler_grace";
            case CPU_INFINITE -> "divine_cpu_grace";
            default -> null;
        };
    }

    /** 各技能最大等级（与上游一致：两个超频 4 级，其余 5 级） */
    public static int maxLevel(String id)
    {
        return ASSEMBLER_SPEED.equals(id) || CPU_SPEED.equals(id) ? 4 : 5;
    }

    public static int divineMultiplier(int level)
    {
        return DIVINE_MULTIPLIERS[Math.clamp(level, 0, DIVINE_MULTIPLIERS.length - 1)];
    }

    /** 该技能在 {@link AeSkillEffects#update} 里的参数下标（0..4）。 */
    public static int slot(String id)
    {
        return IDS.indexOf(id);
    }
}
