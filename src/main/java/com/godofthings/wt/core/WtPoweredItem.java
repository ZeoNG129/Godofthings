package com.godofthings.wt.core;

import java.util.function.DoubleSupplier;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.items.tools.powered.WirelessTerminalItem;

/**
 * 无耗能无线终端物品基类（继承 AE2 的 {@link WirelessTerminalItem}——与上游 ItemWT 的
 * 继承位置一致，[本地改动] 即「无耗能五连」的落点：hasPower / usePower 恒 true、
 * 充电速率 0、当前电量恒满、extractAEPower 恒满足——所有无线终端（含通用终端）统一走这里，
 * 菜单闲置耗电与终端内操作耗电全部免除）。
 */
public abstract class WtPoweredItem extends WirelessTerminalItem
{
    public WtPoweredItem(DoubleSupplier capacity, Properties props)
    {
        super(capacity, props);
    }

    @Override
    public boolean hasPower(Player player, double amt, ItemStack stack)
    {
        return true;
    }

    @Override
    public boolean usePower(Player player, double amt, ItemStack stack)
    {
        return true;
    }

    @Override
    public double getChargeRate(ItemStack stack)
    {
        return 0;
    }

    @Override
    public double getAECurrentPower(ItemStack stack)
    {
        return getAEMaxPower(stack);
    }

    @Override
    public double extractAEPower(ItemStack stack, double amt, Actionable mode)
    {
        return amt;
    }
}
