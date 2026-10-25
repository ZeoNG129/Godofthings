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
        this.storage = new SupplierStorage(new StackDependentSupplier<>(
                this::getItemStack, this::getStorageFromStack));
        viewCellInventory = new SupplierInternalInventory<>(
                new StackDependentSupplier<>(this::getItemStack,
                        stack -> createInv(player, stack, WtComponents.VIEW_CELL_INVENTORY, 5)));
        singularityInventory = new SupplierInternalInventory<>(
                new StackDependentSupplier<>(this::getItemStack, stack -> createSingularityInv(player, stack)));

        // [本地改动] 上游在这里按当前终端改 closeHotkey；本模组沿用 AE2 默认。
        closeHotkey = super.getCloseHotkey();
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

    private MEStorage getStorageFromStack(ItemStack stack)
    {
        updateConnectedAccessPoint();
        IGridNode node = getActionableNode();
        if (node == null)
        {
            return appeng.me.storage.NullInventory.of();
        }
        IGrid targetGrid = node.getGrid();
        if (targetGrid != null)
        {
            return targetGrid.getStorageService().getInventory();
        }
        return appeng.me.storage.NullInventory.of();
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
