package com.godofthings.mixin;

import java.math.BigInteger;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.godofthings.infinitecell.InfinityBigStorage;

/**
 * 把 AE2 {@link KeyCounter} 的 long 计数钳制在 {@link Long#MAX_VALUE}，永不溢出为负数。
 *
 * <p>思路参考 Cell4（作者 iamSpencerr，MIT）的 {@code KeyCounterMixin}，本模组按自己的
 * 「万物元件」需求重写。</p>
 *
 * <p><b>为什么必须钳制</b>：{@code KeyCounter} 内部是裸 long 加法。当某个 key 的可见数量
 * 已经顶到 {@link Long#MAX_VALUE}（9,223,372,036,854,775,807 ≈ 9.2E），再来任何正数都会
 * 回绕成负数，后果是：</p>
 * <ul>
 *   <li>AE2 终端把负数当作「没有」→ 物品从终端里消失；</li>
 *   <li>自动合成判定可用量为负 → 配方拒绝执行；</li>
 *   <li>{@code ReadableNumberConverter.format(long)} 明确拒绝负数 → 客户端崩溃。</li>
 * </ul>
 *
 * <p>真实数量由 {@link InfinityBigStorage} 用 BigInteger 单独记账，这里只保证交给 AE2
 * 的 long 永远是「非负且饱和在 MAX」。</p>
 */
@Mixin(value = KeyCounter.class, remap = false)
public class KeyCounterMixin
{
    /**
     * 拦截 {@code add(AEKey, long)}：会溢出时改为记账到 BigInteger 并饱和在 MAX。
     */
    @Inject(method = "add(Lappeng/api/stacks/AEKey;J)V", at = @At("HEAD"), cancellable = true)
    private void godofthings$addClamped(AEKey key, long amount, CallbackInfo ci)
    {
        // 只处理正数；负数走 remove()，0 是 no-op
        if (amount <= 0)
        {
            return;
        }

        @SuppressWarnings("unchecked")
        KeyCounter self = (KeyCounter) (Object) this;
        long current = self.get(key);

        // 已经饱和（或历史遗留的负数）→ 精确值转 BigInteger，long 字段钉在 MAX
        if (current == Long.MAX_VALUE || current < 0)
        {
            InfinityBigStorage.addPositive(key, amount, current);
            self.set(key, Long.MAX_VALUE);
            ci.cancel();
            return;
        }

        // 预判溢出：amount > MAX - current
        if (amount > Long.MAX_VALUE - current)
        {
            InfinityBigStorage.addPositive(key, amount, current);
            self.set(key, Long.MAX_VALUE);
            ci.cancel();
        }
        // 否则加法安全，交回原方法
    }

    /**
     * 拦截 {@code get(AEKey)}：被 BigInteger 跟踪的 key 一律返回 MAX（非负饱和值）。
     */
    @Inject(method = "get(Lappeng/api/stacks/AEKey;)J", at = @At("RETURN"), cancellable = true)
    private void godofthings$getClamped(AEKey key, CallbackInfoReturnable<Long> cir)
    {
        if (InfinityBigStorage.get(key) != null)
        {
            cir.setReturnValue(Long.MAX_VALUE);
        }
    }

    /**
     * 拦截 {@code remove(AEKey, long)}：被跟踪的 key 递减精确值，降回 long 范围时取消跟踪。
     */
    @Inject(method = "remove(Lappeng/api/stacks/AEKey;J)V", at = @At("HEAD"), cancellable = true)
    private void godofthings$removeClamped(AEKey key, long amount, CallbackInfo ci)
    {
        if (amount <= 0 || InfinityBigStorage.get(key) == null)
        {
            return;
        }

        BigInteger newValue = InfinityBigStorage.subtract(key, amount);
        if (newValue == null)
        {
            // 已降回 long 范围，跟踪条目被移除，让原方法处理 long 字段
            return;
        }

        @SuppressWarnings("unchecked")
        KeyCounter self = (KeyCounter) (Object) this;
        self.set(key, newValue.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0
                ? Long.MAX_VALUE
                : newValue.longValue());
        ci.cancel();
    }

    /**
     * 拦截 {@code set(AEKey, long)}：调用方显式写入小于 MAX 的值时，丢弃 BigInteger 跟踪
     * （以调用方写入的 long 为准）。
     */
    @Inject(method = "set(Lappeng/api/stacks/AEKey;J)V", at = @At("HEAD"))
    private void godofthings$setClamped(AEKey key, long amount, CallbackInfo ci)
    {
        if (amount < Long.MAX_VALUE)
        {
            InfinityBigStorage.remove(key);
        }
    }
}
