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

            desc = "Allows a player to teleport to a located biome when in creative mode, opped, or in cheat mode.";
            allowTeleport = builder.comment(desc).define("allowTeleport", true);

            desc = "The maximum number of times a player can search for the next instance of a located biome. Set to 0 to disable searching for additional biome instances and make the compass always locate the nearest biome.";
            maxNextSearches = builder.comment(desc).defineInRange("maxNextSearches", 25, 0, 10000);

            desc = "Allows players to view the precise coordinates and distance of a located structure on the HUD, rather than relying on the direction the compass is pointing.";
            displayCoordinates = builder.comment(desc).define("displayCoordinates", true);

            desc = "biomeSize * radiusModifier = maxSearchRadius. Raising this value will increase search accuracy but will potentially make the process more resource .";
            radiusModifier = builder.comment(desc).defineInRange("radiusModifier", 2500, 0, 1000000);

            desc = "biomeSize * sampleSpaceModifier = sampleSpace. Lowering this value will increase search accuracy but will make the process more resource intensive.";
            sampleSpaceModifier = builder.comment(desc).defineInRange("sampleSpaceModifier", 16, 0, 1000000);

            desc = "A list of biomes that the compass will not be able to search for, specified by resource location. The wildcard character * can be used to match any number of characters, and ? can be used to match one character. Ex: [\"minecraft:savanna\", \"minecraft:desert\", \"minecraft:*ocean*\"]";
            biomeBlacklist = builder.comment(desc).define("biomeBlacklist", new ArrayList<String>());

            desc = "The default number of XP levels consumed when searching for a biome. Individual biomes can be configured via perBiomeXpLevels.";
            defaultXpLevels = builder.comment(desc).defineInRange("defaultXpLevels", 0, 0, 3);

            desc = "A list of per-biome XP level costs that override defaultXpLevels, specified as comma-separated \"biome_id,num_levels\" pairs. Biomes not listed here use defaultXpLevels. Max of 3 levels. The wildcard character * can be used to match any number of characters, and ? can be used to match one character. Ex: [\"minecraft:deep_dark,3\", \"minecraft:end*,2\", \"minecraft:*caves,3\"]";
            perBiomeXpLevels = builder.comment(desc).define("perBiomeXpLevels", new ArrayList<String>());

            desc = "The maximum number of samples to be taken when searching for a biome.";
            maxSamples = builder.comment(desc).defineInRange("maxSamples", 50000, 0, 1000000);

            desc = "The number of successful biome searches before the compass breaks and must be repaired. Set to 0 to disable durability.";
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

            desc = "Displays Nature's Compass information even while chat is open.";
            displayWithChatOpen = builder.comment(desc).define("displayWithChatOpen", true);

            desc = "Fixes biome names by adding missing spaces. Ex: ForestHills becomes Forest Hills";
            fixBiomeNames = builder.comment(desc).define("fixBiomeNames", true);

            desc = "The line offset for information rendered on the HUD.";
            overlayLineOffset = builder.comment(desc).defineInRange("overlayLineOffset", 1, 0, 50);

            desc = "The side for information rendered on the HUD. Ex: LEFT, RIGHT";
            overlaySide = builder.comment(desc).defineEnum("overlaySide", OverlaySide.LEFT);

            builder.pop();
        }
    }
}
