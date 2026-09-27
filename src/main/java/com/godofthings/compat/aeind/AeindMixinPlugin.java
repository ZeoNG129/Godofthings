package com.godofthings.compat.aeind;

import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * 让 {@code godofthings.aeind.mixins.json} 里的补丁**只在装了 Applied Industrialization 时**加载。
 *
 * <p>本配置里的 Mixin 目标（{@code cn.autoforged.me_pattern_input_hatch_mod_1786194568.*}）
 * 属于第三方模组，未安装时那些类根本不存在。若不做判断，Mixin 会对每个补丁各刷一条
 * 「target class not found」报错；用插件在这里一次性拦掉，日志干净且不影响启动。</p>
 *
 * <p><b>踩过的坑</b>：这里不能用 {@code ModList.get()} —— Mixin 配置的加载时机早于 FML 建好
 * ModList，会抛异常并导致**整份配置被丢弃**（日志表现为
 * {@code Failed to select mixin config: godofthings.aeind.mixins.json}），
 * 补丁静默失效。必须用加载期的 {@link LoadingModList}。</p>
 */
public final class AeindMixinPlugin implements IMixinConfigPlugin {

    private static boolean aeindPresent;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            aeindPresent = LoadingModList.get().getModFileById(AeindPatternSlots.AEIND_MODID) != null;
        } catch (Throwable t) {
            // 判定不了就不应用，避免在未安装时刷一堆「找不到目标类」
            aeindPresent = false;
        }
        AeindPatternSlots.LOGGER.info(
                "[God of Things] Applied Industrialization 样板槽位补丁：{}（{} {}）",
                aeindPresent ? "已启用" : "跳过（未安装该模组）",
                AeindPatternSlots.AEIND_MODID,
                aeindPresent ? "已检测到" : "未检测到");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return aeindPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                         IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                          IMixinInfo mixinInfo) {
        AeindPatternSlots.LOGGER.info(
                "[God of Things] 样板槽位补丁已应用到 {}（mixin: {}）",
                targetClassName, mixinClassName);
    }
}
