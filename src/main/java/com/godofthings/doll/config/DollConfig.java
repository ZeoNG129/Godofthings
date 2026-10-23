package com.godofthings.doll.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 玩偶系统的服务端配置。
 *
 * <p>四项配置与原 mod 的 {@code StretcherConfig} 玩偶相关段一一对应：
 * 树叶掉落开关与概率、星光庭院邀请开关、时间停止彩蛋开关。</p>
 */
public final class DollConfig
{
    public static final ModConfigSpec SPEC;

    /** 是否允许打树叶独立掉落玩偶 */
    public static final ModConfigSpec.BooleanValue LEAF_DROP;
    /** 每次打树叶掉落玩偶的概率（0.0001 = 万分之一） */
    public static final ModConfigSpec.DoubleValue LEAF_DROP_PROBABILITY;
    /** 星光庭院邀请开关 */
    public static final ModConfigSpec.BooleanValue STARFIELD_INVITATION;
    /** 时间停止彩蛋开关（时间停止系统未移植，此项目前只控制一个恒假分支） */
    public static final ModConfigSpec.BooleanValue TIME_STOP;

    static
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("玩偶相关设置").push("doll");

        LEAF_DROP = builder
                .comment("是否允许打树叶独立掉落紬紬玩偶；每位玩家每个存档只领取一次。")
                .translation("godofthings.configuration.doll.leaf_drop")
                .define("leaf_drop", true);

        LEAF_DROP_PROBABILITY = builder
                .comment("每次打树叶独立获得玩偶的概率，0.0001 为万分之一。")
                .translation("godofthings.configuration.doll.leaf_drop_probability")
                .defineInRange("leaf_drop_probability", 1.0E-4D, 0.0D, 1.0D);

        STARFIELD_INVITATION = builder
                .comment("是否允许玩偶发出星光庭院邀请和重游，默认开启。")
                .translation("godofthings.configuration.doll.starfield_invitation")
                .define("starfield_invitation", true);

        TIME_STOP = builder
                .comment("是否允许玩偶的时间停止恶作剧彩蛋（当前版本未实现时间停止，占位项）。")
                .translation("godofthings.configuration.doll.time_stop")
                .define("time_stop", true);

        builder.pop();
        SPEC = builder.build();
    }

    private DollConfig()
    {
    }

    /** 配置未加载时回退默认值，避免早期调用抛 IllegalStateException */
    private static boolean value(ModConfigSpec.BooleanValue v, boolean fallback)
    {
        try
        {
            return v.get();
        }
        catch (IllegalStateException e)
        {
            return fallback;
        }
    }

    private static double value(ModConfigSpec.DoubleValue v, double fallback)
    {
        try
        {
            return v.get();
        }
        catch (IllegalStateException e)
        {
            return fallback;
        }
    }

    public static boolean leafDropEnabled()
    {
        return value(LEAF_DROP, true);
    }

    public static double leafDropProbability()
    {
        return value(LEAF_DROP_PROBABILITY, 1.0E-4D);
    }

    public static boolean starfieldInvitationEnabled()
    {
        return value(STARFIELD_INVITATION, true);
    }

    public static boolean serverTimeStop()
    {
        return value(TIME_STOP, true);
    }
}
