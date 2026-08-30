package com.godofthings.generator;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * 能量发电机配置。
 * <p>
 * 初始发电量 100 FE/t，每 1 秒发电量增加 100 FE/t，最大发电量 Long.MAX_VALUE。
 * 移植自 auto-resource（LGPL-3.0），按需求改为固定数值。
 */
public final class EnergyGenConfig
{
    private EnergyGenConfig() {}

    /** 初始发电量（FE/t） */
    public static final long MIN = 100L;

    /** 最大发电量（FE/t） */
    public static final long MAX = Long.MAX_VALUE;

    /** 每隔多少秒增长一次 */
    public static final long SECOND = 1L;

    /** 每次增长的发电量（FE/t） */
    public static final long STEP = 100L;

    /** 加速增长所需物品（放入后每次增长变为当前发电量的 1%） */
    public static final Item STAR_ITEM = Items.NETHER_STAR;
}
