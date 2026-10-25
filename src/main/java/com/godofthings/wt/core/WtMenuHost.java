package com.godofthings.wt.core;

import java.util.function.BiConsumer;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.MEStorage;
import appeng.api.storage.SupplierStorage;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.items.contents.StackDependentSupplier;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.SupplierInternalInventory;

/**
 * 无线终端菜单宿主基类（照抄 ae2wtlib 的 {@code WTMenuHost}，[本地改动] 泛型参数换成
 * {@link WtTerminalItem}；量子桥相关逻辑保留结构但本模组未移植量子桥卡，恒不生效）。
 */
public abstract class WtMenuHost extends WirelessTerminalMenuHost<WtTerminalItem>
        implements ISegmentedInventory
{
    private final SupplierInternalInventory<InternalInventory> singularityInventory;
    private final SupplierInternalInventory<InternalInventory> viewCellInventory;
    private final MEStorage storage;
    private ILinkStatus linkStatus = ILinkStatus.ofDisconnected();
    private final ILinkStatus quantumStatus = ILinkStatus.ofDisconnected();
    private final String closeHotkey;

    public WtMenuHost(WtTerminalItem item, Player player, ItemMenuHostLocator locator,
            BiConsumer<Player, ISubMenu> returnToMainMenu)
    {
        super(item, player, locator, returnToMainMenu);
        // [本地改动] v7.15.6：照抄 AE2 原生 WirelessTerminalMenuHost 的 storage 链路——直接
        // getLinkedGrid(stack)（绑定目标就是无线访问点，本身已含范围判定）。上一版抄 wtlib
        // 走 updateConnectedAccessPoint() + getActionableNode()（取最近访问点），有两个致命差异：
        // ① 菜单构造瞬间 updateConnectedAccessPoint 还没跑过，getActionableNode 恒 null →
        //    求值结果是 NullInventory；② StackDependentSupplier 按「栈实例引用」缓存，
        //    无耗能终端组件变化不复制品栈，实例不变 → 永不重估 → 终端永久空白。
        // AE2 原生 getLinkedGrid 不依赖宿主字段时序，且每次 get 都会重估，两条全解。
        this.storage = new SupplierStorage(this::getStorageFromStack);
        viewCellInventory = new SupplierInternalInventory<>(
                new StackDependentSupplier<>(this::getItemStack,
                        stack -> createInv(player, stack, WtComponents.VIEW_CELL_INVENTORY, 5)));
        singularityInventory = new SupplierInternalInventory<>(
                new StackDependentSupplier<>(this::getItemStack, stack -> createSingularityInv(player, stack)));

        // [本地改动] 上游在这里按当前终端改 closeHotkey；本模组沿用 AE2 默认。
        closeHotkey = super.getCloseHotkey();
    }

    private MEStorage getStorageFromStack()
    {
        // [本地改动] v7.15.6：照抄 AE2 原生 getStorageFromStack——直接按绑定目标取网络
        // （getLinkedGrid 本身已按绑定访问点解析，不依赖宿主访问点缓存的时序）。
        var stack = getItemStack();
        IGrid targetGrid = getItem().getLinkedGrid(stack, getPlayer().level(), null);
        if (targetGrid != null)
        {
            return targetGrid.getStorageService().getInventory();
        }
        return appeng.me.storage.NullInventory.of();
    }

    public String getCloseHotkey()
    {
        return closeHotkey;
    }

    @Override
    public MEStorage getInventory()
    {
        return this.storage;
    }

    @Override
    public IGridNode getActionableNode()
    {
        // [本地改动] 本模组未移植量子桥卡，quantumStatus 恒断开，直接走 AE2 原生节点。
        return super.getActionableNode();
    }

    @Override
    public void updateLinkStatus()
    {
        super.updateLinkStatus();
        linkStatus = super.getLinkStatus();
    }

    @Override
    public ILinkStatus getLinkStatus()
    {
        return linkStatus;
    }

    @Override
    public void updateConnectedAccessPoint()
    {
        super.updateConnectedAccessPoint();
    }

    // [本地改动] 上游的 findQuantumBridge / getQEFrequency / isQuantumLinked / recharge 均
    // 服务量子桥卡（未移植），整段省略；getPowerDrainPerTick 走 AE2 原生闲置耗电，
    // 再由 WtPoweredItem.extractAEPower 的无耗能实现兜底。

    @Override
    public boolean consumeIdlePower(Actionable action)
    {
        // [本地改动] 无耗能：菜单打开期间不消耗任何能量。
        return true;
    }

    public InternalInventory getViewCellStorage()
    {
        return viewCellInventory;
    }

    @Override
    public InternalInventory getSubInventory(net.minecraft.resources.ResourceLocation id)
    {
        if (id.equals(INV_SINGULARITY))
        {
            return singularityInventory;
        }
        return null;
    }

    public static final net.minecraft.resources.ResourceLocation INV_SINGULARITY =
            WtComponents.godofthingsId("singularity");

    protected static InternalInventory createInv(Player player, ItemStack stack,
            DataComponentType<ItemContainerContents> componentType, int size)
    {
        var craftingGrid = new AppEngInternalInventory(new InternalInventoryHost()
        {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv)
            {
                stack.set(componentType, inv.toItemContainerContents());
            }

            @Override
            public boolean isClientSide()
            {
                return player.level().isClientSide();
            }
        }, size);
        craftingGrid
                .fromItemContainerContents(stack.getOrDefault(componentType, ItemContainerContents.EMPTY));
        return craftingGrid;
    }

    private static InternalInventory createSingularityInv(Player player, ItemStack stack)
    {
        var inv = new AppEngInternalInventory(new InternalInventoryHost()
        {
            @Override
            public void saveChangedInventory(AppEngInternalInventory inv)
            {
                stack.set(WtComponents.SINGULARITY, inv.getStackInSlot(0));
            }

            @Override
            public boolean isClientSide()
            {
                return player.level().isClientSide();
            }
        }, 1, 1);
        inv.setItemDirect(0, stack.getOrDefault(WtComponents.SINGULARITY, ItemStack.EMPTY));
        return inv;
    }
}
