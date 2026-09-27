package com.godofthings.beef.compat.jei;

import com.godofthings.beef.UselessMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JEI 运行时持有者。
 *
 * <p>上游 {@code com.sorrowmist.useless.compat.jei.JEIPlugin}（321 行）里与造化杖有关的
 * <b>只有</b>这个 {@link IJeiRuntime} 持有器：连锁等价组界面
 * （{@code ChainGroupScreen}）要靠它在 JEI 里查物品。
 * 上游该类的其余内容（万象合金炉配方类别、催化剂信息页、全能样板转移）全部属于机器子系统，
 * 未随本次照抄带入，因此这里只保留运行时持有这一部分。</p>
 *
 * <p>本类自带 {@link JeiPlugin} 注解，是独立于 {@code com.godofthings.jei.GodJeiPlugin}
 * 的另一个 JEI 插件：JEI 允许同一模组注册多个插件（按 pluginUid 区分）。</p>
 */
@JeiPlugin
public final class JEIPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(UselessMod.MODID, "beef_tool_runtime");

    @Nullable
    private static IJeiRuntime runtime;

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    /** 供连锁等价组界面查询 JEI 物品列表；JEI 缺席时为 {@code null}。 */
    @Nullable
    public static IJeiRuntime getRuntime() {
        return runtime;
    }
}
