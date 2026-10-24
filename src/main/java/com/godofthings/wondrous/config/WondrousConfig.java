package com.godofthings.wondrous.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 荒辰移晷之杖的服务端 / 客户端配置（照抄上游 StretcherConfig 中杖用到的条目）。
 * [本地改动] 上游一个 config 类服务整个 Useless Stretcher 模组；本地只保留杖相关的
 * 配置项与读取器，文件名加 wondrous- 前缀避免与项目其它配置冲突。
 */
public final class WondrousConfig
{
    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue SERVER_STAFF_ACCELERATION = SERVER_BUILDER
            .comment("是否允许杖的时间加速功能（服务器总开关）。")
            .translation("godofthings.wondrous.configuration.server.staff_acceleration")
            .define("staff_acceleration", true);
    private static final ModConfigSpec.BooleanValue SERVER_IDLE_THROTTLE = SERVER_BUILDER
            .comment("永久加速时是否对空闲机器自动降频以省性能（推荐开启）。")
            .translation("godofthings.wondrous.configuration.acceleration.idle_throttle")
            .define("idle_throttle", true);
    private static final ModConfigSpec.BooleanValue SERVER_ENTITY_DISABLE_AI = SERVER_BUILDER
            .comment("实体加速时是否关闭目标 Mob 的 AI（完整 tick 模式）。")
            .translation("godofthings.wondrous.configuration.acceleration.entity_disable_ai")
            .define("entity_disable_ai", true);
    private static final ModConfigSpec.BooleanValue SERVER_STAFF_SUMMON = SERVER_BUILDER
            .comment("是否允许杖的召唤目录和召唤功能。[本地改动] 上游默认关闭，本模组按需求默认开启。")
            .translation("godofthings.wondrous.configuration.summoning.enable")
            .define("enable", true);
    private static final ModConfigSpec.BooleanValue SERVER_STAFF_LOOT_REFRESH = SERVER_BUILDER
            .comment("是否允许杖刷新战利品箱、其它原版战利品容器、容器矿车和 Lootr 容器。[本地改动] 上游默认关闭，本模组按需求默认开启。")
            .translation("godofthings.wondrous.configuration.loot_refresh.enable")
            .define("enable", true);
    private static final ModConfigSpec.BooleanValue SERVER_TIME_STOP = SERVER_BUILDER
            .comment("是否允许杖使用一次性时间暂停。每次启动固定暂停 12 秒，结束后自动恢复。")
            .translation("godofthings.wondrous.configuration.server.time_stop")
            .define("time_stop", true);
    private static final ModConfigSpec.BooleanValue SERVER_RANGE_ACCELERATION = SERVER_BUILDER
            .comment("是否允许杖的范围加速场（范围放置/标记模式）。")
            .translation("godofthings.wondrous.configuration.server.range_acceleration")
            .define("range_acceleration", true);
    private static final ModConfigSpec.BooleanValue SERVER_REMOTE_RECLAIMER = SERVER_BUILDER
            .comment("管理员回收模式是否允许回收其他玩家的加速场。")
            .translation("godofthings.wondrous.configuration.server.remote_reclaimer")
            .define("remote_reclaimer", true);
    private static final ModConfigSpec.BooleanValue SERVER_STAFF_LEAF_DROP = SERVER_BUILDER
            .comment("手持杖破坏树叶时是否有概率掉落奖励（红石/经验等）。")
            .translation("godofthings.wondrous.configuration.staff_leaf_drop.enable")
            .define("enable", true);
    private static final ModConfigSpec.DoubleValue SERVER_STAFF_LEAF_DROP_PROBABILITY = SERVER_BUILDER
            .comment("杖破坏树叶的奖励掉落概率（0-1）。")
            .translation("godofthings.wondrous.configuration.staff_leaf_drop.probability")
            .defineInRange("probability", 0.02D, 0.0D, 1.0D);
    private static final ModConfigSpec.BooleanValue SERVER_DOLL_LEAF_DROP = SERVER_BUILDER
            .comment("紬紬玩偶破坏树叶时是否有概率掉落奖励。[本地改动] 本模组玩偶对应上游 TwoI。")
            .translation("godofthings.wondrous.configuration.doll_leaf_drop.enable")
            .define("enable", true);
    private static final ModConfigSpec.DoubleValue SERVER_DOLL_LEAF_DROP_PROBABILITY = SERVER_BUILDER
            .comment("玩偶破坏树叶的奖励掉落概率（0-1）。")
            .translation("godofthings.wondrous.configuration.doll_leaf_drop.probability")
            .defineInRange("probability", 0.01D, 0.0D, 1.0D);
    private static final ModConfigSpec.BooleanValue SERVER_APOTHEOSIS_COMPAT = SERVER_BUILDER
            .comment("是否启用 APOC 附魔台兼容（选择台/加成注入）。未装 Apotheosis 附魔时无效。",
                    "此条目只在检测到 Apotheosis 附魔注册类型时才注册（hasApotheosisConfig 对应）。")
            .translation("godofthings.wondrous.configuration.server.apotheosis_compat")
            .define("apotheosis_compat", true);

