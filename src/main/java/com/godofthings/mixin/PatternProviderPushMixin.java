package com.godofthings.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.KeyCounter;
import appeng.helpers.patternprovider.PatternProviderLogic;
import com.godofthings.aeskill.divine.DivineGrace;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 产线发现：AE2 及所有基于 {@link PatternProviderLogic} 的供应器都在这里把样板交给机器。
 *
 * <p>移植自 zifeng_ae2speed_skills 的 {@code org.zifeng.ae2skills.mixin.ae2.PatternProviderPushMixin}
 * （作者 子枫 zifeng，CC BY-NC-SA 4.0），<b>逐行对照编写</b>。
 * 本模组已有 {@link PatternProviderLogicAccessor#getHost()}，因此不再额外 {@code @Shadow}。</p>
 *
 * <p>只观察「成功推给机器」这件事，用来判定哪些供应器此刻真的在为活跃任务供货，
 * 进而得到「神明之力」的加速目标集合。</p>
 */
@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderPushMixin
{
    @Inject(method = "pushPattern", at = @At("RETURN"), require = 1)
    private void god$observeMachinePush(IPatternDetails patternDetails, KeyCounter[] inputHolder,
                                        CallbackInfoReturnable<Boolean> ci)
    {
        if (!Boolean.TRUE.equals(ci.getReturnValue()))
        {
            return;
        }
        BlockEntity provider = ((PatternProviderLogicAccessor) this).getHost().getBlockEntity();
        if (provider != null)
        {
            DivineGrace.onPush(provider);
        }
    }
}
