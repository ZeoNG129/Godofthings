package com.godofthings.doll.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 玩偶系统的服务端配置。
 *
 * <p>只保留移植后仍然有意义的两项：树叶掉落开关与概率。原 mod 还有
 * {@code starfield_invitation}，但星光庭院维度没有移植过来，那一项已无意义，故删除。</p>
 */
public final class DollConfig
{
    public static final ModConfigSpec SPEC;

    /** 是否允许打树叶独立掉落玩偶 */
    public static final ModConfigSpec.BooleanValue LEAF_DROP;
    /** 每次打树叶掉落玩偶的概率（0.0001 = 万分之一） */
    public static final ModConfigSpec.DoubleValue LEAF_DROP_PROBABILITY;

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

        builder.pop();
        SPEC = builder.build();
    }

    private DollConfig()
    {
    }

    /** 树叶掉落是否启用（配置未加载时默认启用，避免早期调用 NPE） */
    public static boolean leafDropEnabled()
    {
        try
        {
            return LEAF_DROP.get();
        }
        catch (IllegalStateException e)
        {
            return true;
        }
    }

    /** 树叶掉落概率（配置未加载时回退到万分之一） */
    public static double leafDropProbability()
    {
        try
        {
            return LEAF_DROP_PROBABILITY.get();
        }
        catch (IllegalStateException e)
        {
            return 1.0E-4D;
        }
    }
}
