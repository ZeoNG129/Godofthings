package com.godofthings.beef.core.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 上游 useless_mod 的配置管理器（照抄）。
 *
 * <p><b>本次死代码清理</b>：牛排工具框架已整套删除，只服务于工具的配置项连同它们的
 * 字段、注册块、getter 与配套校验方法一并移除：客户端 {@code beef_tool} 段（连点速率）、
 * 服务端 {@code beef_tool} 段（药水效果 / 飞行 / 连锁挖掘范围与上限 / 范围伤害 / 范围磁力 /
 * 时运 / 抢夺 / 战利品大爆发 / 打草掉杖 / 挖掘速度 / 无视挖掘等级 / 伪硬度 / 触及范围 /
 * 短距传送 / 建筑手杖三个上限 / 强制挖掘黑名单 / 强制击杀名单）。</p>
 *
 * <p>保留的配置项都还有活引用：无用维度地板黑白名单（{@code DimensionGenerationConfig}）、
 * 合金炉等级规则（{@code AlloyFurnaceTierRules}）、以及上游机器子系统带进来但本模组
 * 尚未接线的那些段（配方转换 / 万向炉 / 线圈 / AE2 礼物包等，未列入本次清理范围）。</p>
 */
public class ConfigManager {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec SERVER_SPEC;
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
    // 植物盆生长速度配置
    private static final ModConfigSpec.IntValue BOTANY_POT_GROWTH_MULTIPLIER;
    // 植物盆渲染配置
    private static final ModConfigSpec.BooleanValue ENABLE_BOTANY_POT_RENDERING;
    // 矩阵样板数量配置
    private static final ModConfigSpec.IntValue MATRIX_PATTERN_COUNT;

    // Mekanism 升级配置
    private static final ModConfigSpec.IntValue TIME_MULTIPLIER;
    private static final ModConfigSpec.IntValue ELECTRICITY_MULTIPLIER;
    private static final ModConfigSpec.IntValue CAPACITY_MULTIPLIER;
    private static final ModConfigSpec.IntValue MAX_UPGRADE;

    // 万象炉从AE网络抽取能量配置
    private static final ModConfigSpec.BooleanValue FURNACE_DRAW_APPFLUX_ENERGY;
    private static final ModConfigSpec.BooleanValue FURNACE_DRAW_AE_ENERGY;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> FURNACE_RECIPE_TIER_RULES;
    // 万象炉 AE 批次成熟等待窗口
    private static final ModConfigSpec.IntValue FURNACE_AE_BATCH_RIPE_TICKS;
    // 万象炉产物回网每 tick 时间预算
    private static final ModConfigSpec.IntValue FURNACE_AE_OUTPUT_RETURN_BUDGET_MILLIS;
    // 万象炉是否解除「材料窗口」对单批规模的限制
    private static final ModConfigSpec.BooleanValue FURNACE_AE_UNLIMITED_BIGINT_PARALLELISM;
    private static final ModConfigSpec.IntValue[] FURNACE_TIER_THREADS =
            new ModConfigSpec.IntValue[11];
    private static final ModConfigSpec.IntValue[] CATALYST_TIER_PARALLEL =
            new ModConfigSpec.IntValue[10];
    private static final ModConfigSpec.IntValue[] CATALYST_TIER_ENERGY_DIVISOR =
            new ModConfigSpec.IntValue[10];
    private static final ModConfigSpec.DoubleValue[] CATALYST_TIER_TIME_MULTIPLIER =
            new ModConfigSpec.DoubleValue[10];

    // 万象炉配方转换配置
    private static final ModConfigSpec.BooleanValue ENABLE_CRAFTING_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_SMELTING_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_BREWING_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_FISHING_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_VILLAGER_TRADE_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_FARMERS_DELIGHT_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_EXTRA_DELIGHT_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_CRABBERS_DELIGHT_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_CASUALNESS_DELIGHT_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_EXPANDED_DELIGHT_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_BREWIN_AND_CHEWIN_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_NOMADS_DELIGHT_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_UBES_DELIGHT_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_BARBEQUES_DELIGHT_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_YOUKAI_HOMECOMING_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_KALEIDOSCOPE_COOKERY_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_KALEIDOSCOPE_GRILLING_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_KALEIDOSCOPE_TAVERN_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_EXTENDED_AE_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_ADVANCED_AE_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_MEKANISM_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_MEKANISM_GENERATORS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_APP_MEK_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_AE2_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_AE2CS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_INDUSTRIAL_FOREGOING_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_ACTUALLY_ADDITIONS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_ARS_NOUVEAU_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_MYSTICAL_AGRICULTURE_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_AE2LT_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_DATA_ENERGISTICS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_PRODUCTIVE_BEES_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_DRACONIC_EVOLUTION_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_POWAH_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_EXTENDED_CRAFTING_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_AVARITIA_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_NEO_ECO_AE_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_NATURES_AURA_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_FORBIDDEN_ARCANUS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_OCCULTISM_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_SUMMONING_RITUALS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_MALUM_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_ENDER_IO_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_CREATE_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_ORITECH_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_NEOVITAE_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_UFO_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_MODERN_INDUSTRIALIZATION_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_IMMERSIVE_ENGINEERING_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_PNEUMATICCRAFT_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_BIG_REACTORS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_IRONS_SPELLBOOKS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_HOSTILE_NETWORKS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_APOTHIC_FLUX_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_FLUX_NETWORKS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_APOTHEOSIS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_LYCHEE_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_ASTRAL_SORCERY_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_JUSTDIRETHINGS_RECIPE_CONVERSION;
    private static final ModConfigSpec.BooleanValue ENABLE_JDTE_RECIPE_CONVERSION;
    private static final Map<String, ModConfigSpec.BooleanValue> RECIPE_CONVERSION_OPTIONS;

