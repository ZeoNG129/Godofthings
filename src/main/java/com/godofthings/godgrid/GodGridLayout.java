package com.godofthings.godgrid;

import com.godofthings.item.GodAcceleratorItem;

/**
 * 神之机器统一网格界面的布局参数 —— <b>一比一照抄</b>
 * Productive Bees Genesis 的「无限通用机械离心机工厂」
 * （{@code productivebeesgenesis:infinite_extra_mek_centrifuge_factory}）。
 *
 * <p>原版几何（ME 无限级，17 进程）：底图宽 = 176 + 36*(3+2) + 2*3 = <b>362</b>，
 * 槽位 {@code baseX=27}、{@code pitch=19}、输入 {@code y=17}、输出三排 {@code y=57/77/97}、
 * 物品栏 {@code y=135} 居中、面板高 <b>218</b>。这里原样照搬，只把原版左侧的
 * 流体罐位置（18×30，位于 x=7,y=84）换成 1 格神之加速槽。</p>
 *
 * <p>原版的多流体 / PB 升级 / 排序 / Mek 升级槽全部删除，因此界面上只在面板外侧保留
 * 左右两条竖排按钮（与截图一致）：左侧「面配置」，右侧「AE」「排序」。</p>
 *
 * <p><b>贴图</b>：原版槽框由 Mekanism 的 dynamicSlots 程序化绘制，没有整张背景图；
 * 这里同样全部用 {@link net.minecraft.client.gui.GuiGraphics#fill} 画，不 blit，
 * 因此不受本项目「GUI 贴图必须 256×256」的约定限制（本界面宽 362 也塞不进 256）。</p>
 */
public final class GodGridLayout
{
    /** 列数（= 原版 17 进程） */
    public static final int COLS = 17;

    /** 槽位边长 */
    public static final int SLOT = 18;

    /** 列距（原版 baseXMult = 19） */
    public static final int PITCH = 19;

    /** 第一列槽位 X（原版 baseX = 27） */
    public static final int BASE_X = 27;

    public static final int INPUT_COUNT = COLS;
    public static final int OUTPUT_ROWS = 1;
    public static final int OUTPUT_COUNT = COLS * OUTPUT_ROWS;
    public static final int MACHINE_SLOTS = INPUT_COUNT + OUTPUT_COUNT;

    // ---- 原版 Y 坐标 ----

    /** 输入行（原版 y=17） */
    public static final int INPUT_Y = 17;

    /** 进度箭头（原版 GuiProgress 在 y=33） */
    public static final int ARROW_Y = 33;
    public static final int ARROW_H = 20;

    /** 输出三排（原版 y=57 / 77 / 97） */
    public static final int OUTPUT_Y0 = 57;
    public static final int OUTPUT_ROW_PITCH = 20;

    /** 物品栏（原版 getInventoryYOffset = 135） */
    public static final int INVENTORY_Y = 105;
    public static final int HOTBAR_OFFSET = 58;

    // ---- 神之加速槽：占用原版流体罐的位置（x=7, y=84） ----

    public static final int ACCEL_X = 7;
    public static final int ACCEL_Y = 84;

    // ---- 面板尺寸（原版 ME 无限级） ----

    public static final int IMAGE_W = 362;
    public static final int IMAGE_H = 188;

    /** 原版 getInventoryXOffset：8 + (imageWidth/2 - 90) */
    public static final int INVENTORY_X = 8 + (IMAGE_W / 2 - 90);

    // ---- 面板外的左右竖排按钮（照截图的观感） ----

    public static final int TAB_SIZE = 22;
    public static final int TAB_LEFT_X = -26;
    public static final int TAB_RIGHT_X = IMAGE_W + 4;
    public static final int TAB_FACE_Y = 9;
    public static final int TAB_AE_Y = 9;
    public static final int TAB_SORT_Y = 33;

    // ---- 配色（照原版红/蓝槽框） ----
    public static final int COLOR_PANEL = 0xFFC6C6C6;
    public static final int COLOR_PANEL_LIGHT = 0xFFFFFFFF;
    public static final int COLOR_PANEL_DARK = 0xFF555555;
    public static final int COLOR_SLOT_INNER = 0xFF2A2F3A;
    public static final int COLOR_INPUT_BORDER = 0xFFB03A3A;
    public static final int COLOR_OUTPUT_BORDER = 0xFF3A5FBE;
    public static final int COLOR_ACCEL_BORDER = 0xFF7A5C1E;
    public static final int COLOR_ARROW = 0xFF9AA6B8;
    public static final int COLOR_TEXT = 0xFFE8ECF4;
    public static final int COLOR_BUTTON_BG = 0xFF3A3A3A;
    public static final int COLOR_BUTTON_BG_HOVER = 0xFF5A5A5A;

    /** 内置并行基数：等价 PBG「满级速度 + 6 级 STACK（2^6）」的每列每 tick 操作数 */
    public static final int BUILTIN_PARALLEL = 64;


    /** 加速槽满一组（64 个神之加速）的档位：单轮处理量直接拉到 int 上限 */
    public static final int ACCEL_FULL_STACK = 64;
    private GodGridLayout()
    {
    }

    /** 第 col 列槽位左边缘 X = 27 + 19*col */
    public static int columnX(int col)
    {
        return BASE_X + col * PITCH;
    }

    public static int inputX(int i)
    {
        return columnX(i);
    }

    public static int outputX(int j)
    {
        return columnX(j % COLS);
    }

    public static int outputY(int j)
    {
        return OUTPUT_Y0 + (j / COLS) * OUTPUT_ROW_PITCH;
    }

    public static int accelX()
    {
        return ACCEL_X;
    }

    /**
     * 单列每 tick 的并行操作数 = 内置 64 × 神之加速倍率。
     *
     * <p>PBG 的 STACK 语义是 <code>min(2^N, 速度允许的轮数)</code>；按你的要求以
     * 「原版插满速度升级」为前提，该值即为上限本身。17 列各算一份，与原版一致。</p>
     */
    public static int operationsPerColumn(int accelCount)
    {
        return (int) Math.min(Integer.MAX_VALUE,
                (long) BUILTIN_PARALLEL * GodAcceleratorItem.multiplierFor(accelCount));
    }

    /**
     * 单轮处理量 —— 五台产出机器（熔炉 / 去皮 / 资源三机 / 怪蛋 / 掉落）的统一口径（v5.18.8）。
     *
     * <ul>
     *   <li>加速槽放满 64 个神之加速（{@link #ACCEL_FULL_STACK}）→ 单轮 = int 上限
     *       {@link Integer#MAX_VALUE}（2,147,483,647）；</li>
     *   <li>不满 64 个 → 沿用各机器原有公式 {@code builtinBase × multiplier}。</li>
     * </ul>
     *
     * <p>乘算全程用 long：{@code builtinBase} 最大 int 上限、{@code multiplier} 最大 1008（63 × 16），
     * 乘积约 2.16e12，远小于 long 上限，不会溢出。</p>
     *
     * @param accelCount  加速槽里的神之加速数量
     * @param builtinBase 内置基数（熔炉 / 去皮 = {@link #BUILTIN_PARALLEL}；资源 / 怪蛋 / 掉落 = 单份产出数量）
     * @param multiplier  并行倍率（{@link GodAcceleratorItem#multiplierFor(int)}）
     * @return 单轮最多处理的份数（long）
     */
    public static long roundBatch(int accelCount, int builtinBase, long multiplier)
    {
        if (accelCount >= ACCEL_FULL_STACK)
        {
            return Integer.MAX_VALUE;
        }
        return (long) builtinBase * multiplier;
    }
}
