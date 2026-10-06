package com.godofthings.config;

import com.godofthings.measurement.MeasureLineColor;
import com.godofthings.measurement.MeasureTextColor;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * 客户端配置（存在 {@code config/godofthings-client.toml}）。
 * <p>
 * 「gui」段：记住「配置界面上次停在哪一页」，这样重新打开界面时回到上次离开的页，
 * 而不是每次都跳回第一页（用户要求）。关游戏再进也保留。
 * <p>
 * 「measurement」段：神之测量的线框 / 数字外观（v5.13.0 自 Measurements 移植）。
 */
public class ClientConfig
{
    // 注意初始化顺序：BUILDER 先声明；所有 define 必须在 build() 之前；SPEC 在最后构建。
    private static final Builder BUILDER = new Builder();
    public static final ModConfigSpec SPEC;

    /** 配置界面上次停留的标签页（0=神之增幅 1=神之共鸣 2=套装功能） */
    public static final IntValue LAST_SKILL_TAB;

    // ---- 神之测量 ----
    public static final EnumValue<MeasureLineColor> LINE_COLOR;
    public static final EnumValue<MeasureTextColor> TEXT_COLOR;
    public static final DoubleValue TEXT_SIZE;
    public static final DoubleValue LINE_WIDTH;
    public static final IntValue LINE_WIDTH_MAX;

    static
    {
        BUILDER.push("gui");
        // v5.12.1 范围收紧到 0-2（实际只有 3 页：增幅 / 共鸣 / 套装功能；旧存档若存了更大值由 getLastTab 兜底 clamp）
        LAST_SKILL_TAB = BUILDER.comment("神之套装配置界面上次停留的标签页（0=神之增幅 1=神之共鸣 2=套装功能）。 / Last selected tab of the God Armor config screen (0=Boosts 1=Resonance 2=Suit Functions).")
                .defineInRange("lastSkillTab", 0, 0, 2);
        BUILDER.pop();

        BUILDER.push("measurement");
        LINE_COLOR = BUILDER.comment("测量线框颜色（RANDOM=每个测量框随机）。 / Color of the measurement lines (RANDOM=random per box).")
                .defineEnum("lineColor", MeasureLineColor.YELLOW);
        TEXT_COLOR = BUILDER.comment("三轴长度数字颜色（RANDOM=随机 / XYZRGB=按轴红绿蓝）。 / Color of the length text (RANDOM / XYZRGB=RGB per axis).")
                .defineEnum("textColor", MeasureTextColor.YELLOW);
        TEXT_SIZE = BUILDER.comment("长度数字大小。 / Size of the length text.")
                .defineInRange("textSize", 0.02D, 0.01, 0.10);
        LINE_WIDTH = BUILDER.comment("线框粗细。 / Width (thickness) of the lines.")
                .defineInRange("lineWidth", 2.0D, 1, 16);
        LINE_WIDTH_MAX = BUILDER.comment("距离超过 48 格时的线框粗细。 / Line width when further away than 48 blocks.")
                .defineInRange("lineWidthMax", 2, 1, 16);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ClientConfig() {}

    /** 读上次的标签页；配置尚未加载 / 读取失败 / 超出页数范围时 clamp 回 0-2（绝不因配置问题崩界面）。 */
    public static int getLastTab()
    {
        try
        {
            return Math.floorMod(LAST_SKILL_TAB.get(), 3);
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    /** 记住当前标签页并落盘（只收 0-2）。 */
    public static void setLastTab(int tab)
    {
        try
        {
            LAST_SKILL_TAB.set(Math.floorMod(tab, 3));
            SPEC.save();
        }
        catch (Exception ignored)
        {
            // 配置写入失败不影响游戏（下次仍回到默认页）
        }
    }
}