    private static final ModConfigSpec.IntValue OMNIVERSAL_PATTERN_SLOTS;
    private static final ModConfigSpec.IntValue OMNIVERSAL_MOLD_SLOTS;
    private static final ModConfigSpec.IntValue OMNIVERSAL_PASSIVE_PATTERN_SLOTS;
    private static final ModConfigSpec.IntValue OMNIVERSAL_DECODE_CACHE_CAPACITY;
    private static final ModConfigSpec.IntValue[] OMNIVERSAL_COIL_TIER_THREADS =
            new ModConfigSpec.IntValue[9];
    private static final ModConfigSpec.LongValue[] OMNIVERSAL_COIL_TIER_PARALLEL =
            new ModConfigSpec.LongValue[9];
    private static final ModConfigSpec.IntValue[] OMNIVERSAL_COIL_TIER_ENERGY_DIVISOR =
            new ModConfigSpec.IntValue[9];
    private static final ModConfigSpec.DoubleValue[] OMNIVERSAL_COIL_TIER_TIME_MULTIPLIER =
            new ModConfigSpec.DoubleValue[9];
    private static final ModConfigSpec.IntValue OMNIVERSAL_USEFUL_TIER_THREADS;
    private static final ModConfigSpec.IntValue ORE_GENERATOR_SLOTS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>>
            USELESS_DIMENSION_FLOOR_BLOCK_BLACKLIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>>
            USELESS_DIMENSION_FLOOR_BLOCK_WHITELIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>>
            AE2_GIFT_PACKAGE_ITEMS;
    private static final String DIMENSION_FLOOR_BLACKLIST_NAME = "Useless Dimension floor block blacklist";
    private static final String DIMENSION_FLOOR_WHITELIST_NAME = "Useless Dimension floor block whitelist";
    private static volatile List<String> cachedUselessDimensionFloorBlockBlacklist = List.of();
    private static volatile BlockBlacklistMatcher uselessDimensionFloorBlockBlacklistMatcher =
            BlockBlacklistMatcher.empty(DIMENSION_FLOOR_BLACKLIST_NAME);
    private static volatile List<String> cachedUselessDimensionFloorBlockWhitelist = List.of();
    private static volatile BlockBlacklistMatcher uselessDimensionFloorBlockWhitelistMatcher =
            BlockBlacklistMatcher.empty(DIMENSION_FLOOR_WHITELIST_NAME);

