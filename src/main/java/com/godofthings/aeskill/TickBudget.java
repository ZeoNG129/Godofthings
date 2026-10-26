package com.godofthings.aeskill;

/**
 * 瞬时合成 / 无限派发 / 神明之力加速共用的「每 tick 时间预算」。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.TickBudget}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），<b>逐行照抄</b>：常量一个不动。</p>
 *
 * <p><b>为什么必须有它</b>：这几项技能都是「一个 tick 内尽可能多做」，没有节流器时
 * 一个超大合成订单能把服务端主线程卡死。这里给出两道闸：单个 tick 最多花 20ms，
 * 且任何情况下不超过 tick 开始后 45ms。</p>
 */
public final class TickBudget
{
    /** 单个服务端 tick 内共享工作切片的硬上限（纳秒）：20ms */
    private static final long HARD_SPENT_LIMIT = 20_000_000L;

    /** 即使预算还有余量，也绝不超过 tick 开始后的这个位置（45ms） */
    private static final long TICK_SPEND_WINDOW = 45_000_000L;

    private static long start, spent;

    private TickBudget() {}

    public static void markTickStart()
    {
        start = System.nanoTime();
        spent = 0;
    }

    public static long tickStartNanos()
    {
        return start;
    }

    public static void reset()
    {
        start = spent = 0;
    }

    public static long deadline(long now)
    {
        return deadline(now, 1_000_000L);
    }

    /** 同一共享池，但给显式调过的调用方更大的一片（1ms 默认，见无参重载）。 */
    public static long deadline(long now, long allowanceNanos)
    {
        long allowance = Math.min(allowanceNanos, HARD_SPENT_LIMIT - spent);
        if (start != 0)
        {
            allowance = Math.min(allowance, start + TICK_SPEND_WINDOW - now);
        }
        return now + Math.max(0, allowance);
    }

    public static void charge(long began)
    {
        spent += Math.max(0, System.nanoTime() - began);
    }
}
