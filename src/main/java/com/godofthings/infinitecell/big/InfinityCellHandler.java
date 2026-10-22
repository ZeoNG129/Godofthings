package com.godofthings.infinitecell.big;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import net.minecraft.world.item.ItemStack;

/**
 * 把「吞噬万籁的寂静」注册成 AE2 认识的存储元件（AE2 的 {@code ICellHandler} 扩展点）。
 *
 * <p>移植自 ExtendedAE+ 1.6.3 的
 * {@code com.extendedae_plus.api.storage.InfinityBigIntegerCellHandler}
 * （作者 glodblock 等，LGPL-3.0）。注册点在 {@code Godofthings#commonSetup}，
 * 与既有的 {@code InfinityCellInventory.HANDLER} 并列。</p>
 */
public class InfinityCellHandler implements ICellHandler
{
    public static final InfinityCellHandler INSTANCE = new InfinityCellHandler();

    @Override
    public boolean isCell(ItemStack is)
    {
        return is != null && is.getItem() instanceof InfinityCellItem;
    }

    @Override
    public InfinityCellInventory getCellInventory(ItemStack is, ISaveProvider container)
    {
        return InfinityCellInventory.createInventory(is, container, InfinityCellStorage.get());
    }
}