    static {
        // Server config: these values change world or machine behavior.
        SERVER_BUILDER.push("game_mechanics");
        BOTANY_POT_GROWTH_MULTIPLIER = SERVER_BUILDER
                .comment("植物盆生长倍率 - 1.0为原版速度, 2.0为2倍速度 / Botany pot growth multiplier - 1.0 is vanilla speed, 2.0 is 2x speed")
                .defineInRange("botany_pot_growth_multiplier", 1, 1, Integer.MAX_VALUE);

        MATRIX_PATTERN_COUNT = SERVER_BUILDER
                .comment("矩阵样板槽位倍数 - 减少数量时请保持槽位空！否则可能会造成样板丢失 / Matrix pattern slot multiplier - keep slots empty when reducing! Otherwise patterns may be lost")
                .defineInRange("matrix_pattern_count", 1, 1, 100);
        SERVER_BUILDER.pop();

        // Client config: rendering-only options must never affect server behavior.
        CLIENT_BUILDER.push("game_mechanics");
        ENABLE_BOTANY_POT_RENDERING = CLIENT_BUILDER
                .comment("是否启用植物盆作物渲染 / Whether to enable botany pot crop rendering")
                .define("enable_botany_pot_rendering", true);
        CLIENT_BUILDER.pop();

        SERVER_BUILDER.translation("godofthings.configuration.omniversal_multiblock_alloy_furnace")
                .push("omniversal_multiblock_alloy_furnace");
        OMNIVERSAL_PATTERN_SLOTS = SERVER_BUILDER
                .comment("ME pattern assembly slots. Values are normalized to pages of 27.")
                .translation("godofthings.configuration.pattern_slots")
                .defineInRange("pattern_slots", 108, 27, 4096);
        OMNIVERSAL_MOLD_SLOTS = SERVER_BUILDER
                .comment("Omniversal mold hub slots. Values are normalized to pages of 27.")
                .translation("godofthings.configuration.mold_slots")
                .defineInRange("mold_slots", 108, 27, 4096);
        OMNIVERSAL_PASSIVE_PATTERN_SLOTS = SERVER_BUILDER
                .comment("Passive crafting hatch slots. Higher coil tiers unlock this capacity gradually.")
                .translation("godofthings.configuration.passive_pattern_slots")
                .defineInRange("passive_pattern_slots", 30, 1, 4096);
        OMNIVERSAL_DECODE_CACHE_CAPACITY = SERVER_BUILDER
                .comment("Maximum decoded omniversal pattern entries kept per level. Takes effect after restart.")
                .translation("godofthings.configuration.decode_cache_capacity")
                .defineInRange("decode_cache_capacity", 2048, 64, 16384);
        for (int tier = 1; tier <= 9; tier++) {
            int index = tier - 1;
            OMNIVERSAL_COIL_TIER_THREADS[index] = SERVER_BUILDER
                    .comment("普通线圈 " + tier + " 阶的最大AE任务数 / Max AE jobs of this coil tier")
                    .defineInRange("coil_tier_" + tier + "_threads", tier + 1, 1, Integer.MAX_VALUE);
            OMNIVERSAL_COIL_TIER_PARALLEL[index] = SERVER_BUILDER
                    .comment("普通线圈 " + tier + " 阶的单任务最大并行数 / Max parallel count for a single task at this coil tier")
                    .defineInRange("coil_tier_" + tier + "_single_task_parallel",
                            1L << (tier * 2), 1L, Long.MAX_VALUE);
            OMNIVERSAL_COIL_TIER_ENERGY_DIVISOR[index] = SERVER_BUILDER
                    .comment("普通线圈 " + tier + " 阶的能耗除数 / Energy divisor of this coil tier")
                    .defineInRange("coil_tier_" + tier + "_energy_divisor", 1 << tier, 1, Integer.MAX_VALUE);
            OMNIVERSAL_COIL_TIER_TIME_MULTIPLIER[index] = SERVER_BUILDER
                    .comment("普通线圈 " + tier + " 阶的处理时间倍率 / Processing time multiplier of this coil tier")
                    .defineInRange("coil_tier_" + tier + "_time_multiplier",
                            1.0 / (1L << tier), 0.0, 1.0);
        }
        OMNIVERSAL_USEFUL_TIER_THREADS = SERVER_BUILDER
                .comment("有用级线圈的最大AE任务数 / Max AE jobs of the useful-tier coil")
                .defineInRange("useful_tier_threads", 11, 1, Integer.MAX_VALUE);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.translation("godofthings.configuration.ore_generator")
                .push("ore_generator");
        ORE_GENERATOR_SLOTS = SERVER_BUILDER
                .comment("Ore generator sample slots. Slots above this value remain recovery-only.")
                .translation("godofthings.configuration.ore_generator_slots")
                .defineInRange("ore_generator_slots", 9, 1, 540);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.translation("godofthings.configuration.ae2_gift_package")
                .push("ae2_gift_package");
        AE2_GIFT_PACKAGE_ITEMS = SERVER_BUILDER
                .comment("Items granted by the AE2 gift package. Format: modid:item_id,count.",
                        "Missing items and entries with invalid quantities are skipped.")
                .translation("godofthings.configuration.ae2_gift_package.items")
                .defineListAllowEmpty("items", defaultAE2GiftPackageItems(), () -> "",
                        ConfigManager::isValidAE2GiftPackageEntry);
        SERVER_BUILDER.pop();

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

        SERVER_BUILDER.push("mekanism_upgrade");
        TIME_MULTIPLIER = SERVER_BUILDER
                .comment("速度升级增强倍率 / Speed upgrade multiplier")
                .defineInRange("time_multiplier", 1, 1, Integer.MAX_VALUE);

        ELECTRICITY_MULTIPLIER = SERVER_BUILDER
                .comment("能量升级节电增强倍率 / Energy upgrade efficiency multiplier")
                .defineInRange("electricity_multiplier", 1, 1, Integer.MAX_VALUE);

        CAPACITY_MULTIPLIER = SERVER_BUILDER
                .comment("能量升级储电增强倍率 / Energy upgrade capacity multiplier")
                .defineInRange("capacity_multiplier", 1, 1, Integer.MAX_VALUE);

        MAX_UPGRADE = SERVER_BUILDER
                .comment("机器可接受的最大速度/能量升级数量, 重启游戏生效 / Maximum number of speed/energy upgrades a machine accepts, takes effect after a game restart")
                .defineInRange("max_upgrade", 16, 1, 64);
        SERVER_BUILDER.pop();

        SERVER_BUILDER.translation("godofthings.configuration.advanced_alloy_furnace")
                .push("advanced_alloy_furnace");
        FURNACE_DRAW_APPFLUX_ENERGY = SERVER_BUILDER
                .comment("万象炉是否自动从所在AE网络抽取AppliedFlux(应用通量)存储的FE能量 / Whether the Advanced Alloy Furnace automatically draws FE stored by AppliedFlux from its AE network",
                        "需要安装AppliedFlux且网络中有通量元件, 每tick抽取量受熔炉最大输入速率限制 / Requires AppliedFlux and flux cells in the network; the per-tick draw is limited by the furnace max input rate")
                .define("draw_appflux_energy", true);

        FURNACE_DRAW_AE_ENERGY = SERVER_BUILDER
                .comment("万象炉是否直接抽取AE网络自身的能量(按 1 AE = 2 FE 折算) / Whether the Advanced Alloy Furnace draws the AE network energy directly (converted at 1 AE = 2 FE)",
                        "警告: 会与网络中其他设备争抢供电, 网络储能不足时可能导致设备频繁掉线 / Warning: competes for power with other devices in the network and may cause frequent device disconnects when network storage is low",
                        "在AppliedFlux抽取之后作为补充, 每tick总抽取量受熔炉最大输入速率限制 / Applied as a supplement after AppliedFlux extraction; the total per-tick draw is limited by the furnace max input rate")
                .define("draw_ae_energy", false);

        FURNACE_AE_BATCH_RIPE_TICKS = SERVER_BUILDER
                .comment("万象炉收到 AE 推送的批次后，先等待多少个 tick 再投入执行 / How many ticks the Advanced Alloy Furnace waits after receiving a pushed AE batch before starting it",
                        "这个窗口用于把连续推送合并成一个任务，等待期间 GUI 显示为「排队」 / This window merges consecutive pushes into a single task; the GUI shows Queued while waiting",
                        "设为 0 表示推送即刻投入执行；机器完全空闲（无任何运行中任务）时会跳过该窗口 / Set to 0 to start immediately; the window is skipped when the machine is completely idle (no running task)")
                .translation("godofthings.configuration.ae_batch_ripe_ticks")
                .defineInRange("ae_batch_ripe_ticks", 10, 0, 200);

        FURNACE_AE_UNLIMITED_BIGINT_PARALLELISM = SERVER_BUILDER
                .comment("是否解除「材料窗口」对单批规模的限制（即去掉 线程数 × Long.MAX 这道闸） / Whether to remove the material-window cap on single-batch size (that is, drop the threads x Long.MAX gate)",
                        "开启后单批规模只由「产物交付能力」决定，可以一次吃下任意大的份数， / When enabled, batch size is decided only by output delivery capacity, so a single dispatch can absorb arbitrarily large counts,",
                        "不必再靠堆线程数来抬高单批上限 / without stacking thread counts to raise the single-batch cap",
                        "代价：单次准入可能吃下极大量材料（由调用方自己的 BigInteger 账本扣除）； / Trade-off: a single admission may take an extremely large amount of materials (debited from the caller BigInteger ledger);",
                        "低档线圈会转而受「能量」闸限制，有用线圈无能量闸、不受影响 / Low-tier coils then fall back to the energy gate, while the useful coil has no energy gate and is unaffected")
                .translation("godofthings.configuration.ae_unlimited_bigint_parallelism")
                .define("ae_unlimited_bigint_parallelism", false);

        FURNACE_AE_OUTPUT_RETURN_BUDGET_MILLIS = SERVER_BUILDER
                .comment("万象炉每 tick 最多花多少毫秒把产物写回 ME 网络 / How many milliseconds per tick the Advanced Alloy Furnace may spend writing outputs back to the ME network",
                        "这个值直接决定「可持续合成速度」：AE2 存储接口单次只能写一个 long 分段， / This value directly sets the sustainable crafting speed: the AE2 storage interface accepts only one long chunk per call,",
                        "产物必须逐段插入，所以每 tick 能插多少次就决定了能跑多快 / outputs must be inserted chunk by chunk, so the per-tick insert count limits throughput",
                        "调大 = 合成更快，但单 tick 更重（可能掉 TPS）；调小 = 更省 tick，但合成变慢 / Higher = faster crafting but a heavier tick (may cost TPS); lower = cheaper tick but slower crafting",
                        "机器或服务端过载时，本预算还会被全局降频系数按比例收窄（下限 250 微秒） / When the machine or server is overloaded this budget is scaled down by the global throttle factor (floor 250 microseconds)",
                        "全局降频基准会跟随本值放大（= 本值×2，下限 10 毫秒），所以调大本值确实能生效 / The global throttle baseline scales with this value (this value x2, floor 10 ms), so raising it does take effect")
                .translation("godofthings.configuration.ae_output_return_budget_millis")
                .defineInRange("ae_output_return_budget_millis", 8, 1, 200);

        for (int tier = 0; tier <= 10; tier++) {
            FURNACE_TIER_THREADS[tier] = SERVER_BUILDER
                    .comment("单方块熔炉 " + tier + " 阶的最大AE任务数 / Max AE jobs of this single-block furnace tier")
                    .defineInRange("furnace_tier_" + tier + "_threads", tier + 1, 1, Integer.MAX_VALUE);
        }
        for (int tier = 0; tier <= 9; tier++) {
            CATALYST_TIER_PARALLEL[tier] = SERVER_BUILDER
                    .comment("催化剂 " + tier + " 阶的普通配方并行数 / Normal recipe parallel count of this catalyst tier")
                    .defineInRange("catalyst_tier_" + tier + "_parallel", 1 << tier, 1, Integer.MAX_VALUE);
            CATALYST_TIER_ENERGY_DIVISOR[tier] = SERVER_BUILDER
                    .comment("催化剂 " + tier + " 阶的能耗除数 / Energy divisor of this catalyst tier")
                    .defineInRange("catalyst_tier_" + tier + "_energy_divisor", 1, 1, Integer.MAX_VALUE);
            CATALYST_TIER_TIME_MULTIPLIER[tier] = SERVER_BUILDER
                    .comment("催化剂 " + tier + " 阶的处理时间倍率 / Processing time multiplier of this catalyst tier")
                    .defineInRange("catalyst_tier_" + tier + "_time_multiplier",
                            Math.max(0.1, 1.0 - tier * 0.1), 0.0, 1.0);
        }

        FURNACE_RECIPE_TIER_RULES = SERVER_BUILDER
                .comment("万象炉配方等级限制，格式为 配方ID通配符,等级 / Advanced Alloy Furnace recipe tier limits, format: recipe ID wildcard,tier",
                        "*可匹配任意字符；精确配方ID优先于通配符，匹配具体度相同时后面的规则覆盖前面的规则 / * matches any characters; exact recipe IDs take priority over wildcards, and later rules override earlier ones at the same specificity",
                        "等级范围为0-10；未匹配规则的配方不受限制 / Tiers range from 0 to 10; recipes matching no rule are unrestricted")
                .translation("godofthings.configuration.advanced_alloy_furnace.recipe_tier_rules")
                .defineListAllowEmpty("recipe_tier_rules", List.<String>of(), () -> "",
                        ConfigManager::isValidFurnaceRecipeTierRuleEntry);
        SERVER_BUILDER.pop();

        // Common config: adapter registration happens during common setup on both physical sides.
        COMMON_BUILDER.translation("godofthings.configuration.advanced_alloy_furnace")
                .push("advanced_alloy_furnace");
        COMMON_BUILDER.translation("godofthings.configuration.advanced_alloy_furnace.recipe_conversion")
                .push("recipe_conversion");
        COMMON_BUILDER.translation("godofthings.configuration.advanced_alloy_furnace.recipe_conversion.ae")
                .push("ae");
        ENABLE_ADVANCED_AE_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_advanced_ae_recipe_conversion", true);
        ENABLE_AE2_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_ae2_recipe_conversion", true);
        ENABLE_AE2CS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_ae2cs_recipe_conversion", true);
        ENABLE_AE2LT_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_ae2lt_recipe_conversion", true);
        ENABLE_DATA_ENERGISTICS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_data_energistics_recipe_conversion", true);
        ENABLE_EXTENDED_AE_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_extendedae_recipe_conversion", true);
        ENABLE_NEO_ECO_AE_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_neo_eco_ae_recipe_conversion", true);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.translation("godofthings.configuration.advanced_alloy_furnace.recipe_conversion.farmers_delight")
                .push("farmers_delight");
        ENABLE_BARBEQUES_DELIGHT_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_barbequesdelight_recipe_conversion", true);
        ENABLE_BREWIN_AND_CHEWIN_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_brewinandchewin_recipe_conversion", true);
        ENABLE_CASUALNESS_DELIGHT_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_casualnessdelight_recipe_conversion", true);
        ENABLE_CRABBERS_DELIGHT_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_crabbersdelight_recipe_conversion", true);
        ENABLE_EXPANDED_DELIGHT_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_expandeddelight_recipe_conversion", true);
        ENABLE_EXTRA_DELIGHT_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_extradelight_recipe_conversion", true);
        ENABLE_FARMERS_DELIGHT_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_farmersdelight_recipe_conversion", true);
        ENABLE_NOMADS_DELIGHT_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_nomadsdelight_recipe_conversion", true);
        ENABLE_UBES_DELIGHT_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_ubesdelight_recipe_conversion", true);
        ENABLE_YOUKAI_HOMECOMING_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_youkaishomecoming_recipe_conversion", true);
        COMMON_BUILDER.pop();

        // Just Dire Things 及其附属独立成组，与 AE、Mekanism 的分组方式保持一致
        COMMON_BUILDER.translation("godofthings.configuration.advanced_alloy_furnace.recipe_conversion.justdirethings")
                .push("justdirethings");
        ENABLE_JDTE_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_jdte_recipe_conversion", true);
        ENABLE_JUSTDIRETHINGS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_justdirethings_recipe_conversion", true);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.translation("godofthings.configuration.advanced_alloy_furnace.recipe_conversion.kaleidoscope")
                .push("kaleidoscope");
        ENABLE_KALEIDOSCOPE_COOKERY_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_kaleidoscope_cookery_recipe_conversion", true);
        ENABLE_KALEIDOSCOPE_GRILLING_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_kaleidoscope_grilling_recipe_conversion", true);
        ENABLE_KALEIDOSCOPE_TAVERN_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_kaleidoscope_tavern_recipe_conversion", true);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.translation("godofthings.configuration.advanced_alloy_furnace.recipe_conversion.mekanism")
                .push("mekanism");
        // AppMek 属 Mekanism 家族：其配方转换开关归入本分组，避免落在 AE 分组下造成误导
        ENABLE_APP_MEK_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_appmek_recipe_conversion", true);
        ENABLE_MEKANISM_GENERATORS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_mekanism_generators_recipe_conversion", true);
        ENABLE_MEKANISM_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_mekanism_recipe_conversion", true);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.translation("godofthings.configuration.advanced_alloy_furnace.recipe_conversion.minecraft")
                .push("minecraft");
        ENABLE_BREWING_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_brewing_recipe_conversion", true);
        ENABLE_CRAFTING_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_crafting_recipe_conversion", false);
        ENABLE_FISHING_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_fishing_recipe_conversion", true);
        ENABLE_SMELTING_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_smelting_recipe_conversion", true);
        ENABLE_VILLAGER_TRADE_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_villager_trade_recipe_conversion", true);
        COMMON_BUILDER.pop();

        COMMON_BUILDER.translation("godofthings.configuration.advanced_alloy_furnace.recipe_conversion.other")
                .push("other");
        ENABLE_ACTUALLY_ADDITIONS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_actually_additions_recipe_conversion", true);
        ENABLE_APOTHIC_FLUX_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_apothic_flux_recipe_conversion", true);
        ENABLE_FLUX_NETWORKS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_fluxnetworks_recipe_conversion", true);
        ENABLE_APOTHEOSIS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_apotheosis_recipe_conversion", true);
        ENABLE_LYCHEE_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_lychee_recipe_conversion", true);
        ENABLE_ASTRAL_SORCERY_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_astralsorcery_recipe_conversion", true);
        ENABLE_ARS_NOUVEAU_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_ars_nouveau_recipe_conversion", true);
        ENABLE_AVARITIA_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_avaritia_recipe_conversion", true);
        ENABLE_BIG_REACTORS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_bigreactors_recipe_conversion", true);
        ENABLE_CREATE_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_create_recipe_conversion", true);
        ENABLE_DRACONIC_EVOLUTION_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_draconic_evolution_recipe_conversion", true);
        ENABLE_ENDER_IO_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_ender_io_recipe_conversion", true);
        ENABLE_EXTENDED_CRAFTING_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_extended_crafting_recipe_conversion", true);
        ENABLE_FORBIDDEN_ARCANUS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_forbidden_arcanus_recipe_conversion", true);
        ENABLE_HOSTILE_NETWORKS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_hostilenetworks_recipe_conversion", true);
        ENABLE_IMMERSIVE_ENGINEERING_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_immersiveengineering_recipe_conversion", true);
        ENABLE_INDUSTRIAL_FOREGOING_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_industrial_foregoing_recipe_conversion", true);
        ENABLE_IRONS_SPELLBOOKS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_irons_spellbooks_recipe_conversion", true);
        ENABLE_MALUM_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_malum_recipe_conversion", true);
        ENABLE_MODERN_INDUSTRIALIZATION_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_modern_industrialization_recipe_conversion", true);
        ENABLE_MYSTICAL_AGRICULTURE_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_mystical_agriculture_recipe_conversion", true);
        ENABLE_NATURES_AURA_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_natures_aura_recipe_conversion", true);
        ENABLE_NEOVITAE_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_neovitae_recipe_conversion", true);
        ENABLE_OCCULTISM_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_occultism_recipe_conversion", true);
        ENABLE_ORITECH_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_oritech_recipe_conversion", true);
        ENABLE_PNEUMATICCRAFT_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_pneumaticcraft_recipe_conversion", true);
        ENABLE_POWAH_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_powah_recipe_conversion", true);
        ENABLE_PRODUCTIVE_BEES_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_productive_bees_recipe_conversion", true);
        ENABLE_SUMMONING_RITUALS_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_summoningrituals_recipe_conversion", true);
        ENABLE_UFO_RECIPE_CONVERSION = defineRecipeConversionOption(
                "enable_ufo_recipe_conversion", true);
        COMMON_BUILDER.pop();

        RECIPE_CONVERSION_OPTIONS = Map.ofEntries(
                Map.entry("actuallyadditions", ENABLE_ACTUALLY_ADDITIONS_RECIPE_CONVERSION),
                Map.entry("advanced_ae", ENABLE_ADVANCED_AE_RECIPE_CONVERSION),
                Map.entry("ae2", ENABLE_AE2_RECIPE_CONVERSION),
                Map.entry("ae2cs", ENABLE_AE2CS_RECIPE_CONVERSION),
                Map.entry("ae2lt", ENABLE_AE2LT_RECIPE_CONVERSION),
                Map.entry("apotheosis", ENABLE_APOTHEOSIS_RECIPE_CONVERSION),
                Map.entry("apothic_flux", ENABLE_APOTHIC_FLUX_RECIPE_CONVERSION),
                Map.entry("appmek", ENABLE_APP_MEK_RECIPE_CONVERSION),
                Map.entry("ars_nouveau", ENABLE_ARS_NOUVEAU_RECIPE_CONVERSION),
                Map.entry("astralsorcery", ENABLE_ASTRAL_SORCERY_RECIPE_CONVERSION),
                Map.entry("avaritia", ENABLE_AVARITIA_RECIPE_CONVERSION),
                Map.entry("barbequesdelight", ENABLE_BARBEQUES_DELIGHT_RECIPE_CONVERSION),
                Map.entry("bigreactors", ENABLE_BIG_REACTORS_RECIPE_CONVERSION),
                Map.entry("brewinandchewin", ENABLE_BREWIN_AND_CHEWIN_RECIPE_CONVERSION),
                Map.entry("casualnessdelight", ENABLE_CASUALNESS_DELIGHT_RECIPE_CONVERSION),
                Map.entry("crabbersdelight", ENABLE_CRABBERS_DELIGHT_RECIPE_CONVERSION),
                Map.entry("create", ENABLE_CREATE_RECIPE_CONVERSION),
                Map.entry("data_energistics", ENABLE_DATA_ENERGISTICS_RECIPE_CONVERSION),
                Map.entry("draconicevolution", ENABLE_DRACONIC_EVOLUTION_RECIPE_CONVERSION),
                Map.entry("enderio", ENABLE_ENDER_IO_RECIPE_CONVERSION),
                Map.entry("expandeddelight", ENABLE_EXPANDED_DELIGHT_RECIPE_CONVERSION),
                Map.entry("extendedae", ENABLE_EXTENDED_AE_RECIPE_CONVERSION),
                Map.entry("extendedcrafting", ENABLE_EXTENDED_CRAFTING_RECIPE_CONVERSION),
                Map.entry("extradelight", ENABLE_EXTRA_DELIGHT_RECIPE_CONVERSION),
                Map.entry("farmersdelight", ENABLE_FARMERS_DELIGHT_RECIPE_CONVERSION),
                Map.entry("forbidden_arcanus", ENABLE_FORBIDDEN_ARCANUS_RECIPE_CONVERSION),
                Map.entry("fluxnetworks", ENABLE_FLUX_NETWORKS_RECIPE_CONVERSION),
                Map.entry("hostilenetworks", ENABLE_HOSTILE_NETWORKS_RECIPE_CONVERSION),
                Map.entry("immersiveengineering", ENABLE_IMMERSIVE_ENGINEERING_RECIPE_CONVERSION),
                Map.entry("industrialforegoing", ENABLE_INDUSTRIAL_FOREGOING_RECIPE_CONVERSION),
                Map.entry("irons_spellbooks", ENABLE_IRONS_SPELLBOOKS_RECIPE_CONVERSION),
                Map.entry("jdte", ENABLE_JDTE_RECIPE_CONVERSION),
                Map.entry("justdirethings", ENABLE_JUSTDIRETHINGS_RECIPE_CONVERSION),
                Map.entry("kaleidoscope_cookery", ENABLE_KALEIDOSCOPE_COOKERY_RECIPE_CONVERSION),
                Map.entry("kaleidoscope_grilling", ENABLE_KALEIDOSCOPE_GRILLING_RECIPE_CONVERSION),
                Map.entry("kaleidoscope_tavern", ENABLE_KALEIDOSCOPE_TAVERN_RECIPE_CONVERSION),
                Map.entry("lychee", ENABLE_LYCHEE_RECIPE_CONVERSION),
                Map.entry("malum", ENABLE_MALUM_RECIPE_CONVERSION),
                Map.entry("mekanism", ENABLE_MEKANISM_RECIPE_CONVERSION),
                Map.entry("mekanismgenerators", ENABLE_MEKANISM_GENERATORS_RECIPE_CONVERSION),
                Map.entry("minecraft_fishing", ENABLE_FISHING_RECIPE_CONVERSION),
                Map.entry("modern_industrialization", ENABLE_MODERN_INDUSTRIALIZATION_RECIPE_CONVERSION),
                Map.entry("mysticalagriculture", ENABLE_MYSTICAL_AGRICULTURE_RECIPE_CONVERSION),
                Map.entry("naturesaura", ENABLE_NATURES_AURA_RECIPE_CONVERSION),
                Map.entry("neoecoae", ENABLE_NEO_ECO_AE_RECIPE_CONVERSION),
                Map.entry("neovitae", ENABLE_NEOVITAE_RECIPE_CONVERSION),
                Map.entry("nomads_delight", ENABLE_NOMADS_DELIGHT_RECIPE_CONVERSION),
                Map.entry("nomadsdelight", ENABLE_NOMADS_DELIGHT_RECIPE_CONVERSION),
                Map.entry("occultism", ENABLE_OCCULTISM_RECIPE_CONVERSION),
                Map.entry("oritech", ENABLE_ORITECH_RECIPE_CONVERSION),
                Map.entry("pneumaticcraft", ENABLE_PNEUMATICCRAFT_RECIPE_CONVERSION),
                Map.entry("powah", ENABLE_POWAH_RECIPE_CONVERSION),
                Map.entry("productivebees", ENABLE_PRODUCTIVE_BEES_RECIPE_CONVERSION),
                Map.entry("summoningrituals", ENABLE_SUMMONING_RITUALS_RECIPE_CONVERSION),
                Map.entry("ubesdelight", ENABLE_UBES_DELIGHT_RECIPE_CONVERSION),
                Map.entry("ufo", ENABLE_UFO_RECIPE_CONVERSION),
                Map.entry("youkaisfeasts", ENABLE_YOUKAI_HOMECOMING_RECIPE_CONVERSION),
                Map.entry("youkaishomecoming", ENABLE_YOUKAI_HOMECOMING_RECIPE_CONVERSION));
        COMMON_BUILDER.pop();
        COMMON_BUILDER.pop();

        COMMON_SPEC = COMMON_BUILDER.build();
        CLIENT_SPEC = CLIENT_BUILDER.build();
        SERVER_SPEC = SERVER_BUILDER.build();
    }

