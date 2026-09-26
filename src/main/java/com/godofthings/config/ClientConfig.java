package com.godofthings.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * 客户端配置（存在 {@code config/godofthings-client.toml}）。
 * <p>
 * 目前只用于记住「配置界面上次停在哪一页」，这样重新打开界面时回到上次离开的页，
 * 而不是每次都跳回第一页（用户要求）。关游戏再进也保留。
 */
public class ClientConfig
{
    // 注意初始化顺序：BUILDER 先声明；所有 define 必须在 build() 之前；SPEC 在最后构建。
    private static final Builder BUILDER = new Builder();
    public static final ModConfigSpec SPEC;

    /** 配置界面上次停留的标签页（0=基础属性 1=特殊增幅 2=套装功能） */
    public static final IntValue LAST_SKILL_TAB;

    static
    {
        BUILDER.push("gui");
        LAST_SKILL_TAB = BUILDER.comment("神之套装配置界面上次停留的标签页（0=基础属性 1=特殊增幅 2=套装功能）。")
                .defineInRange("lastSkillTab", 0, 0, 8);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ClientConfig() {}

    /** 读上次的标签页；配置尚未加载或读取失败时回退到 0（绝不因配置问题崩界面）。 */
    public static int getLastTab()
    {
        try
        {
            return Math.max(0, LAST_SKILL_TAB.get());
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    /** 记住当前标签页并落盘。 */
    public static void setLastTab(int tab)
    {
        try
        {
            LAST_SKILL_TAB.set(Math.max(0, tab));
            SPEC.save();
        }
        catch (Exception ignored)
        {
            // 配置写入失败不影响游戏（下次仍回到默认页）
        }
    }
}
