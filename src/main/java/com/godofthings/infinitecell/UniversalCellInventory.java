package com.godofthings.infinitecell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

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

/**
 * 「万物元件」的 AE2 存储单元视图：对清单里的每一种资源都「无限存、无限取」。
 *
 * <p>与原来 4 个单资源元件的行为一致（不消耗耐久、不写 NBT），区别只是<b>一个元件管 5 种资源</b>，
 * 且容量口径从 {@code Integer.MAX_VALUE}（2.1G）提升到 {@link Long#MAX_VALUE}（9.2E）。</p>
 *
 * <p>写入时用 {@link KeyCounter#set} 而不是 {@code add}：{@code add} 在已有 MAX 之上再加会
 * 溢出成负数（AE2 会把负数当「没有」，物品从终端消失）。{@code set} 直接覆盖，天然不会累积溢出。</p>
 */
public class UniversalCellInventory implements StorageCell
{
    /** 闲置耗电（AE/t），与原来 4 个元件保持一致 */
    public static final double IDLE_DRAIN = 8.0D;

    private final ItemStack stack;
    private final List<AEKey> records;
    private final Set<AEKey> recordSet;

    public static final ICellHandler HANDLER = new Handler();

    public UniversalCellInventory(ItemStack stack)
    {
        if (!(stack.getItem() instanceof UniversalCellItem item))
        {
            throw new IllegalArgumentException("Cell isn't a universal cell!");
        }
        this.stack = stack;
        this.records = item.getRecords();
        this.recordSet = new HashSet<>(this.records);
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
        // 幻影存储：接受同类资源但不实际保存（与原来 4 个元件一致）
        return this.recordSet.contains(what) ? amount : 0;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        return this.recordSet.contains(what) ? amount : 0;
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
        long capacity = UniversalCellItem.getCapacity();
        for (AEKey key : this.records)
        {
            long existing = out.get(key);
            // 已经饱和（或历史遗留负数）就不动；否则直接钉到 9.2E
            if (existing < capacity)
            {
                out.set(key, capacity);
            }
        }
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source)
    {
        return this.recordSet.contains(what);
    }

    /** 本元件供应的资源清单（供 JEI / 调试使用） */
    public List<AEKey> getRecords()
    {
        return new ArrayList<>(this.records);
    }

    private static class Handler implements ICellHandler
    {
        @Override
        public boolean isCell(ItemStack is)
        {
            return is != null && is.getItem() instanceof UniversalCellItem;
        }

        @Override
        public @Nullable StorageCell getCellInventory(ItemStack is, @Nullable ISaveProvider host)
        {
            return isCell(is) ? new UniversalCellInventory(is) : null;
        }
    }
}
