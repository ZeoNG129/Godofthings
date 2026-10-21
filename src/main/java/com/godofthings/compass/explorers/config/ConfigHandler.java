package com.godofthings.compass.explorers.config;

import java.util.ArrayList;
import java.util.List;

import com.godofthings.compass.explorers.client.OverlaySide;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 探险者指南针的配置（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>两项配置各自成一份 spec：{@code GENERAL_SPEC} 是服务端权威值（搜索半径 / 采样数 / 结构黑名单 /
 * 传送开关 / 自定义模型），{@code CLIENT_SPEC} 是纯客户端显示项（HUD 位置 / 是否翻译结构名）。
 * 注册在 {@code Godofthings} 的构造器里，文件名 {@code godofthings-explorers-compass.toml}
 * 与 {@code godofthings-explorers-compass-client.toml}。</p>
 *
 * <p>上游是 1.20.x Forge 的 {@code ForgeConfigSpec}，这里换成 NeoForge 的 {@link ModConfigSpec}，
 * 结构（builder.push / comment / define / defineInRange / defineEnum）完全一致。</p>
 *
 * <p><b>已删除</b>：上游 {@code Client.newWayPoint}（「搜到后自动在小地图模组里建路径点」）整项删除，
 * 用户明确要求去掉那段外部地图联动。</p>
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
        public final ModConfigSpec.BooleanValue displayCoordinates;
        public final ModConfigSpec.IntValue maxRadius;
        public final ModConfigSpec.ConfigValue<List<String>> structureBlacklist;
        public final ModConfigSpec.IntValue maxSamples;
        public final ModConfigSpec.BooleanValue cleanCache;
        public final ModConfigSpec.BooleanValue couldFoundAlexCaves;
        public final ModConfigSpec.BooleanValue customResource;

        General(ModConfigSpec.Builder builder)
        {
            String desc;
            builder.push("General");

            desc = "Allows a player to teleport to a located structure when in creative mode, opped, or in cheat mode.";
            allowTeleport = builder.comment(desc).define("allowTeleport", true);

            desc = "Allows players to view the precise coordinates and distance of a located structure on the HUD, rather than relying on the direction the compass is pointing.";
            displayCoordinates = builder.comment(desc).define("displayCoordinates", true);

            desc = "The maximum radius that will be searched for a structure. Raising this value will increase search accuracy but will potentially make the process more resource intensive.";
            maxRadius = builder.comment(desc).defineInRange("maxRadius", 10000, 0, 1000000);

            desc = "A list of structures that the compass will not display in the GUI and will not be able to search for. Wildcard character * can be used to match any number of characters, and ? can be used to match one character. Ex: [\"minecraft:stronghold\", \"minecraft:endcity\", \"minecraft:*village*\"]";
            structureBlacklist = builder.comment(desc).define("structureBlacklist", new ArrayList<>());

            desc = "The maximum number of samples to be taken when searching for a structure.";
            maxSamples = builder.comment(desc).defineInRange("maxSamples", 100000, 0, 100000000);

            desc = "If this option is enabled, then the compass will clear the cache of previously found structures when the structure couldn't found.";
            cleanCache = builder.comment(desc).define("cleanCache", false);

            desc = "If this option is enabled, then the compass will search alex cave";
            couldFoundAlexCaves = builder.comment(desc).define("couldFoundAlexCaves", true);

            desc = "Load config.json (custom model data per structure, see config/godofthings/config.json)";
            customResource = builder.comment(desc).define("customResource", false);

            builder.pop();
        }
    }

    /** 纯客户端配置（HUD 显示） */
    public static class Client
    {
        public final ModConfigSpec.BooleanValue displayWithChatOpen;
        public final ModConfigSpec.BooleanValue translateStructureNames;
        public final ModConfigSpec.EnumValue<OverlaySide> overlaySide;
        public final ModConfigSpec.IntValue overlayLineOffset;
        public final ModConfigSpec.BooleanValue showHaveFound;

        Client(ModConfigSpec.Builder builder)
        {
            String desc;
            builder.push("Client");

            desc = "Displays Explorer's Compass information on the HUD even while chat is open.";
            displayWithChatOpen = builder.comment(desc).define("displayWithChatOpen", true);

            desc = "Attempts to translate structure names before fixing the unlocalized names. Translations may not be available for all structures.";
            translateStructureNames = builder.comment(desc).define("translateStructureNames", true);

            desc = "The line offset for information rendered on the HUD.";
            overlayLineOffset = builder.comment(desc).defineInRange("overlayLineOffset", 1, 0, 50);

            desc = "The side for information rendered on the HUD. Ex: LEFT, RIGHT";
            overlaySide = builder.comment(desc).defineEnum("overlaySide", OverlaySide.LEFT);

            desc = "If this option is enabled, then show the structures that have been searched in the past.";
            showHaveFound = builder.comment(desc).define("showHaveFound", true);

            builder.pop();
        }
    }
}
