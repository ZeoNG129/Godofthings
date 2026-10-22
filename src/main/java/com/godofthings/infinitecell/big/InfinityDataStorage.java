package com.godofthings.infinitecell.big;

import java.math.BigInteger;

import javax.annotation.Nullable;

import appeng.api.stacks.AEKey;
import appeng.core.AELog;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * 单个磁盘的实际内容（key → 数量），支持超出 {@code long} 范围的精确计数。
 *
 * <p>移植自 ExtendedAE+ 1.6.3 的 {@code com.extendedae_plus.util.storage.InfinityDataStorage}
 * （作者 glodblock 等，LGPL-3.0），算法与存档格式保持不变。</p>
 *
 * <p>设计要点：普通数量用原生 {@code long} 保存（高频路径零 BigInteger 开销），
 * 只有单项超过 {@link Long#MAX_VALUE} 时才搬到 {@link #bigAmounts}；
 * 总数同理，未溢出前只用 {@code longItemCount}，溢出后才建 {@code bigItemCount} 基值。</p>
 */
public class InfinityDataStorage
{
    /** 占位 / 默认值 */
    public static final InfinityDataStorage EMPTY = new InfinityDataStorage();

    private static final BigInteger BI_LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final int LONG_MAX_BIT_LENGTH = Long.SIZE - 1;

    /** 普通数量（≤ long 上限） */
    public final Object2LongMap<AEKey> longAmounts;
    /** 超过 long 范围的数量 */
    public final Object2ObjectMap<AEKey, BigInteger> bigAmounts;

    /** 总数未溢出时的精确值；溢出后是尚未并入大数基值的增量 */
    private long longItemCount;
    /** 总数溢出后的大数基值，与 {@link #longItemCount} 相加才是精确总数 */
    @Nullable
    private BigInteger bigItemCount;
    /** 大数基值与 long 增量合并后的读取缓存 */
    @Nullable
    private BigInteger cachedItemCount;

    public InfinityDataStorage()
    {
        this(new Object2LongOpenHashMap<>(), new Object2ObjectOpenHashMap<>(), BigInteger.ZERO);
    }

    private InfinityDataStorage(Object2LongMap<AEKey> longAmounts,
            Object2ObjectMap<AEKey, BigInteger> bigAmounts,
            BigInteger itemCount)
    {
        this.longAmounts = longAmounts;
        this.bigAmounts = bigAmounts;
        this.setItemCount(itemCount);
    }

    /** 精确总数（只有存在未合并增量时才做一次 BigInteger 加法） */
    public BigInteger getItemCount()
    {
        BigInteger cached = this.cachedItemCount;
        if (cached != null)
        {
            return cached;
        }

        BigInteger itemCount;
        if (this.bigItemCount == null)
        {
            itemCount = BigInteger.valueOf(this.longItemCount);
        }
        else if (this.longItemCount == 0)
        {
            itemCount = this.bigItemCount;
        }
        else
        {
            itemCount = this.bigItemCount.add(BigInteger.valueOf(this.longItemCount));
            // 总数重新落回 long 范围时立即降级，避免后续增量逼近 long 边界
            if (itemCount.bitLength() <= LONG_MAX_BIT_LENGTH)
            {
                this.setItemCount(itemCount);
            }
        }

        this.cachedItemCount = itemCount;
        return itemCount;
    }

    /** 是否还有内容（所有条目都为正数，直接看 Map 即可） */
    public boolean hasItems()
    {
        return !this.longAmounts.isEmpty() || !this.bigAmounts.isEmpty();
    }

    /** 种类数 */
    public int size()
    {
        return this.longAmounts.size() + this.bigAmounts.size();
    }

    /** 增加数量（普通路径完全不碰 BigInteger） */
    public void insert(AEKey key, long amount)
    {
        if (amount <= 0)
        {
            return;
        }

        long currentAmount = this.longAmounts.getLong(key);
        if (currentAmount > 0)
        {
            if (amount <= Long.MAX_VALUE - currentAmount)
            {
                this.longAmounts.put(key, currentAmount + amount);
            }
            else
            {
                this.longAmounts.removeLong(key);
                this.bigAmounts.put(key, BigInteger.valueOf(currentAmount).add(BigInteger.valueOf(amount)));
            }
        }
        else
        {
            BigInteger currentBigAmount = this.bigAmounts.get(key);
            if (currentBigAmount == null)
            {
                this.longAmounts.put(key, amount);
            }
            else
            {
                this.bigAmounts.put(key, currentBigAmount.add(BigInteger.valueOf(amount)));
            }
        }

        this.addToItemCount(amount);
    }

    /** 模拟提取（只读，不做 BigInteger 运算也不改状态） */
    public long getExtractableAmount(AEKey key, long amount)
    {
        if (amount <= 0)
        {
            return 0;
        }

        long currentAmount = this.longAmounts.getLong(key);
        if (currentAmount > 0)
        {
            return Math.min(currentAmount, amount);
        }

        // bigAmounts 里的一定超过 Long.MAX_VALUE，任意正 long 请求都满足
        BigInteger currentBigAmount = this.bigAmounts.get(key);
        return currentBigAmount != null && currentBigAmount.signum() > 0 ? amount : 0;
    }

    /** 提取（模拟走只读路径，实际操作才改 Map 与总数） */
    public long extract(AEKey key, long amount, boolean modulate)
    {
        if (!modulate)
        {
            return this.getExtractableAmount(key, amount);
        }
        if (amount <= 0)
        {
            return 0;
        }

        long currentAmount = this.longAmounts.getLong(key);
        if (currentAmount > 0)
        {
            long extractedAmount = Math.min(currentAmount, amount);
            if (extractedAmount == currentAmount)
            {
                this.longAmounts.removeLong(key);
            }
            else
            {
                this.longAmounts.put(key, currentAmount - extractedAmount);
            }
            this.subtractFromItemCount(extractedAmount);
            return extractedAmount;
        }

        BigInteger currentBigAmount = this.bigAmounts.get(key);
        if (currentBigAmount == null || currentBigAmount.signum() <= 0)
        {
            return 0;
        }

        // 正常情况下 bigAmounts 里的量一定超过 Long.MAX_VALUE，
        // 只有被提取回 long 范围时才搬回 long Map
        BigInteger removedAmount = amount == Long.MAX_VALUE
                ? BI_LONG_MAX
                : BigInteger.valueOf(amount);
        BigInteger remainingAmount = currentBigAmount.subtract(removedAmount);
        if (remainingAmount.signum() <= 0)
        {
            this.bigAmounts.remove(key);
        }
        else if (remainingAmount.bitLength() <= LONG_MAX_BIT_LENGTH)
        {
            this.bigAmounts.remove(key);
            this.longAmounts.put(key, remainingAmount.longValue());
        }
        else
        {
            this.bigAmounts.put(key, remainingAmount);
        }
        this.subtractFromItemCount(amount);
        return amount;
    }

    /** 序列化为 NBT（保持旧版字段结构，便于与上游存档互通） */
    public CompoundTag serializeNBT(HolderLookup.Provider registries)
    {
        CompoundTag nbt = new CompoundTag();
        ListTag keys = new ListTag();
        ListTag amountsTag = new ListTag();

        for (var entry : Object2LongMaps.fastIterable(this.longAmounts))
        {
            long amount = entry.getLongValue();
            if (amount <= 0)
            {
                continue;
            }

            keys.add(entry.getKey().toTagGeneric(registries));
            CompoundTag amountTag = new CompoundTag();
            amountTag.putByteArray("value", encodePositiveLong(amount));
            amountsTag.add(amountTag);
        }

        for (var entry : Object2ObjectMaps.fastIterable(this.bigAmounts))
        {
            BigInteger amount = entry.getValue();
            if (amount == null || amount.signum() <= 0)
            {
                continue;
            }

            keys.add(entry.getKey().toTagGeneric(registries));
            CompoundTag amountTag = new CompoundTag();
            amountTag.putByteArray("value", amount.toByteArray());
            amountsTag.add(amountTag);
        }

        nbt.put(InfinityConstants.INFINITY_CELL_KEYS, keys);
        nbt.put(InfinityConstants.INFINITY_CELL_AMOUNTS, amountsTag);
        nbt.putByteArray(InfinityConstants.INFINITY_CELL_ITEM_COUNT, this.getItemCount().toByteArray());
        return nbt;
    }

    /** 从 NBT 反序列化（兼容旧版列表式存档结构） */
    public static InfinityDataStorage loadFromNBT(CompoundTag nbt, HolderLookup.Provider registries)
    {
        ListTag keys = nbt.getList(InfinityConstants.INFINITY_CELL_KEYS, ListTag.TAG_COMPOUND);
        ListTag amounts = nbt.getList(InfinityConstants.INFINITY_CELL_AMOUNTS, ListTag.TAG_COMPOUND);
        if (keys.size() != amounts.size())
        {
            AELog.warn("Loading storage cell with mismatched amounts/tags: %d != %d", amounts.size(), keys.size());
        }

        int limit = Math.min(keys.size(), amounts.size());
        Object2LongMap<AEKey> storedLongAmounts = new Object2LongOpenHashMap<>(Math.max(2, limit));
        // 存档以 long 数量为主，大数 Map 按需扩容
        Object2ObjectMap<AEKey, BigInteger> storedBigAmounts = new Object2ObjectOpenHashMap<>();
        for (int i = 0; i < limit; i++)
        {
            AEKey key = AEKey.fromTagGeneric(registries, keys.getCompound(i));
            BigInteger amount = new BigInteger(amounts.getCompound(i).getByteArray("value"));
            if (key == null || amount.signum() <= 0)
            {
                continue;
            }

            storedBigAmounts.remove(key);
            storedLongAmounts.removeLong(key);

            if (amount.bitLength() <= LONG_MAX_BIT_LENGTH)
            {
                storedLongAmounts.put(key, amount.longValue());
            }
            else
            {
                storedBigAmounts.put(key, amount);
            }
        }

        return new InfinityDataStorage(storedLongAmounts, storedBigAmounts,
                calculateItemCount(storedLongAmounts, storedBigAmounts));
    }

    /**
     * 按两个数量 Map 重新算总数。
     *
     * <p>普通数量优先用 long 累加，只在总数将超 {@link Long#MAX_VALUE} 时才建 BigInteger；
     * 传入的 Map 已是最终状态，所以这里算的是各 key 当前数量之和，
     * 不使用存档里可能过期的总数摘要。</p>
     */
    private static BigInteger calculateItemCount(Object2LongMap<AEKey> longAmounts,
            Object2ObjectMap<AEKey, BigInteger> bigAmounts)
    {
        long longItemCount = 0;
        // null 表示总数仍可完全用 long 表示
        BigInteger bigItemCount = null;

        for (var entry : Object2LongMaps.fastIterable(longAmounts))
        {
            long amount = entry.getLongValue();
            if (amount <= 0)
            {
                continue;
            }

            // 用减法判溢出，避免先加再检查
            if (bigItemCount == null && amount <= Long.MAX_VALUE - longItemCount)
            {
                longItemCount += amount;
            }
            else
            {
                if (bigItemCount == null)
                {
                    bigItemCount = BigInteger.valueOf(longItemCount);
                }
                bigItemCount = bigItemCount.add(BigInteger.valueOf(amount));
                longItemCount = 0;
            }
        }

        for (var entry : Object2ObjectMaps.fastIterable(bigAmounts))
        {
            BigInteger amount = entry.getValue();
            if (amount == null || amount.signum() <= 0)
            {
                continue;
            }

            if (bigItemCount == null)
            {
                bigItemCount = BigInteger.valueOf(longItemCount);
                longItemCount = 0;
            }
            bigItemCount = bigItemCount.add(amount);
        }

        return bigItemCount == null ? BigInteger.valueOf(longItemCount) : bigItemCount;
    }

    /**
     * 把正 long 编码成与 {@link BigInteger#toByteArray()} 相同的最短有符号大端字节数组。
     *
     * <p>加载端用 {@code new BigInteger(byte[])} 解码，所以最高有效位为 1 时必须补一个 0
     * 符号字节，否则正数会被解成负数。这样既省掉临时 BigInteger 对象，又保持存档格式兼容。</p>
     */
    private static byte[] encodePositiveLong(long value)
    {
        int byteCount = (64 - Long.numberOfLeadingZeros(value) + 7) >>> 3;
        if (byteCount == 0)
        {
            byteCount = 1;
        }

        if ((value & (1L << (byteCount * 8 - 1))) != 0)
        {
            byteCount++;
        }

        byte[] encoded = new byte[byteCount];
        for (int index = byteCount - 1; index >= 0; index--)
        {
            encoded[index] = (byte) value;
            value >>>= 8;
        }
        return encoded;
    }

    /** 把精确总数拆成 long / BigInteger 保存 */
    private void setItemCount(BigInteger itemCount)
    {
        this.cachedItemCount = null;
        if (itemCount == null || itemCount.signum() <= 0)
        {
            this.longItemCount = 0;
            this.bigItemCount = null;
        }
        else if (itemCount.bitLength() <= LONG_MAX_BIT_LENGTH)
        {
            this.longItemCount = itemCount.longValue();
            this.bigItemCount = null;
        }
        else
        {
            this.longItemCount = 0;
            this.bigItemCount = itemCount;
        }
    }

    /** 增加总数，尽量留在 long 快速路径里 */
    private void addToItemCount(long amount)
    {
        this.cachedItemCount = null;
        if (this.bigItemCount != null)
        {
            // 已有大数基值时，增量先暂存在 long 里
            if (this.longItemCount <= Long.MAX_VALUE - amount)
            {
                this.longItemCount += amount;
            }
            else
            {
                this.bigItemCount = this.bigItemCount
                        .add(BigInteger.valueOf(this.longItemCount))
                        .add(BigInteger.valueOf(amount));
                this.longItemCount = 0;
            }
        }
        else if (amount <= Long.MAX_VALUE - this.longItemCount)
        {
            this.longItemCount += amount;
        }
        else
        {
            this.bigItemCount = BigInteger.valueOf(this.longItemCount).add(BigInteger.valueOf(amount));
            this.longItemCount = 0;
        }
    }

    /** 减少总数，回落到 long 范围时自动降级 */
    private void subtractFromItemCount(long amount)
    {
        this.cachedItemCount = null;
        if (this.bigItemCount != null)
        {
            // long 增量允许暂时为负，表示从大数基值里扣的量
            if (this.longItemCount >= Long.MIN_VALUE + amount)
            {
                this.longItemCount -= amount;
            }
            else
            {
                this.setItemCount(this.bigItemCount
                        .add(BigInteger.valueOf(this.longItemCount))
                        .subtract(BigInteger.valueOf(amount)));
            }
        }
        else
        {
            this.longItemCount -= amount;
        }
    }
}
