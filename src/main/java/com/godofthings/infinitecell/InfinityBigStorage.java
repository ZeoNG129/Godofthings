package com.godofthings.infinitecell;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import appeng.api.stacks.AEKey;

/**
 * 记录「超过 {@link Long#MAX_VALUE} 之后」的真实数量（BigInteger 级）。
 *
 * <p>思路参考 Cell4（作者 iamSpencerr，MIT）的 {@code Cell4BigStorage}，本模组重写。</p>
 *
 * <p><b>为什么需要这一层</b>：AE2 的 {@code KeyCounter} 只有 long。当某个 key 的可见数量
 * 饱和在 {@link Long#MAX_VALUE}（9.2E）后继续累加，{@code KeyCounterMixin} 会把 long 字段
 * 钉在 MAX 不变，同时把「真实值」记在这里，于是：</p>
 * <ul>
 *   <li>AE2 侧永远看到非负的 {@code Long.MAX_VALUE}，界面显示 9.2E，不会溢出、不会崩；</li>
 *   <li>本模组自己读写时能拿到精确的 BigInteger，超过 9.2E 的部分一样不会丢。</li>
 * </ul>
 *
 * <p>用 {@link AEKey} 作 key（它的 equals/hashCode 基于内容，物品/流体/化学品同一口径），
 * 条目跨 {@code KeyCounter} 实例保留 —— AE2 每次查询网络都会新建 counter，靠这一点才不会丢账。</p>
 */
public final class InfinityBigStorage
{
    private static final ConcurrentMap<AEKey, BigInteger> BIG_AMOUNTS = new ConcurrentHashMap<>();

    private InfinityBigStorage()
    {
    }

    /** key 的真实 BigInteger 值；从未溢出过则返回 null */
    public static BigInteger get(AEKey key)
    {
        return BIG_AMOUNTS.get(key);
    }

    /**
     * 给 key 的真实值加正数。
     *
     * @param key      目标 key
     * @param amount   要加的非负数量
     * @param baseline 当前 long 计数（首次溢出时用它作 BigInteger 初值）
     * @return 加完后的新值
     */
    public static BigInteger addPositive(AEKey key, long amount, long baseline)
    {
        return BIG_AMOUNTS.compute(key, (k, existing) ->
        {
            BigInteger base = existing;
            if (base == null)
            {
                base = BigInteger.valueOf(baseline);
            }
            return base.add(BigInteger.valueOf(amount));
        });
    }

    /**
     * 从 key 的真实值里减正数。结果回落到 {@link Long#MAX_VALUE} 以下时移除条目
     * （之后读取会回退到 long 计数）。
     *
     * @return 减完后的新值；若条目被移除（已能放进 long）则返回 null
     */
    public static BigInteger subtract(AEKey key, long amount)
    {
        BigInteger[] holder = new BigInteger[1];
        BIG_AMOUNTS.compute(key, (k, existing) ->
        {
            if (existing == null)
            {
                return null;
            }
            BigInteger newValue = existing.subtract(BigInteger.valueOf(amount));
            holder[0] = newValue;
            if (newValue.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) < 0)
            {
                return null;
            }
            return newValue;
        });
        return holder[0];
    }

    /** 直接设置真实值（超过 MAX 才跟踪，否则移除） */
    public static void set(AEKey key, BigInteger value)
    {
        if (value.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0)
        {
            BIG_AMOUNTS.put(key, value);
        }
        else
        {
            BIG_AMOUNTS.remove(key);
        }
    }

    /** 停止跟踪某个 key */
    public static void remove(AEKey key)
    {
        BIG_AMOUNTS.remove(key);
    }

    /** key 的有效 long 值（真实值超过 MAX 时饱和为 MAX） */
    public static long getEffectiveLong(AEKey key, long fallback)
    {
        BigInteger big = BIG_AMOUNTS.get(key);
        if (big == null)
        {
            return fallback;
        }
        if (big.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0)
        {
            return Long.MAX_VALUE;
        }
        return big.longValue();
    }

    /** key 是否正在被跟踪（真实值超过 MAX） */
    public static boolean isTracked(AEKey key)
    {
        return BIG_AMOUNTS.containsKey(key);
    }

    /** 清空所有跟踪条目（测试 / 世界卸载用） */
    public static void clear()
    {
        BIG_AMOUNTS.clear();
    }
}