    private static ModConfigSpec.BooleanValue defineRecipeConversionOption(
            String key, boolean defaultValue) {
        return COMMON_BUILDER
                .comment("是否启用该配方来源的配方转换 / Whether to enable recipe conversion for this recipe source", "修改后重启游戏生效 / Takes effect after restarting the game")
                .translation("godofthings.configuration." + key)
                .define(key, defaultValue);
    }

    private static boolean isValidFurnaceRecipeTierRuleEntry(Object entry) {
        return AlloyFurnaceTierRules.isValidEntry(entry);
    }

    private static List<String> defaultAE2GiftPackageItems() {
        List<String> items = new ArrayList<>();
        items.add("ae2:creative_energy_cell,1");
        items.add("ae2:fluix_covered_cable,64");
        items.add("ae2:wireless_access_point,1");
        items.add("ae2:wireless_booster,64");
        items.add("ae2:wireless_crafting_terminal,1");
        items.add("ae2:crafting_terminal,1");

        if (ModList.get().isLoaded("extendedae_plus")) {
            items.add("extendedae_plus:infinity_biginteger_cell,1");
        } else {
            items.add("ae2:item_storage_cell_256k,8");
        }

        if (ModList.get().isLoaded("extendedae")) {
            items.add("extendedae:ex_drive,1");
        } else {
            items.add("ae2:drive,1");
        }

        if (ModList.get().isLoaded("ae2wtlib")) {
            items.add("ae2wtlib:quantum_bridge_card,1");
            items.add("ae2:quantum_ring,8");
            items.add("ae2:quantum_link,1");
            items.add("ae2:quantum_entangled_singularity,2");
        }

        return List.copyOf(items);
    }

