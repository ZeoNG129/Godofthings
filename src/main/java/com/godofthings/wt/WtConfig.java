package com.godofthings.wt;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 无线终端配置（对应 ae2wtlib 的 {@code AE2wtlibConfig} + {@code AE2wtlibClientConfig}，
 * [本地改动] 只保留用得到的项：磁卡范围 + 选择面板常显开关）。
 *
 * <p>注意 define 必须全部在 build 之前——7.15.4 首版把 build 放在了 define 前面，
 * 配置值不在 spec 里，客户端渲染终端选择按钮时直接崩
 * 「Cannot get config value before spec is built」。</p>
 */
public final class WtConfig
{
    private WtConfig()
    {
    }

    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.ConfigValue<Double> MAGNET_CARD_RANGE = COMMON_BUILDER
            .comment("磁卡的作用范围（格）")
            .define("magnet_card_range", 16.0D);
    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();

    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.ConfigValue<Boolean> ALWAYS_SHOW_TERMINAL_SELECTOR = CLIENT_BUILDER
            .comment("始终显示终端选择面板（而不是悬停展开）")
            .define("always_show_terminal_selector", false);
    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    public static double magnetCardRange()
    {
        return MAGNET_CARD_RANGE.get();
    }

    public static boolean alwaysShowTerminalSelector()
    {
        return ALWAYS_SHOW_TERMINAL_SELECTOR.get();
    }
}
