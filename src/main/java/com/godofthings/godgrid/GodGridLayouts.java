package com.godofthings.godgrid;

import java.util.HashMap;
import java.util.Map;

/**
 * 7 台共用网格框架的机器的界面几何。
 *
 * <p><b>v5.31.2 的两条硬规则</b>：</p>
 * <ol>
 *   <li><b>上排输入格数 = 下排输出格数</b>（用户要求「上下槽位数量一致」）。每台取原来两者中的
 *       较小值，然后重新居中：{@code x = (panelW - (n*18)) / 2}。</li>
 *   <li><b>玩家物品栏一律 MC 标准 18 格距</b>，且快捷栏就在 {@code invY + 4*18} ——
 *       与底图底板按 4×18 均匀排布画出来的第 4 行完全重合（之前用 hotbarOffset 58 差了 4px）。</li>
 * </ol>
 *
 * <p>加速槽统一在左上角 (10, 6)，7 台一致。</p>
 */
public final class GodGridLayouts
{
    /** 槽位边长 / 列距 / 行距（标准 MC 值） */
    private static final int S = 18;

    /** 加速槽位置（面板左上角，所有机器一致） */
    private static final int ACCEL_X = 10;
    private static final int ACCEL_Y = 6;

    private GodGridLayouts()
    {
    }

    /** 输入/输出区水平居中：{@code (panelW - n*S) / 2} */
    private static int centerX(int panelW, int n)
    {
        return (panelW - n * S) / 2;
    }

    /** 物品栏水平居中：9 格 × 18 格距 = 162 宽 */
    private static int invX(int panelW)
    {
        return (panelW - 9 * S) / 2 + 1;
    }

    /**
     * 物品栏纵向位置：底图里的底板占 {@code 4*18 + 8}（上下各 4px 内衬），
     * 面板底部再留 6px，所以 {@code invY = panelH - (4*18+8) - 6 + 4}。
     * 之前写成 {@code panelH - 4*18 - 8}，底板会被面板下沿裁掉一条。
     */
    private static int invY(int panelH)
    {
        return panelH - (4 * S + 8) - 6 + 4;
    }

    // ---------------- 01 炽神圣火熔炉 ----------------
    /** 上排 15 格输入 / 下排 15 格输出（原 17/15，取小者 15）；面板 335×344。 */
    private static final GodGridLayoutSpec FURNACE = GodGridLayoutSpec
            .builder("god_furnace", 335, 344)
            .inputs(15, 1, centerX(335, 15), 40)
            .outputs(15, 1, centerX(335, 15), 172)
            .slot(S, S, S)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(invX(335), invY(344), S)
            .arrow(160, 106)
            .texture(335, 344)
            .build();

    // ---------------- 03 点金圣灵秘柜 ----------------
    /** 上排 14 格 / 下排 14 格（原 14/16，取小者 14）；面板 324×330。 */
    private static final GodGridLayoutSpec ORE_MACHINE = GodGridLayoutSpec
            .builder("god_ore_machine", 324, 330)
            .inputs(14, 1, centerX(324, 14), 52)
            .outputs(14, 1, centerX(324, 14), 217)
            .slot(S, S, S)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(invX(324), invY(330), S)
            .arrow(155, 134)
            .texture(324, 330)
            .build();

    // ---------------- 04 丰饶母神温床 ----------------
    /**
     * 左 3×4 = 12 格输入、下带 4×3 = 12 格输出（原 12/16，取小者 12）；面板 256×256。
     * 输出仍横排在下方，只是列数从 8 收到 4、行数 2 增到 3。
     */
    private static final GodGridLayoutSpec CROP_MACHINE = GodGridLayoutSpec
            .builder("god_crop_machine", 256, 288)
            .inputs(3, 4, 20, 50)
            .outputs(4, 3, centerX(256, 4), 139)
            .slot(S, 25, 19)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(invX(256), invY(288), S)
            .arrow(196, 132)
            .texture(256, 288)
            .build();

    // ---------------- 05 创生摹刻圣坛 ----------------
    /** 上排 14 格 / 下排 14 格（原 14/15，取小者 14）；面板 310×298。 */
    private static final GodGridLayoutSpec DUPLICATE_MACHINE = GodGridLayoutSpec
            .builder("god_duplicate_machine", 310, 298)
            .inputs(14, 1, centerX(310, 14), 60)
            .outputs(14, 1, centerX(310, 14), 186)
            .slot(S, S, S)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(invX(310), invY(298), S)
            .arrow(148, 124)
            .texture(310, 298)
            .build();

    // ---------------- 06 净木剥相祭台 ----------------
    /**
     * 左 3×3 = 9 格输入、右 3×3 = 9 格输出（本来就是 9/9）；面板 256×256。
     * 输出必须与输入等量：去皮机「输入 i 一一对应输出 i」（{@code convertOne}，
     * GameTest 的 9 槽独立测试依赖这个配对）。
     */
    private static final GodGridLayoutSpec PEELER = GodGridLayoutSpec
            .builder("god_peeler", 256, 256)
            .inputs(3, 3, 22, 46)
            .outputs(3, 3, 142, 46)
            .slot(S, 24, 24)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(invX(256), invY(256), S)
            .arrow(122, 126)
            .texture(256, 256)
            .build();

    // ---------------- 07 万魂余响鉴录仪 ----------------
    /** 上排 15 格 / 下排 15 格（本来就是 15/15）；面板 297×302。 */
    private static final GodGridLayoutSpec DROP = GodGridLayoutSpec
            .builder("god_drop", 297, 302)
            .inputs(15, 1, centerX(297, 15), 47)
            .outputs(15, 1, centerX(297, 15), 165)
            .slot(S, S, S)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(invX(297), invY(302), S)
            .arrow(141, 110)
            .texture(297, 302)
            .build();

    // ---------------- 08 灵胚孳生圣卵 ----------------
    /** 上排 15 格 / 下排 15 格；面板 300×270（加宽到能放下 15 格并留边距）。 */
    private static final GodGridLayoutSpec SPAWN_EGG = GodGridLayoutSpec
            .builder("god_spawn_egg", 300, 270)
            .inputs(15, 1, centerX(300, 15), 41)
            .outputs(15, 1, centerX(300, 15), 145)
            .slot(S, S, S)
            .accel(ACCEL_X, ACCEL_Y)
            .inventory(invX(300), invY(270), S)
            .arrow(143, 100)
            .texture(300, 270)
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