    private static boolean isValidAE2GiftPackageEntry(Object entry) {
        if (!(entry instanceof String value)) {
            return false;
        }

        String[] parts = value.split(",", -1);
        if (parts.length != 2 || ResourceLocation.tryParse(parts[0].trim()) == null) {
            return false;
        }

        try {
            return Integer.parseInt(parts[1].trim()) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static int getBotanyPotGrowthMultiplier() {
        return getConfigValue(BOTANY_POT_GROWTH_MULTIPLIER);
    }

    public static boolean shouldEnableBotanyPotRendering() {
        return getConfigValue(ENABLE_BOTANY_POT_RENDERING);
    }

    public static int getMatrixPatternCount() {
        return getConfigValue(MATRIX_PATTERN_COUNT);
    }

    public static int getTimeMultiplier() {
        return getConfigValue(TIME_MULTIPLIER);
    }

    public static int getElectricityMultiplier() {
        return getConfigValue(ELECTRICITY_MULTIPLIER);
    }

    public static int getCapacityMultiplier() {
        return getConfigValue(CAPACITY_MULTIPLIER);
    }

    public static int getMaxUpgrade() {
        return getConfigValue(MAX_UPGRADE);
    }

    // 万象炉AE网络抽电配置
    public static boolean isFurnaceDrawAppfluxEnergyEnabled() {
        return getConfigValue(FURNACE_DRAW_APPFLUX_ENERGY);
    }

    public static boolean isFurnaceDrawAeEnergyEnabled() {
        return getConfigValue(FURNACE_DRAW_AE_ENERGY);
    }

    public static List<String> getFurnaceRecipeTierRules() {
        return readConfigList(FURNACE_RECIPE_TIER_RULES);
    }

    public static int getAdvancedAlloyFurnaceTierThreads(int tier) {
        return getConfigValue(FURNACE_TIER_THREADS[Math.max(0, Math.min(10, tier))]);
    }

    /** 万象炉 AE 批次成熟等待窗口（tick）；0 表示推送即刻投入执行 */
    public static int getAdvancedAlloyFurnaceAeBatchRipeTicks() {
        return Math.max(0, getConfigValue(FURNACE_AE_BATCH_RIPE_TICKS));
    }

    /**
     * 万象炉每 tick 的产物回网时间预算（毫秒）。
     *
     * <p>它直接决定可持续合成速度：AE2 存储接口单次只能写一个 long 分段，产物必须逐段插入，
     * 所以每 tick 能插多少次就决定了能跑多快。</p>
     */
    public static int getAdvancedAlloyFurnaceAeOutputReturnBudgetMillis() {
        return Math.max(1, getConfigValue(FURNACE_AE_OUTPUT_RETURN_BUDGET_MILLIS));
    }

    /**
     * 是否解除「材料窗口」对单批规模的限制。
     *
     * <p>开启后 {@code maximumWindowedCount} 返回哨兵值，单批规模只受产物交付能力约束。</p>
     */
    public static boolean isFurnaceAeUnlimitedBigintParallelism() {
        return getConfigValue(FURNACE_AE_UNLIMITED_BIGINT_PARALLELISM);
    }

    public static int getAdvancedAlloyFurnaceCatalystParallel(int tier) {
        return getConfigValue(CATALYST_TIER_PARALLEL[Math.max(0, Math.min(9, tier))]);
    }

    public static int getAdvancedAlloyFurnaceCatalystEnergyDivisor(int tier) {
        return getConfigValue(CATALYST_TIER_ENERGY_DIVISOR[Math.max(0, Math.min(9, tier))]);
    }

    public static double getAdvancedAlloyFurnaceCatalystTimeMultiplier(int tier) {
        return getConfigValue(CATALYST_TIER_TIME_MULTIPLIER[Math.max(0, Math.min(9, tier))]);
    }

    public static int getOmniversalCoilThreads(int tier) {
        return getConfigValue(OMNIVERSAL_COIL_TIER_THREADS[Math.max(1, Math.min(9, tier)) - 1]);
    }

    public static long getOmniversalCoilSingleTaskParallel(int tier) {
        return getConfigValue(OMNIVERSAL_COIL_TIER_PARALLEL[Math.max(1, Math.min(9, tier)) - 1]);
    }

    public static int getOmniversalCoilEnergyDivisor(int tier) {
        return getConfigValue(OMNIVERSAL_COIL_TIER_ENERGY_DIVISOR[Math.max(1, Math.min(9, tier)) - 1]);
    }

    public static double getOmniversalCoilTimeMultiplier(int tier) {
        return getConfigValue(OMNIVERSAL_COIL_TIER_TIME_MULTIPLIER[Math.max(1, Math.min(9, tier)) - 1]);
    }

    public static int getOmniversalUsefulTierThreads() {
        return getConfigValue(OMNIVERSAL_USEFUL_TIER_THREADS);
    }

    public static boolean isCraftingRecipeConversionEnabled() {
        return getConfigValue(ENABLE_CRAFTING_RECIPE_CONVERSION);
    }

    public static boolean isSmeltingRecipeConversionEnabled() {
        return getConfigValue(ENABLE_SMELTING_RECIPE_CONVERSION);
    }

    public static boolean isBrewingRecipeConversionEnabled() {
        return getConfigValue(ENABLE_BREWING_RECIPE_CONVERSION);
    }

    public static boolean isVillagerTradeRecipeConversionEnabled() {
        return getConfigValue(ENABLE_VILLAGER_TRADE_RECIPE_CONVERSION);
    }

    public static boolean isFishingRecipeConversionEnabled() {
        return getConfigValue(ENABLE_FISHING_RECIPE_CONVERSION);
    }

    public static boolean isRecipeConversionEnabled(String sourceId) {
        ModConfigSpec.BooleanValue option = RECIPE_CONVERSION_OPTIONS.get(sourceId);
        return option == null || getConfigValue(option);
    }

    public static int getOmniversalPatternSlots() {
        return normalizeInventorySlots(getConfigValue(OMNIVERSAL_PATTERN_SLOTS));
    }

    public static int getOmniversalMoldSlots() {
        return normalizeInventorySlots(getConfigValue(OMNIVERSAL_MOLD_SLOTS));
    }

    public static int getOmniversalPassivePatternSlots() {
        return Math.max(1, Math.min(4096, getConfigValue(OMNIVERSAL_PASSIVE_PATTERN_SLOTS)));
    }

    public static int getOmniversalDecodeCacheCapacity() {
        return Math.max(64, Math.min(16384, getConfigValue(OMNIVERSAL_DECODE_CACHE_CAPACITY)));
    }

    public static int getOreGeneratorSlots() {
        return Math.max(1, Math.min(540, getConfigValue(ORE_GENERATOR_SLOTS)));
    }

    private static int normalizeInventorySlots(int value) {
        int clamped = Math.max(27, Math.min(4096, value));
        return Math.max(27, clamped / 27 * 27);
    }

    public static List<String> getAE2GiftPackageItems() {
        return resolveAE2GiftPackageItems(readConfigList(AE2_GIFT_PACKAGE_ITEMS));
    }

    /**
     * Upgrades the automatic fallback entries from older server configs when an
     * AE2 addon is installed after the config was first created. Explicit custom
     * entries are left untouched.
     */
    private static List<String> resolveAE2GiftPackageItems(List<String> configured) {
        if (configured.isEmpty()) {
            return configured;
        }

        List<String> resolved = new ArrayList<>(configured);
        if (ModList.get().isLoaded("extendedae_plus")) {
            replaceLegacyAE2GiftStorageCells(resolved);
        }
        if (ModList.get().isLoaded("extendedae")) {
            replaceLegacyAE2GiftEntry(resolved,
                    "ae2:drive", 1,
                    "extendedae:ex_drive,1");
        }

        return List.copyOf(resolved);
    }

    private static void replaceLegacyAE2GiftStorageCells(List<String> entries) {
        String replacement = "extendedae_plus:infinity_biginteger_cell,1";
        if (containsAE2GiftItem(entries, "extendedae_plus:infinity_biginteger_cell")) {
            return;
        }

        int firstLegacyEntry = -1;
        for (int index = 0; index < entries.size(); index++) {
            String entry = entries.get(index);
            if (entry == null) {
                continue;
            }

            String[] parts = entry.split(",", -1);
            if (parts.length != 2 || !"ae2:item_storage_cell_256k".equals(parts[0].trim())) {
                continue;
            }

            if (parsePositiveInteger(parts[1].trim()) != 8) {
                continue;
            }
            if (firstLegacyEntry < 0) {
                firstLegacyEntry = index;
            }
        }

        if (firstLegacyEntry < 0) {
            return;
        }

        for (int index = entries.size() - 1; index >= 0; index--) {
            String entry = entries.get(index);
            if (entry == null) {
                continue;
            }

            String[] parts = entry.split(",", -1);
            if (parts.length == 2
                    && "ae2:item_storage_cell_256k".equals(parts[0].trim())
                    && parsePositiveInteger(parts[1].trim()) == 8) {
                entries.remove(index);
            }
        }
        entries.add(firstLegacyEntry, replacement);
    }

    private static void replaceLegacyAE2GiftEntry(
            List<String> entries, String legacyItemId, int legacyCount, String replacementEntry) {
        String replacementItemId = replacementEntry.substring(0, replacementEntry.indexOf(','));
        if (containsAE2GiftItem(entries, replacementItemId)) {
            return;
        }

        for (int index = 0; index < entries.size(); index++) {
            String entry = entries.get(index);
            if (entry == null) {
                continue;
            }

            String[] parts = entry.split(",", -1);
            if (parts.length == 2
                    && legacyItemId.equals(parts[0].trim())
                    && parsePositiveInteger(parts[1].trim()) == legacyCount) {
                entries.set(index, replacementEntry);
                return;
            }
        }
    }

    private static int parsePositiveInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean containsAE2GiftItem(List<String> entries, String itemId) {
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }

            int separator = entry.indexOf(',');
            if (separator >= 0 && itemId.equals(entry.substring(0, separator).trim())) {
                return true;
            }
        }
        return false;
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
