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

            desc = "允许玩家传送到已定位的结构（创造模式 / OP / 作弊模式可用）。";
            allowTeleport = builder.comment(desc).define("allowTeleport", true);

            desc = "在 HUD 上显示已定位结构的精确坐标与距离，而不是只依赖罗盘指向。";
            displayCoordinates = builder.comment(desc).define("displayCoordinates", true);

            desc = "搜索结构的最大半径。提高该值会提升搜索精度，但可能显著增加资源开销。";
            maxRadius = builder.comment(desc).defineInRange("maxRadius", 10000, 0, 1000000);

            desc = "罗盘不显示也无法搜索的结构列表（资源位置）。通配符 * 匹配任意字符，? 匹配单个字符。例：[\"minecraft:stronghold\", \"minecraft:endcity\", \"minecraft:*village*\"]";
            structureBlacklist = builder.comment(desc).define("structureBlacklist", new ArrayList<>());

            desc = "搜索结构时的最大采样数。";
            maxSamples = builder.comment(desc).defineInRange("maxSamples", 100000, 0, 100000000);

            desc = "开启后，找不到目标结构时会清空已找到结构的缓存。";
            cleanCache = builder.comment(desc).define("cleanCache", false);

            desc = "开启后罗盘会搜索 Alex 的洞穴模组。";
            couldFoundAlexCaves = builder.comment(desc).define("couldFoundAlexCaves", true);

            desc = "加载 config.json（每结构的自定义模型数据，见 config/godofthings/config.json）";
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

            desc = "聊天栏打开时仍在 HUD 显示探险者指南针信息。";
            displayWithChatOpen = builder.comment(desc).define("displayWithChatOpen", true);

            desc = "修正非本地化名称前先尝试翻译结构名。并非所有结构都有译文。";
            translateStructureNames = builder.comment(desc).define("translateStructureNames", true);

            desc = "HUD 信息渲染的行偏移量。";
            overlayLineOffset = builder.comment(desc).defineInRange("overlayLineOffset", 1, 0, 50);

            desc = "HUD 信息渲染在屏幕哪一侧。例：LEFT / RIGHT";
            overlaySide = builder.comment(desc).defineEnum("overlaySide", OverlaySide.LEFT);

            desc = "开启后显示过去搜索过的结构。";
            showHaveFound = builder.comment(desc).define("showHaveFound", true);

            builder.pop();
        }
    }
}
