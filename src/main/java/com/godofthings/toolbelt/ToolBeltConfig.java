package com.godofthings.toolbelt;

import com.godofthings.toolbelt.belt.ToolBeltItem;
import net.minecraft.world.item.ItemStack;

/**
 * 工具皮带（Tool Belt）配置：以常量形式提供（无需 ForgeConfigSpec 配置文件）。
 * 原项目：Tool Belt（gigaherz，BSD 3-Clause）。
 *
 * 用户要求：工具皮带默认 9 格，且不再需要皮带包升级，因此相关升级逻辑全部移除，
 * 皮带容量固定为 9。
 */
public final class ToolBeltConfig
{
    /** 工具皮带物品栏固定 9 格。 */
    public static final int BELT_SLOTS = 9;

    // ---- 客户端显示 ----
    public static boolean showBeltOnPlayers = true;
    public static float beltItemScale = 0.5f;

    // ---- 径向菜单 ----
    public static boolean releaseToSwap = false;
    public static boolean clipMouseToCircle = false;
    public static boolean allowClickOutsideBounds = false;
    public static boolean displayEmptySlots = false;

    // ---- 自定义皮带栏（未装 Curios 时的兜底方案，本移植始终启用） ----
    public static final boolean customBeltSlotEnabled = true;

    private ToolBeltConfig() {}

    /**
     * 判断物品是否可放入皮带。默认只允许不可堆叠的物品（工具/武器等），
     * 且禁止皮带套皮带。
     */
    public static boolean isItemStackAllowed(ItemStack stack)
    {
        if (stack.getCount() <= 0)
            return true;

        if (stack.getItem() instanceof ToolBeltItem)
            return false;

        return stack.getMaxStackSize() == 1;
    }
}
