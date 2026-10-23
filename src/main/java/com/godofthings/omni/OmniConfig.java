package com.godofthings.omni;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.config.ModConfigEvent.Unloading;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置（原样移植自 Omni Upgrades 的 {@code OmniConfig}）：
 * 总开关 / 顺序填满 / 对角检测 + 客户端个人黑名单。
 */
@EventBusSubscriber(modid = GodofthingsOmni.MODID)
public class OmniConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 注意：SPEC 必须在全部配置项定义【之后】才能 BUILDER.build()。
    // 反编译稿里 build() 出现在文件靠前位置（Vineflower 按字节码顺序呈现），照抄会导致
    // 四个配置项创建于 build 之后、从未挂进 spec，客户端配置加载时 .get() 直接 NPE 闪退
    // （v7.1.1 首发 crash：Cannot get config value before spec is built）。
    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER.comment(
            "总开关（默认开启）。关闭后全能升级完全不工作：批量安装、高亮预览与右键接管全部跳过，",
            "把操作交还原版或其他模组。游戏中按 Shift+O 切换，也可直接修改本文件。")
            .define("enabled", true);

    public static final ModConfigSpec.BooleanValue SEQUENTIAL_FILL_MODE = BUILDER.comment(
            "顺序填满模式（默认关闭）。开启：先把一台机器塞满，再塞下一台；",
            "关闭：在所有机器之间平均分配插件。游戏中按 Shift+N 切换。")
            .define("sequentialFillMode", false);

    public static final ModConfigSpec.BooleanValue DIAGONAL_DETECTION_MODE = BUILDER.comment(
            "对角检测模式（默认关闭）。开启：批量检测同时检查斜对角的机器（正方形范围扫描）；",
            "关闭：只检查直接相邻的机器。游戏中按 Shift+M 切换。")
            .define("diagonalDetectionMode", false);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST = BUILDER.comment(
            "物品黑名单（客户端，每个玩家独立生效）。",
            "列入此名单的物品，全能升级一律不再当升级卡处理：",
            "右键拦截与高亮预览全部跳过，把操作交还原版或其他模组。",
            "适用于继承 AE2 卡片 API 但并非可安装机器升级的功能卡",
            "（例如闪电科技的过载频道卡 'ae2lt:overloaded_frequency_card'）。",
            "每行填物品注册 id；以 '#' 开头表示屏蔽整个物品标签（例如 '#ae2:upgrade_cards'）。",
            "该名单在进入服务器时同步给服务端；修改后重启游戏生效。")
            .defineListAllowEmpty("blacklist", List.of(), () -> "", obj -> obj instanceof String);

    // 全部配置项就绪后再 build（顺序不能提前，见上方注释）。
    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean isEnabled = true;
    public static boolean isSequentialFillEnabled = false;
    public static boolean isDiagonalDetectionEnabled = false;

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent event) {
        if (!(event instanceof Unloading)) {
            if (event.getConfig().getSpec() == SPEC) {
                isEnabled = ENABLED.get();
                isSequentialFillEnabled = SEQUENTIAL_FILL_MODE.get();
                isDiagonalDetectionEnabled = DIAGONAL_DETECTION_MODE.get();
                OmniBlacklist.setLocal(BLACKLIST.get());
            }
        }
    }

    public static List<String> getBlacklistEntries() {
        return new ArrayList<>(BLACKLIST.get());
    }

    static {
        BUILDER.push("General");
        BUILDER.pop();
        BUILDER.push("BatchInstall");
        BUILDER.pop();
        BUILDER.push("Blacklist");
        BUILDER.pop();
    }
}
