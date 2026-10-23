package com.godofthings.godgrid;

import java.util.HashMap;
import java.util.Map;

/**
 * 7 台共用网格框架的机器的界面几何 —— 每台的槽位行列与坐标全部按<b>自己的 UI 原画</b>逐像素量出来。
 *
 * <p><b>为什么每台各不相同</b>：重绘资源包 18 张概念原画里，这 7 台的槽位排布完全不一样
 * （熔炉上 17 下 15、秘柜上 14 下 16、温床左右各 3×4 加下带 8×2、祭台左 3×3 右 3×2……）。
 * v5.30.0 之前所有机器共用一套 17+17 的 {@link GodGridLayout}，对不上任何一张原画。</p>
 *
 * <p><b>放大系数（用户选 A：忠于原画）</b>：原画 256×256 里槽位格距只有 13~18px，
 * 而 MC 物品图标固定 16×16、槽位标准 18px。所以底图按
 * {@code 18 / 最小原画格距} 等比放大，槽位一律用标准 18px 画在放大后的坐标上。
 * 代价是面板会超出 GUI 缩放 4 的可视高度，玩家需要把界面缩放调到 3 或更小。</p>
 *
 * <p><b>槽位口径</b>（用户定）：原画<b>上排/左块 = 输入</b>，<b>下排/右块 = 输出</b>。</p>
 */
public final class GodGridLayouts
{
    private GodGridLayouts()
    {
    }

    // ---------------- 01 炽神圣火熔炉 ----------------
    /**
     * 上排 17 格输入（原画 x=13 起、格距 13.4、y=30）；下排 15 格输出（x=13 起、格距 15.4、y=128）。
     * 放大 1.34×，面板 335×344。
     */
    private static final GodGridLayoutSpec FURNACE = GodGridLayoutSpec
            .builder("god_furnace", 335, 344)
            .inputs(17, 1, 17, 40)
            .outputs(15, 1, 17, 172)
            .slot(18, 18, 18)
            .accel(0, 0)
            .inventory(101, 236, 50, 17)
            .arrow(160, 108)
            .texture(335, 344)
            .build();

    // ---------------- 03 点金圣灵秘柜 ----------------
    /**
     * 上排 14 格输入（x=24 起、格距 15.3、y=40）；下排 16 格输出（x=18 起、格距 14.0、y=168）。
     * 放大 1.29×，面板 324×330。
     */
    private static final GodGridLayoutSpec ORE_MACHINE = GodGridLayoutSpec
            .builder("god_ore_machine", 324, 330)
            .inputs(14, 1, 31, 52)
            .outputs(16, 1, 23, 217)
            .slot(18, 18, 18)
            .accel(0, 0)
            .inventory(96, 250, 50, 17)
            .arrow(155, 120)
            .texture(324, 330)
            .build();

    // ---------------- 04 丰饶母神温床 ----------------
    /**
     * 左 3×4 = 12 格输入（x=20/44/69、y=50/69/89，格距 25）；右 3×4 = 12 格输出（x=167/192/216）；
     * 下带 8×2 = 16 格作为输出扩展区。原画格距 25 已经大于 18，故不放大（0.72× 会缩小，这里取 1× 并用原坐标）。
     * 面板 256×256。
     */
    private static final GodGridLayoutSpec CROP_MACHINE = GodGridLayoutSpec
            .builder("god_crop_machine", 256, 256)
            .inputs(3, 4, 20, 50)
            .outputs(8, 2, 10, 128)
            .slot(18, 25, 19)
            .accel(119, 116)
            .inventory(38, 176, 40, 15)
            .arrow(128, 108)
            .texture(256, 256)
            .build();

    // ---------------- 05 创生摹刻圣坛 ----------------
    /**
     * 上排 14 格输入（x=14 起、格距 16.2、y=52）；下排 15 格输出（x=12 起、格距 15.5、y=160）。
     * 放大 1.16×，面板 310×298。
     */
    private static final GodGridLayoutSpec DUPLICATE_MACHINE = GodGridLayoutSpec
            .builder("god_duplicate_machine", 310, 298)
            .inputs(14, 1, 16, 60)
            .outputs(15, 1, 14, 186)
            .slot(18, 18, 18)
            .accel(0, 0)
            .inventory(89, 214, 46, 17)
            .arrow(150, 118)
            .texture(310, 298)
            .build();

    // ---------------- 06 净木剥相祭台 ----------------
    /**
     * 左 3×3 = 9 格输入（x=22/46/70、y=46/70/94，格距 24）；右 3×3 = 9 格输出（x=142/166/190，同 y）。
     *
     * <p><b>为什么左右并排且输出 9 格</b>：①去皮机的核心语义是「输入 i 一一对应输出 i」
     * （`convertOne`，GameTest 的 9 槽独立测试依赖这个配对），输入输出格数必须相等，
     * 原画右块只画了 3×2=6 格，补到 3×3；②原画是木质立柜，但木板贴图在 256 宽的面板里
     * 塞不下上下两排，故改为左右并排 —— 这样上下还留出空间给中央徽记与玩家栏。</p>
     */
    private static final GodGridLayoutSpec PEELER = GodGridLayoutSpec
            .builder("god_peeler", 256, 256)
            .inputs(3, 3, 22, 46)
            .outputs(3, 3, 142, 46)
            .slot(18, 24, 24)
            .accel(0, 0)
            .inventory(56, 128, 64, 16)
            .arrow(122, 122)
            .texture(256, 256)
            .build();

    // ---------------- 07 万魂余响鉴录仪 ----------------
    /**
     * 上排 15 格输入（x=14 起、格距 15.3、y=40）；下排 15 格输出（y=140）。
     * 放大 1.18×，面板 297×302。
     */
    private static final GodGridLayoutSpec DROP = GodGridLayoutSpec
            .builder("god_drop", 297, 302)
            .inputs(15, 1, 16, 47)
            .outputs(15, 1, 16, 165)
            .slot(18, 18, 18)
            .accel(0, 0)
            .inventory(75, 200, 50, 17)
            .arrow(145, 112)
            .texture(297, 302)
            .build();

    // ---------------- 08 灵胚孳生圣卵 ----------------
    /**
     * 上排 15 格输入（x=2 起、格距 17.7、y=40）；下排 15 格输出（y=136）。
     * 放大 1.02×，面板 283×261。
     */
    private static final GodGridLayoutSpec SPAWN_EGG = GodGridLayoutSpec
            .builder("god_spawn_egg", 283, 261)
            .inputs(15, 1, 12, 41)
            .outputs(15, 1, 12, 139)
            .slot(18, 18, 18)
            .accel(0, 0)
            .inventory(66, 168, 50, 17)
            .arrow(141, 98)
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
