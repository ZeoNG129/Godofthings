package com.godofthings.fluxcell;

import appeng.api.stacks.AEKey;
import net.neoforged.fml.ModList;

/**
 * AppFlux 的反射桥：把 FE 的 {@link AEKey} 取出来，且<b>不</b>在编译期依赖 AppFlux。
 *
 * <p><b>为什么用反射</b>：本工程 {@code libs/} 里没有 appflux 的 jar，无法编译期依赖；
 * 而 AppFlux 的 {@code EnergyType} 是内部枚举、{@code FluxKey} 是内部类，直接 import
 * 会在 AppFlux 未安装时于类加载阶段抛 {@link NoClassDefFoundError} 让整个模组崩掉。
 * 反射把这段风险收敛到本类内部，并用 {@link #isAvailable()} 做闸门。</p>
 *
 * <p>取键的路径与 AppFlux 自身一致（读源码确认）：
 * {@code com.glodblock.github.appflux.common.me.key.FluxKey.of(EnergyType.FE)}。</p>
 */
public final class FluxBridge
{
    /** AppFlux 的 modid（neoforge.mods.toml 确认）。 */
    public static final String APPFLUX_MODID = "appflux";

    private static final String FLUX_KEY_CLASS = "com.glodblock.github.appflux.common.me.key.FluxKey";
    private static final String ENERGY_TYPE_CLASS = "com.glodblock.github.appflux.common.me.key.type.EnergyType";

    /** 懒解析结果：null 表示「还没算过」，{@code FALSE} 表示「算过且不可用」。 */
    private static Boolean available;
    private static AEKey cachedFeKey;

    private FluxBridge() {}

    /** AppFlux 是否安装。 */
    public static boolean isAvailable()
    {
        Boolean cached = available;
        if (cached != null)
        {
            return cached;
        }
        boolean ok = ModList.get() != null && ModList.get().isLoaded(APPFLUX_MODID);
        if (ok)
        {
            // 再验一次类真的能解析（防止「声明了但 jar 坏了」的情况）
            try
            {
                Class.forName(FLUX_KEY_CLASS, false, FluxBridge.class.getClassLoader());
                Class.forName(ENERGY_TYPE_CLASS, false, FluxBridge.class.getClassLoader());
            }
            catch (Throwable t)
            {
                ok = false;
            }
        }
        available = ok;
        return ok;
    }

    /**
     * 取 AppFlux 的 FE 能量键（{@code FluxKey.of(EnergyType.FE)}）。
     *
     * @return FE 键；AppFlux 未安装或反射失败时为 {@code null}
     */
    public static AEKey feKey()
    {
        if (!isAvailable())
        {
            return null;
        }
        AEKey cached = cachedFeKey;
        if (cached != null)
        {
            return cached;
        }
        try
        {
            Class<?> energyTypeClass = Class.forName(ENERGY_TYPE_CLASS, true, FluxBridge.class.getClassLoader());
            // EnergyType.FE 是枚举常量
            Object fe = null;
            Object[] constants = energyTypeClass.getEnumConstants();
            if (constants != null)
            {
                for (Object constant : constants)
                {
                    if ("FE".equals(String.valueOf(constant)))
                    {
                        fe = constant;
                        break;
                    }
                }
            }
            if (fe == null)
            {
                return null;
            }

            Class<?> fluxKeyClass = Class.forName(FLUX_KEY_CLASS, true, FluxBridge.class.getClassLoader());
            Object key = fluxKeyClass.getMethod("of", energyTypeClass).invoke(null, fe);
            if (key instanceof AEKey aeKey)
            {
                cachedFeKey = aeKey;
                return aeKey;
            }
        }
        catch (Throwable t)
        {
            // 反射失败按「不可用」处理，绝不让异常蔓延到调用方
            available = Boolean.FALSE;
        }
        return null;
    }

    /** 该键是不是 AppFlux 的 FE 键。 */
    public static boolean isFluxKey(AEKey key)
    {
        AEKey fe = feKey();
        return fe != null && fe.equals(key);
    }
}
