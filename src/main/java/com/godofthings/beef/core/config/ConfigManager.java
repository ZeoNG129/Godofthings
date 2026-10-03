package com.godofthings.beef.core.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * 上游 useless_mod 的配置管理器（照抄），如今只剩「无用维度地板黑白名单」这一块。
 *
 * <p><b>死代码清理历程</b>：</p>
 * <ol>
 *   <li>牛排工具框架整套删除，只服务于工具的配置项（客户端 / 服务端 {@code beef_tool} 段：
 *       连点速率、药水效果、飞行、连锁挖掘、范围伤害 / 磁力、时运、抢夺、战利品大爆发、
 *       打草掉杖、挖掘速度、无视挖掘等级、伪硬度、触及范围、短距传送、建筑手杖上限、
 *       强制挖掘黑名单、强制击杀名单）随之一并移除；</li>
 *   <li>高级合金炉（万象炉）服务端段整套删除：从 AE 网络抽电、AE 批次成熟窗口、
 *       产物回网时间预算、单批并行解除、{@code furnace_tier_*_threads} /
 *       {@code catalyst_tier_*} 两组档位数组、配方等级规则 {@code FURNACE_RECIPE_TIER_RULES}
 *       及其校验，外加 {@code AlloyFurnaceTierRules} 类；</li>
 *   <li><b>本次清理</b>：上游机器子系统带进来、但本模组从未接线的配置段全部删除 ——
 *       配方转换（{@code advanced_alloy_furnace.recipe_conversion} 的 ~60 个开关 +
 *       {@code RECIPE_CONVERSION_OPTIONS} 索引 + {@code isRecipeConversionEnabled}）、
 *       万象炉样板 / 模具 / 被动样板 / 解码缓存 / 九阶线圈 / 有用级线圈
 *       （{@code omniversal_multiblock_alloy_furnace} 段）、矿物生成器（{@code ore_generator} 段）、
 *       AE2 礼物包（{@code ae2_gift_package} 段）、机械升级倍率（{@code mekanism_upgrade} 段）、
 *       植物盆与矩阵样板（{@code game_mechanics} 段）。这些段的 getter 在本仓库里
 *       <b>一个调用方都没有</b>（逐个 grep 复核过），删完 ConfigManager 由 825 行降到 120 余行。</li>
 * </ol>
 *
 * <p><b>活引用只剩一处</b>：{@code DimensionGenerationConfig} 调用
 * {@link #isUselessDimensionFloorBlockAllowed(ResourceLocation)} 来过滤维度地板方块。</p>
 *
 * <p>{@code COMMON_SPEC} 与 {@code CLIENT_SPEC} 如今是空表（配方转换是 COMMON 侧唯一的内容，
 * 植物盆渲染开关是客户端侧唯一的内容）。空 spec 在 NeoForge 21.1.249 下是安全的：
 * {@code ModConfigSpec.Builder#build()} 的 {@code ensureEmpty()} 只校验「没有挂着的注释 /
 * 翻译键 / 范围 / 重启标记」，而 {@code validateSpec()} / {@code correct()} /
 * {@code acceptConfig()} 全部按值遍历，空表即空转（已查 neoforge-21.1.249-sources.jar 确认）。
 * 三个 SPEC 仍照常注册，配置文件 godofthings-beef-{common,client,server}.toml 名字不变。</p>
 */
public class ConfigManager {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec SERVER_SPEC;
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends String>>
            USELESS_DIMENSION_FLOOR_BLOCK_BLACKLIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>>
            USELESS_DIMENSION_FLOOR_BLOCK_WHITELIST;
    private static final String DIMENSION_FLOOR_BLACKLIST_NAME = "Useless Dimension floor block blacklist";
    private static final String DIMENSION_FLOOR_WHITELIST_NAME = "Useless Dimension floor block whitelist";
    private static volatile List<String> cachedUselessDimensionFloorBlockBlacklist = List.of();
    private static volatile BlockBlacklistMatcher uselessDimensionFloorBlockBlacklistMatcher =
            BlockBlacklistMatcher.empty(DIMENSION_FLOOR_BLACKLIST_NAME);
    private static volatile List<String> cachedUselessDimensionFloorBlockWhitelist = List.of();
    private static volatile BlockBlacklistMatcher uselessDimensionFloorBlockWhitelistMatcher =
            BlockBlacklistMatcher.empty(DIMENSION_FLOOR_WHITELIST_NAME);

    static {

        SERVER_BUILDER.translation("godofthings.configuration.useless_dimension")
                .push("useless_dimension");
        USELESS_DIMENSION_FLOOR_BLOCK_BLACKLIST = SERVER_BUILDER
                .comment("Blocks that cannot be used for Useless Dimension layers, borders, fills, roads, or center markers",
                        "A block matching this list remains blocked even if it matches the whitelist",
                        "Use exact block IDs, #block tags, or * wildcard patterns")
                .translation("godofthings.configuration.useless_dimension_floor_block_blacklist")
                .defineListAllowEmpty("floor_block_blacklist", List.<String>of(), () -> "",
                        entry -> entry instanceof String);
        USELESS_DIMENSION_FLOOR_BLOCK_WHITELIST = SERVER_BUILDER
                .comment("When non-empty, only matching blocks can be used for Useless Dimension layers, borders, fills, roads, or center markers",
                        "Leave empty to allow every block that is not on the blacklist",
                        "Use exact block IDs, #block tags, or * wildcard patterns")
                .translation("godofthings.configuration.useless_dimension_floor_block_whitelist")
                .defineListAllowEmpty("floor_block_whitelist", List.<String>of(), () -> "",
                        entry -> entry instanceof String);
        SERVER_BUILDER.pop();

        COMMON_SPEC = COMMON_BUILDER.build();
        CLIENT_SPEC = CLIENT_BUILDER.build();
        SERVER_SPEC = SERVER_BUILDER.build();
    }

    public static List<String> getUselessDimensionFloorBlockBlacklist() {
        return readConfigList(USELESS_DIMENSION_FLOOR_BLOCK_BLACKLIST);
    }

    public static List<String> getUselessDimensionFloorBlockWhitelist() {
        return readConfigList(USELESS_DIMENSION_FLOOR_BLOCK_WHITELIST);
    }

    public static boolean isUselessDimensionFloorBlockBlacklisted(ResourceLocation blockId) {
        return uselessDimensionFloorBlockBlacklistMatcher().matches(blockId);
    }

    public static boolean isUselessDimensionFloorBlockAllowed(ResourceLocation blockId) {
        return isUselessDimensionFloorBlockAllowed(blockId,
                uselessDimensionFloorBlockBlacklistMatcher(),
                uselessDimensionFloorBlockWhitelistMatcher());
    }

    static boolean isUselessDimensionFloorBlockAllowed(
            ResourceLocation blockId,
            BlockBlacklistMatcher blacklist,
            BlockBlacklistMatcher whitelist) {
        if (blockId == null || blacklist.matches(blockId)) return false;
        return whitelist.isEmpty() || whitelist.matches(blockId);
    }

    private static List<String> readConfigList(
            ModConfigSpec.ConfigValue<List<? extends String>> value) {
        List<? extends String> configured = getConfigValue(value);
        return configured == null ? List.of() : List.copyOf(configured);
    }

    private static <T> T getConfigValue(ModConfigSpec.ConfigValue<T> value) {
        try {
            return value.get();
        } catch (IllegalStateException ignored) {
            return value.getDefault();
        }
    }

    private static BlockBlacklistMatcher uselessDimensionFloorBlockBlacklistMatcher() {
        List<String> configured = getUselessDimensionFloorBlockBlacklist();
        if (!configured.equals(cachedUselessDimensionFloorBlockBlacklist)) {
            synchronized (ConfigManager.class) {
                if (!configured.equals(cachedUselessDimensionFloorBlockBlacklist)) {
                    uselessDimensionFloorBlockBlacklistMatcher =
                            new BlockBlacklistMatcher(configured, DIMENSION_FLOOR_BLACKLIST_NAME);
                    cachedUselessDimensionFloorBlockBlacklist = configured;
                }
            }
        }
        return uselessDimensionFloorBlockBlacklistMatcher;
    }

    private static BlockBlacklistMatcher uselessDimensionFloorBlockWhitelistMatcher() {
        List<String> configured = getUselessDimensionFloorBlockWhitelist();
        if (!configured.equals(cachedUselessDimensionFloorBlockWhitelist)) {
            synchronized (ConfigManager.class) {
                if (!configured.equals(cachedUselessDimensionFloorBlockWhitelist)) {
                    uselessDimensionFloorBlockWhitelistMatcher =
                            new BlockBlacklistMatcher(configured, DIMENSION_FLOOR_WHITELIST_NAME);
                    cachedUselessDimensionFloorBlockWhitelist = configured;
                }
            }
        }
        return uselessDimensionFloorBlockWhitelistMatcher;
    }

}
