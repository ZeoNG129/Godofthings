package com.godofthings.wt.terminals;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

/**
 * 终端界面的槽位语义（照抄 ae2wtlib 的 {@code AE2wtlibSlotSemantics}，[本地改动]
 * 前缀改为 GODOFTHINGS_ 避免与上游同装时冲突）。
 */
public class WtSlotSemantics
{
    private WtSlotSemantics()
    {
    }

    public static final SlotSemantic HELMET = register("HELMET");
    public static final SlotSemantic CHESTPLATE = register("CHESTPLATE");
    public static final SlotSemantic LEGGINGS = register("LEGGINGS");
    public static final SlotSemantic BOOTS = register("BOOTS");

    public static final SlotSemantic OFFHAND = register("OFFHAND");

    public static final SlotSemantic TRASH = register("TRASH");
    public static final SlotSemantic SINGULARITY = register("SINGULARITY");

    public static final SlotSemantic PICKUP_CONFIG = register("PICKUP_CONFIG");
    public static final SlotSemantic INSERT_CONFIG = register("INSERT_CONFIG");

    private static SlotSemantic register(String id)
    {
        return SlotSemantics.register("GODOFTHINGS_" + id, false);
    }
}
