package com.godofthings.compass.natures.config;

import java.util.ArrayList;
import java.util.List;

import com.godofthings.compass.natures.client.OverlaySide;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 自然罗盘的配置（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>两项配置各自成一份 spec：{@code GENERAL_SPEC} 是服务端权威值（搜索半径 / 采样数 / 群系黑名单 /
 * 经验消耗 / 耐久），{@code CLIENT_SPEC} 是纯客户端显示项（HUD 位置 / 是否修群系名）。
 * 注册在 {@code Godofthings} 的构造器里，文件名 {@code godofthings-natures-compass.toml}
 * 与 {@code godofthings-natures-compass-client.toml}。</p>
 */
public class ConfigHandler
{
    private static final ModConfigSpec.Builder GENERAL_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    public static final General GENERAL = new General(GENERAL_BUILDER);
    public static final Client CLIENT = new Client(CLIENT_BUILDER);

    public static final ModConfigSpec GENERAL_SPEC = GENERAL_BUILDER.build();
    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();

    /** 服务端权威配置（搜索算法参数） */
    public static class General
    {
        public final ModConfigSpec.BooleanValue allowTeleport;
        public final ModConfigSpec.IntValue maxNextSearches;
        public final ModConfigSpec.BooleanValue displayCoordinates;
        public final ModConfigSpec.IntValue radiusModifier;
        public final ModConfigSpec.IntValue sampleSpaceModifier;
        public final ModConfigSpec.ConfigValue<List<String>> biomeBlacklist;
        public final ModConfigSpec.IntValue defaultXpLevels;
        public final ModConfigSpec.ConfigValue<List<String>> perBiomeXpLevels;
        public final ModConfigSpec.IntValue maxSamples;
        public final ModConfigSpec.IntValue compassDurability;

        General(ModConfigSpec.Builder builder)
        {
            String desc;
            builder.push("General");

            desc = "允许玩家传送到已定位的生物群系（创造模式 / OP / 作弊模式可用）。";
            allowTeleport = builder.comment(desc).define("allowTeleport", true);

            desc = "玩家搜索下一个已定位生物群系的最大次数。设为 0 则禁用继续搜索，罗盘将始终定位最近的目标群系。";
            maxNextSearches = builder.comment(desc).defineInRange("maxNextSearches", 25, 0, 10000);

            desc = "在 HUD 上显示已定位目标的精确坐标与距离，而不是只依赖罗盘指向。";
            displayCoordinates = builder.comment(desc).define("displayCoordinates", true);

            desc = "生物群系尺寸 × 该值 = 最大搜索半径。提高该值会提升搜索精度，但可能增加资源开销。";
            radiusModifier = builder.comment(desc).defineInRange("radiusModifier", 2500, 0, 1000000);

            desc = "生物群系尺寸 × 该值 = 采样间隔。降低该值会提升搜索精度，但会显著增加资源开销。";
            sampleSpaceModifier = builder.comment(desc).defineInRange("sampleSpaceModifier", 16, 0, 1000000);

            desc = "A list of biomes that the compass will not be able to search for, specified by resource location. The wildcard character * can be used to match any number of characters, and ? can be used to match one character. Ex: [\"minecraft:savanna\", \"minecraft:desert\", \"minecraft:*ocean*\"]";
            biomeBlacklist = builder.comment(desc).define("biomeBlacklist", new ArrayList<String>());

            desc = "搜索生物群系默认消耗的经验等级数。个别群系可通过 perBiomeXpLevels 单独配置。";
            defaultXpLevels = builder.comment(desc).defineInRange("defaultXpLevels", 0, 0, 3);

            desc = "A list of per-biome XP level costs that override defaultXpLevels, specified as comma-separated \"biome_id,num_levels\" pairs. Biomes not listed here use defaultXpLevels. Max of 3 levels. The wildcard character * can be used to match any number of characters, and ? can be used to match one character. Ex: [\"minecraft:deep_dark,3\", \"minecraft:end*,2\", \"minecraft:*caves,3\"]";
            perBiomeXpLevels = builder.comment(desc).define("perBiomeXpLevels", new ArrayList<String>());

            desc = "搜索生物群系时的最大采样数。";
            maxSamples = builder.comment(desc).defineInRange("maxSamples", 50000, 0, 1000000);

            desc = "罗盘完成多少次成功搜索后损坏并需要修复。设为 0 则无耐久限制。";
            compassDurability = builder.comment(desc).defineInRange("compassDurability", 0, 0, 10000);

            builder.pop();
        }
    }

    /** 客户端配置（HUD 显示） */
    public static class Client
    {
        public final ModConfigSpec.BooleanValue displayWithChatOpen;
        public final ModConfigSpec.BooleanValue fixBiomeNames;
        public final ModConfigSpec.EnumValue<OverlaySide> overlaySide;
        public final ModConfigSpec.IntValue overlayLineOffset;

        Client(ModConfigSpec.Builder builder)
        {
            String desc;
            builder.push("Client");

            desc = "聊天栏打开时仍在 HUD 显示自然罗盘信息。";
            displayWithChatOpen = builder.comment(desc).define("displayWithChatOpen", true);

            desc = "修正生物群系名称缺失的空格（例：ForestHills → Forest Hills）。";
            fixBiomeNames = builder.comment(desc).define("fixBiomeNames", true);

            desc = "HUD 信息渲染的行偏移量。";
            overlayLineOffset = builder.comment(desc).defineInRange("overlayLineOffset", 1, 0, 50);

            desc = "HUD 信息渲染在屏幕哪一侧。例：LEFT / RIGHT";
            overlaySide = builder.comment(desc).defineEnum("overlaySide", OverlaySide.LEFT);

            builder.pop();
        }
    }
}
