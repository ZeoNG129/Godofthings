package com.godofthings.godgrid;

import java.util.HashMap;
import java.util.Map;

/**
 * 7 台共用网格框架的机器的界面几何 —— 每台的槽位行列按自己的 UI 原画定，但
 * <b>玩家物品栏一律用 MC 标准 18 格距</b>（v5.31.1 修正）。
 *
 * <p><b>为什么物品栏必须 18 格距</b>：v5.30.0 给几台机器用了 15/16/17 的紧凑格距，
 * 结果 4 行（3 背包 + 1 快捷栏）挤在 60~68px 里，而 Slot 本身按 16px 图标绘制，
 * 视觉上就出现「槽位压在底板上、整体偏上」的错位。现在统一 18 格距 +
 * hotbarOffset 58（MC 标准），底板与槽位就永远对得上。</p>
 *
 * <p><b>加速槽统一在左上角 (10, 6)</b>：所有机器同一位置，符合「和其他机器一样在左上角」。</p>
 */
public final class GodGridLayouts
{
    /** 加速槽的统一位置（面板左上角，标题铭牌左侧的空位） */
    private static final int ACCEL_X = 10;
    private static final int ACCEL_Y = 6;

    private GodGridLayouts()
    {
    }

    // ---------------- 01 炽神圣火熔炉 ----------------
    /** 上排 17 格输入（y=40）、下排 15 格输出（y=172）；面板 335×344。 */
    private static final GodGridLayoutSpec FURNACE = GodGridLayoutSpec
            .builder("god_furnace", 335, 344)
            .inputs(17, 1, 17, 40)
            .outputs(15, 1, 17, 172)
            .slot(18, 18, 18)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(87, 262, 58, 18)
            .arrow(160, 108)
            .texture(335, 344)
            .build();

    // ---------------- 03 点金圣灵秘柜 ----------------
    /** 上排 14 格输入（y=52）、下排 16 格输出（y=217）；面板 324×330。 */
    private static final GodGridLayoutSpec ORE_MACHINE = GodGridLayoutSpec
            .builder("god_ore_machine", 324, 330)
            .inputs(14, 1, 31, 52)
            .outputs(16, 1, 23, 217)
            .slot(18, 18, 18)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(82, 248, 58, 18)
            .arrow(155, 120)
            .texture(324, 330)
            .build();

    // ---------------- 04 丰饶母神温床 ----------------
    /**
     * 左 3×4 = 12 格输入、下带 8×2 = 16 格输出；面板 256×256。
     * 输入 x=20/44/69、y=50/69/89（格距 25/19）；输出 x=10 起、格距 25、y=128。
     */
    private static final GodGridLayoutSpec CROP_MACHINE = GodGridLayoutSpec
            .builder("god_crop_machine", 256, 256)
            .inputs(3, 4, 20, 50)
            .outputs(8, 2, 10, 128)
            .slot(18, 25, 19)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(48, 174, 58, 18)
            .arrow(150, 108)
            .texture(256, 256)
            .build();

    // ---------------- 05 创生摹刻圣坛 ----------------
    /** 上排 14 格输入（y=60）、下排 15 格输出（y=186）；面板 310×298。 */
    private static final GodGridLayoutSpec DUPLICATE_MACHINE = GodGridLayoutSpec
            .builder("god_duplicate_machine", 310, 298)
            .inputs(14, 1, 16, 60)
            .outputs(15, 1, 14, 186)
            .slot(18, 18, 18)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(75, 216, 58, 18)
            .arrow(150, 128)
            .texture(310, 298)
            .build();

    // ---------------- 06 净木剥相祭台 ----------------
    /**
     * 左 3×3 = 9 格输入（x=22/46/70、y=46/70/94）、右 3×3 = 9 格输出（x=142/166/190）；
     * 面板 256×256。输出补到 9 格是因为去皮机「输入 i 一一对应输出 i」的语义
     * （{@code convertOne}，GameTest 的 9 槽独立测试依赖这个配对）。
     */
    private static final GodGridLayoutSpec PEELER = GodGridLayoutSpec
            .builder("god_peeler", 256, 256)
            .inputs(3, 3, 22, 46)
            .outputs(3, 3, 142, 46)
            .slot(18, 24, 24)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(48, 174, 58, 18)
            .arrow(122, 126)
            .texture(256, 256)
            .build();

    // ---------------- 07 万魂余响鉴录仪 ----------------
    /** 上排 15 格输入（y=47）、下排 15 格输出（y=165）；面板 297×302。 */
    private static final GodGridLayoutSpec DROP = GodGridLayoutSpec
            .builder("god_drop", 297, 302)
            .inputs(15, 1, 16, 47)
            .outputs(15, 1, 16, 165)
            .slot(18, 18, 18)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(68, 220, 58, 18)
            .arrow(141, 112)
            .texture(297, 302)
            .build();

    // ---------------- 08 灵胚孳生圣卵 ----------------
    /** 上排 15 格输入（y=41）、下排 15 格输出（y=139）；面板 283×261。 */
    private static final GodGridLayoutSpec SPAWN_EGG = GodGridLayoutSpec
            .builder("god_spawn_egg", 283, 261)
            .inputs(15, 1, 12, 41)
            .outputs(15, 1, 12, 139)
            .slot(18, 18, 18)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(61, 179, 58, 18)
            .arrow(134, 98)
            .texture(283, 261)
            .build();

    private static final Map<String, GodGridLayoutSpec> BY_ID = new HashMap<>();

    static
    {
        BY_ID.put("god_furnace", FURNACE);
        BY_ID.put("god_ore_machine", ORE_MACHINE);
        BY_ID.put("god_crop_machine", CROP_MACHINE);
        BY_ID.put("god_duplicate_machine", DUPLICATE_MACHINE);
        BY_ID.put("god_peeler", PEELER);
        BY_ID.put("god_drop", DROP);
        BY_ID.put("god_spawn_egg", SPAWN_EGG);
    }

    /**
     * 按注册名取布局。找不到时回退熔炉（保证永不 NPE）；7 台网格机都会命中。
     */
    public static GodGridLayoutSpec of(String id)
    {
        GodGridLayoutSpec spec = BY_ID.get(id);
        return spec != null ? spec : FURNACE;
    }
}
