package com.godofthings.compat.aeind.mixin;

import com.godofthings.compat.aeind.AeindPatternSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 把 Applied Industrialization「样板输入仓 / 高级样板输入仓」的样板槽位从 9 扩到 45（×5）。
 *
 * <p>目标类的构造器里唯一的槽位来源就是这一句：
 * {@code super(node, host, 9)} —— 它调用 AE2 的
 * {@code PatternProviderLogic(IManagedGridNode, PatternProviderLogicHost, int)}，
 * 第三个参数即样板库存容量。这里用 {@link ModifyArg} 只改这一个实参。</p>
 *
 * <p>覆盖范围：「高级样板输入仓」的 {@code AdvancedHatchPatternProviderLogic} 继承本类，
 * 构造器里直接 {@code super(node, host)}，因此同样走这一处 ✓。</p>
 *
 * <p>用字符串 {@code targets} 而不是类字面量：aeind 不是本模组的编译依赖，
 * 这样写不需要它出现在编译类路径上，也不影响未安装时的加载（由 {@code AeindMixinPlugin} 拦掉）。</p>
 */
@Mixin(targets = "cn.autoforged.me_pattern_input_hatch_mod_1786194568.blockentity.HatchPatternProviderLogic")
public abstract class HatchPatternProviderLogicMixin {

    private static boolean godofthings$logged;

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/helpers/patternprovider/PatternProviderLogic;"
                            + "<init>(Lappeng/api/networking/IManagedGridNode;"
                            + "Lappeng/helpers/patternprovider/PatternProviderLogicHost;I)V"),
            index = 2)
    // 必须是 static：这里改的是 super() 构造器调用的实参，那时 this 还不存在，
    // 实例方法会被 Mixin 拒绝（InvalidInjectionException: handler before super() invocation must be static）。
    private static int godofthings$multiplyPatternSlots(int original) {
        int expanded = original * AeindPatternSlots.PROVIDER_MULTIPLIER;
        if (!godofthings$logged) {
            godofthings$logged = true;
            AeindPatternSlots.LOGGER.info(
                    "[God of Things] Applied Industrialization 样板输入仓：样板槽位 {} -> {}（×{}）",
                    original, expanded, AeindPatternSlots.PROVIDER_MULTIPLIER);
        }
        return expanded;
    }
}
