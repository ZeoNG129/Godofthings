package com.godofthings.fluxcell;

import appeng.api.storage.cells.ICellHandler;
import appeng.api.storage.cells.ISaveProvider;
import net.minecraft.world.item.ItemStack;

/**
 * 把 {@link InfinityFluxCellItem} 注册成 AE2 认识的存储元件。
 *
 * <p>与项目里 {@code InfinityCellHandler} 同一套做法：实现 AE2 的 {@link ICellHandler} 扩展点，
 * 在 {@code Godofthings#commonSetup} 里挂到 {@code StorageCells.addCellHandler(...)}。</p>
 */
public class InfinityFluxCellHandler implements ICellHandler
{
    public static final InfinityFluxCellHandler INSTANCE = new InfinityFluxCellHandler();

    @Override
    public boolean isCell(ItemStack is)
    {
        return is != null && !is.isEmpty() && is.getItem() instanceof InfinityFluxCellItem;
    }

    @Override
    public InfinityFluxCellInventory getCellInventory(ItemStack is, ISaveProvider container)
    {
        if (!isCell(is))
        {
            return null;
        }
        return new InfinityFluxCellInventory((InfinityFluxCellItem) is.getItem(), is, container);
    }
}
