package com.godofthings.compass.common;

import java.lang.reflect.Method;
import java.util.Locale;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;

import org.slf4j.Logger;

/**
 * 拼音搜索兼容层（可选依赖，不硬绑）。
 *
 * <p>本模组的两个罗盘界面都有自己的搜索框。上游的过滤是朴素的
 * {@code 显示名.toLowerCase().contains(输入)}，所以「输入 zs 找不到 沼泽」这类拼音需求完全落空。</p>
 *
 * <p><b>为什么不直接把 jecharacters 写成依赖</b>：它的拼音能力来自 coremod 对
 * {@code String.contains} 等方法的字节码改写，而那些改写目标是在它的
 * {@code targets.json} 里按<b>完整类名 + 方法签名</b>硬编码的（例如上游的
 * {@code com.chaosthedude.naturescompass.gui.NaturesCompassScreen:processSearchTerm()V}）。
 * 本模组把包名改成了 {@code com.godofthings.*}，那些改写目标一个都对不上，所以装不装它
 * 我们的搜索框都不会自动变拼音。</p>
 *
 * <p><b>这里怎么做</b>：它在 {@code me.towdium.jecharacters.utils.Match} 里暴露了公开的
 * {@code public static boolean contains(String, CharSequence)}，我们直接<b>反射</b>调用它 ——
 * 装了拼音搜索就得到真正的拼音匹配（支持全拼 / 首字母 / 声调），没装就退化成普通
 * {@code contains}，两者都不需要任何编译期依赖。反射只解析一次并缓存，不在热路径上反复查找。</p>
 *
 * <p>本类只被客户端界面调用（两个 Screen 的过滤循环里），且全部方法都是纯字符串运算，
 * 不引用任何 {@code net.minecraft.client.*}，因此不需要 {@code @OnlyIn}，
 * 也不会让专用服务端因为加载它而报错。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PinyinMatch
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** jecharacters 的匹配工具类名（装了才有） */
    private static final String MATCH_CLASS = "me.towdium.jecharacters.utils.Match";
    /** 拼音搜索的模组 id */
    private static final String PINYIN_MODID = "jecharacters";

    /** 解析结果：null = 还没查过；FALSE = 查过了但没有；其它 = 可用 */
    private static Method containsMethod;
    private static boolean resolved;

    private PinyinMatch()
    {
    }

    /**
     * 装了拼音搜索模组、并且成功拿到它的匹配方法。
     *
     * <p>界面可以据此在搜索框提示里加一句「支持拼音」。</p>
     */
    public static boolean isAvailable()
    {
        resolve();
        return containsMethod != null;
    }

    /**
     * 在 {@code haystack} 里找 {@code needle}，装了拼音搜索时按拼音匹配。
     *
     * <p>语义与 {@link String#contains(CharSequence)} 一致：空 needle 恒真。
     * 任何异常都吞掉并退化成普通 contains —— 搜索框绝不能因为可选依赖出问题而失灵。</p>
     */
    public static boolean contains(String haystack, String needle)
    {
        if (haystack == null)
        {
            return needle == null || needle.isEmpty();
        }
        if (needle == null || needle.isEmpty())
        {
            return true;
        }
        resolve();
        if (containsMethod != null)
        {
            try
            {
                Object result = containsMethod.invoke(null, haystack, needle);
                if (result instanceof Boolean)
                {
                    return (Boolean) result;
                }
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                // 一次性降级：拿不到就不再尝试，避免每帧刷异常
                containsMethod = null;
                LOGGER.warn("[godofthings] 拼音搜索调用失败，已降级为普通匹配：{}", e.toString());
            }
        }
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    /** 只解析一次反射目标（延迟到第一次真正搜索时，避免拖慢启动） */
    private static void resolve()
    {
        if (resolved)
        {
            return;
        }
        resolved = true;
        if (!ModList.get().isLoaded(PINYIN_MODID))
        {
            return;
        }
        try
        {
            Class<?> matchClass = Class.forName(MATCH_CLASS);
            containsMethod = matchClass.getMethod("contains", String.class, CharSequence.class);
        }
        catch (ReflectiveOperationException | RuntimeException | LinkageError e)
        {
            containsMethod = null;
            LOGGER.warn("[godofthings] 检测到拼音搜索模组但未能接入（版本不兼容？），已降级为普通匹配：{}",
                    e.toString());
        }
    }
}