    private static final ModConfigSpec.BooleanValue CLIENT_HIGHLIGHT_SEE_THROUGH = CLIENT_BUILDER
            .comment("加速目标的高亮框是否允许穿墙显示。")
            .translation("godofthings.wondrous.configuration.client.highlight_see_through")
            .define("highlight_see_through", true);

    /** 上游只在装了 Apotheosis 附魔类型时注册此条目；本地保持同一语义。 */
    private static ModConfigSpec.BooleanValue apotheosisCompatValue;
    private static ModConfigSpec SERVER_SPEC;
    private static ModConfigSpec CLIENT_SPEC;

    private WondrousConfig()
    {
    }

    /** 由 Godofthings 构造器调用：装了 Apothic Enchanting 才注册 apotheosis_compat 条目。 */
    public static void register(ModContainer container)
    {
        if (net.neoforged.fml.ModList.get().isLoaded("apotioticenchanting")
                || net.neoforged.fml.ModList.get().isLoaded("apotheosis"))
        {
            SERVER_BUILDER
                    .comment("是否启用 APOC 附魔台兼容（选择台/加成注入）。")
                    .translation("godofthings.wondrous.configuration.server.apotheosis_compat")
                    .define("apotheosis_compat", true);
        }
        SERVER_SPEC = SERVER_BUILDER.build();
        CLIENT_SPEC = CLIENT_BUILDER.build();
        container.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "godofthings-wondrous-server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "godofthings-wondrous-client.toml");
    }

    private static boolean value(ModConfigSpec.BooleanValue value)
    {
        return value != null && value.get();
    }

    public static boolean serverStaffAcceleration() { return value(SERVER_STAFF_ACCELERATION); }
    public static boolean idleThrottle() { return value(SERVER_IDLE_THROTTLE); }
    public static boolean entityDisableAi() { return value(SERVER_ENTITY_DISABLE_AI); }
    public static boolean enableStaffSummon() { return value(SERVER_STAFF_SUMMON); }
    public static boolean enableStaffLootRefresh() { return value(SERVER_STAFF_LOOT_REFRESH); }
    public static boolean serverTimeStop() { return value(SERVER_TIME_STOP); }
    public static boolean serverRangeAcceleration() { return value(SERVER_RANGE_ACCELERATION); }
    public static boolean serverRemoteReclaimer() { return value(SERVER_REMOTE_RECLAIMER); }
    public static boolean enableStaffLeafDrop() { return value(SERVER_STAFF_LEAF_DROP); }
    public static double staffLeafDropProbability() { return SERVER_STAFF_LEAF_DROP_PROBABILITY.get(); }
    public static boolean enableDollLeafDrop() { return value(SERVER_DOLL_LEAF_DROP); }
    public static double dollLeafDropProbability() { return SERVER_DOLL_LEAF_DROP_PROBABILITY.get(); }
    public static boolean highlightSeeThrough() { return value(CLIENT_HIGHLIGHT_SEE_THROUGH); }

    /** 上游 hasApotheosisConfig：compat 条目是否存在（= 是否装了 Apotheosis 附魔）。 */
    public static boolean hasApotheosisConfig()
    {
        return apotheosisCompatValue != null;
    }

    public static boolean enableApotheosisCompat()
    {
        return hasApotheosisConfig() && value(apotheosisCompatValue);
    }
}
