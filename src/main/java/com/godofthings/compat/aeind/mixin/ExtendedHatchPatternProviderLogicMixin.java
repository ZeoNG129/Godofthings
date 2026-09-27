package com.godofthings.compat.aeind.mixin;

import com.godofthings.compat.aeind.AeindPatternSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 把 Applied Industrialization「高级扩展样板输入仓」的样板槽位从 36 扩到 180（×5）。
 *
 * <p>它是**另一条继承线**（不继承 {@code HatchPatternProviderLogic}），构造器里
 * {@code super(node, host, 36)} 同样是 AE2 {@code PatternProviderLogic} 的容量参数，
 * 所以单独一个补丁。</p>
 *
 * <p><b>刻意只改这一个实参</b>：该构造器里还有一个 {@code bipush 36}（{@code new List[36]}
 * 的房间数组），用常量匹配会连带改错，所以这里用 {@link ModifyArg} 精确指向 AE2 构造器调用。</p>
 */
@Mixin(targets = "cn.autoforged.me_pattern_input_hatch_mod_1786194568.blockentity.ExtendedHatchPatternProviderLogic")
public abstract class ExtendedHatchPatternProviderLogicMixin {

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
        int expanded = original * AeindPatternSlots.EXTENDED_MULTIPLIER;
        if (!godofthings$logged) {
            godofthings$logged = true;
            AeindPatternSlots.LOGGER.info(
                    "[God of Things] Applied Industrialization 高级扩展样板输入仓：样板槽位 {} -> {}（×{}）",
                    original, expanded, AeindPatternSlots.EXTENDED_MULTIPLIER);
        }
        return expanded;
    }
}
