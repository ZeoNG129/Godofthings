package com.godofthings.infinitecell;

// 基于 ExtendedAE（作者 glodblock，LGPL-3.0）的 InfinityCellInventory 移植：
// https://github.com/GLondon/ExtendedAE
// ME 无限存储元件的存储逻辑：对绑定的那一种资源**无限存、无限取**，
// 不消耗元件耐久、不写任何 NBT（persist 为空操作，拆下重放数据不变），
// 网络里始终显示 21.4 亿个该资源（见 InfinityCellItem.getAsIntMax）。

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class InfinityCellInventory implements StorageCell
{
    /** 闲置耗电（AE/t），取上游 ExtendedAE 的默认配置值 8.0 */
    public static final double IDLE_DRAIN = 8.0D;

    private final ItemStack stack;
    private final AEKey record;

    public static final ICellHandler HANDLER = new Handler();

    public InfinityCellInventory(ItemStack stack)
    {
        if (!(stack.getItem() instanceof InfinityCellItem))
        {
            throw new IllegalArgumentException("Cell isn't an infinity cell!");
        }
        this.stack = stack;
        this.record = ((InfinityCellItem) stack.getItem()).getRecord();
    }

    @Override
    public CellState getStatus()
    {
        return CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain()
    {
        return IDLE_DRAIN;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        if (this.record.equals(what))
        {
            return amount;
        }
        return 0;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        if (this.record.equals(what))
        {
            return amount;
        }
        return 0;
    }

    @Override
    public void persist()
    {
        // NO-OP：无限元件不保存任何数据
    }

    @Override
    public Component getDescription()
    {
        return this.stack.getHoverName();
    }

    @Override
    public void getAvailableStacks(KeyCounter out)
    {
        out.add(this.record, InfinityCellItem.getAsIntMax(this.record));
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source)
    {
        return this.record.equals(what);
    }

    private static class Handler implements ICellHandler
    {

        @Override
        public boolean isCell(ItemStack is)
        {
            return is != null && is.getItem() instanceof InfinityCellItem;
        }

        @Override
        public @Nullable StorageCell getCellInventory(ItemStack is, @Nullable ISaveProvider host)
        {
            return isCell(is) ? new InfinityCellInventory(is) : null;
        }
    }

}
