package com.godofthings.overloadedinterface;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

/**
 * 过载接口的分页槽位语义（移植自 AE2LT 的 OverloadedSlotSemantics）。
 *
 * <p>36 槽 = 2 页 × 每页 18 槽（CONFIG 两行 + STORAGE 两行）。
 * json 里每个语义只放一行的 9 个槽坐标，翻页时按语义组开关槽位。</p>
 */
public final class OverloadedSlotSemantics
{
    public static final SlotSemantic OL_1 = SlotSemantics.register("GOT_OL_1", false);
    public static final SlotSemantic OL_2 = SlotSemantics.register("GOT_OL_2", false);
    public static final SlotSemantic OL_3 = SlotSemantics.register("GOT_OL_3", false);
    public static final SlotSemantic OL_4 = SlotSemantics.register("GOT_OL_4", false);
    public static final SlotSemantic OL_5 = SlotSemantics.register("GOT_OL_5", false);
    public static final SlotSemantic OL_6 = SlotSemantics.register("GOT_OL_6", false);
    public static final SlotSemantic OL_7 = SlotSemantics.register("GOT_OL_7", false);
    public static final SlotSemantic OL_8 = SlotSemantics.register("GOT_OL_8", false);

    /** CONFIG 行语义：页 0 上/下、页 1 上/下。 */
    public static final SlotSemantic[] CONFIG_PATTERN = { OL_1, OL_3, OL_5, OL_7 };
    /** STORAGE 行语义：页 0 上/下、页 1 上/下。 */
    public static final SlotSemantic[] STORAGE_PATTERN = { OL_2, OL_4, OL_6, OL_8 };

    private OverloadedSlotSemantics()
    {
    }

    public static void init()
    {
    }
}
