package com.godofthings.fluxcell;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.ISaveProvider;
import appeng.api.storage.cells.StorageCell;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 无限 FE 存储元件（软依赖 AppFlux）。
 *
 * <h2>为什么必须走 AE2 的存储元件机制，而不是普通 FE 容器</h2>
 * NeoForge 的 {@code IEnergyStorage} 全部是 {@code int}（上限约 21 亿 FE），
 * 而本元件要的容量是 9.2E（{@link Long#MAX_VALUE}），int 装不下。
 * AppFlux 的 FE 元件同样走 AE2 的 {@link StorageCell}（用 long 记账），
 * 只在「物品 FE 能力」那一层做 int 转换，本元件照此设计。
 *
 * <h2>「无限」的语义</h2>
 * 与项目里其他无限元件一致：
 * <ul>
 *   <li>{@link #insert} 永远接受全部输入，不会因为“满了”而退回；</li>
 *   <li>{@link #extract} 永远满足请求（只要请求量 &gt; 0）；</li>
 *   <li>对外报告的总量固定在 9.2E 这一档。</li>
 * </ul>
 *
 * <p><b>软依赖</b>：本类不 import 任何 {@code com.glodblock.github.appflux} 类型。
 * FE 的 {@link AEKey} 由 {@link FluxBridge} 反射取得；AppFlux 未安装时
 * {@link FluxBridge#isAvailable()} 为 false，元件退化为「能放进去但取不出来」的普通格子，
 * 不会再崩游戏。</p>
 */
public class InfinityFluxCellInventory implements StorageCell
{
    /** 对外报告的「无限」容量：9.2E，与其它无限元件同档。 */
    public static final long INFINITE_AMOUNT = Long.MAX_VALUE;

    private final ItemStack stack;
    private final ISaveProvider container;
    private final InfinityFluxCellItem cellItem;

    public InfinityFluxCellInventory(InfinityFluxCellItem cellItem, ItemStack stack, ISaveProvider container)
    {
        this.cellItem = cellItem;
        this.stack = stack;
        this.container = container;
    }

    @Override
    public CellState getStatus()
    {
        return CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain()
    {
        return this.cellItem.getIdleDrain();
    }

    /** 无限元件：永远不“满”、也永远不“空”，因此任何 FE 都能存入，也能按需取出。 */
    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        if (amount <= 0)
        {
            return 0;
        }
        // 不是 FE 键（或 AppFlux 未装）→ 不收
        if (!FluxBridge.isFluxKey(what))
        {
            return 0;
        }
        return amount;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        if (amount <= 0 || !FluxBridge.isFluxKey(what))
        {
            return 0;
        }
        return amount;
    }

    @Override
    public void persist()
    {
        // 无限元件不保存数量：容量与存量都是恒定的。
    }

    /** 向网络报告“有无限 FE 可用”，这样合成/请求端能看到它。 */
    @Override
    public void getAvailableStacks(KeyCounter out)
    {
        AEKey key = FluxBridge.feKey();
        if (key != null)
        {
            out.add(key, INFINITE_AMOUNT);
        }
    }

    @Override
    public Component getDescription()
    {
        return this.stack.getHoverName();
    }
}
