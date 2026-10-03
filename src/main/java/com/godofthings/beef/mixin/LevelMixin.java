package com.godofthings.beef.mixin;

import com.godofthings.beef.world.dimension.UselessDimensions;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「无用维度永远晴天」的等级层注入。
 *
 * <p><b>死代码清理</b>：本类原先还承载玩家保护层的两条注入
 * （{@code godofthings$filterBeefProtectedPlayers} —— 把「高级隐身」玩家从
 * {@code Level#getEntities} 的查询结果里过滤掉）。随着玩家保护层（无敌 / 高级隐身）
 * 整套删除，那两条注入与其私有辅助方法一并移除。</p>
 *
 * <p><b>本类因此保留、未随其余 7 个保护 Mixin 一起删除</b>：下面这三个注入属于
 * 「无用维度」子系统（{@code UselessDimensions} 的 uselessdim/2/3 三个维度），
 * 与玩家保护无关，删掉会让无用维度重新出现昼夜与雷雨。</p>
 *
 * <p>注意 {@code godofthings$isUselessDimension()} 只认 UselessDimensions 里那三个维度键，
 * 不会误伤本模组自己的超平坦 / 虚空维度。</p>
 */
@Mixin(value = Level.class)
public class LevelMixin {

    @Inject(
            method = "isDay",
            at = @At("HEAD"),
            cancellable = true
    )
    private void injectIsDay(CallbackInfoReturnable<Boolean> cir) {
        if (this.godofthings$isUselessDimension()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "isRaining",
            at = @At("HEAD"),
            cancellable = true
    )
    private void uselessDimAlwaysClear_rain(CallbackInfoReturnable<Boolean> cir) {
        if (this.godofthings$isUselessDimension()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "isThundering",
            at = @At("HEAD"),
            cancellable = true
    )
    private void uselessDimAlwaysClear_thunder(CallbackInfoReturnable<Boolean> cir) {
        if (this.godofthings$isUselessDimension()) {
            cir.setReturnValue(false);
        }
    }

    private boolean godofthings$isUselessDimension() {
        Level level = (Level) (Object) this;
        return UselessDimensions.isUselessDimension(level.dimension());
    }
}
