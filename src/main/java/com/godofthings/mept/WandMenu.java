package com.godofthings.mept;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import com.godofthings.mept.network.UpdateWandSlotPayload;
import net.minecraft.core.RegistryAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 放置工具配置菜单（照抄上游 WandMenu）。
 */
public class WandMenu extends AbstractContainerMenu
{
    public static final int SLOTS_PER_PAGE = 9;
    public static final int TOTAL_SLOTS = 18;
    public static final int MAX_PAGES = TOTAL_SLOTS / SLOTS_PER_PAGE;

    private final ItemStackHandler handler;
    private final List<GhostSlot> ghostSlots = new ArrayList<>();
    private final Map<Integer, String> fluidMap = new HashMap<>();
    private final ItemStack wandStack;
    private int currentPage = 0;

    // 网络包构造（客户端）
    public WandMenu(int id, Inventory playerInventory, FriendlyByteBuf buf)
    {
        this(id, playerInventory, playerInventory.player.getMainHandItem(),
                createHandlerFromBuf(buf, playerInventory.player.level().registryAccess()));
    }

    // 物品栈构造（服务端）
    public WandMenu(int id, Inventory playerInventory, ItemStack wandStack)
    {
        this(id, playerInventory, wandStack,
                createHandlerFromStack(wandStack, playerInventory.player.level().registryAccess()));
        CompoundTag cfg = wandStack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        if (cfg != null && cfg.contains("fluids"))
        {
            var ftag = cfg.getCompound("fluids");
            for (String key : ftag.getAllKeys())
            {
                try
                {
                    int idx = Integer.parseInt(key);
                    this.fluidMap.put(idx, ftag.getString(key));
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }
    }

    private WandMenu(int id, Inventory playerInventory, ItemStack wandStack, ItemStackHandler handler)
    {
        super(MeptMenus.WAND_MENU.get(), id);
        // 绑定到真正被编辑的工具，而不是菜单开着时恰好换到主手的东西
        this.wandStack = wandStack;

        if (handler == null)
        {
            this.handler = new ItemStackHandler(TOTAL_SLOTS);
        }
        else if (handler.getSlots() < TOTAL_SLOTS)
        {
            ItemStackHandler newHandler = new ItemStackHandler(TOTAL_SLOTS);
            for (int i = 0; i < handler.getSlots(); i++)
            {
                newHandler.setStackInSlot(i, handler.getStackInSlot(i));
            }
            this.handler = newHandler;
        }
        else
        {
            this.handler = handler;
        }

        // 3x3 幽灵槽（62,19 起，18px 一格）
        int startX = 62;
        int startY = 19;
        for (int i = 0; i < SLOTS_PER_PAGE; i++)
        {
            int row = i / 3;
            int col = i % 3;
            int x = startX + col * 18;
            int y = startY + row * 18;
            int visualIndex = i;
            GhostSlot s = new GhostSlot(visualIndex, x, y, (stack) ->
            {
                int actualIndex = getActualSlotIndex(visualIndex);
                ItemStack stackToSet = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
                this.handler.setStackInSlot(actualIndex, stackToSet);
                if (playerInventory.player.level().isClientSide)
                {
                    PacketDistributor.sendToServer(new UpdateWandSlotPayload(actualIndex, stackToSet));
                }
            });
            // 挂显示供应器，让原版槽位渲染展示正确物品（JEI/REI 覆盖层 Z 序才正确）
            s.setDisplayStackSupplier(() -> getItemAtVisualSlot(visualIndex));
            this.addSlot(s);
            this.ghostSlots.add(s);
        }

        // 玩家背包
        int playerInvX = 8;
        int playerInvY = 84;
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                int x = playerInvX + col * 18;
                int y = playerInvY + row * 18;
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x, y));
            }
        }
        int hotbarY = 142;
        for (int hb = 0; hb < 9; hb++)
        {
            int x = playerInvX + hb * 18;
            this.addSlot(new Slot(playerInventory, hb, x, hotbarY));
        }
    }

    private static ItemStackHandler createHandlerFromBuf(FriendlyByteBuf buf, RegistryAccess registryAccess)
    {
        CompoundTag cfg = buf.readNbt();
        return createHandlerFromTag(cfg, registryAccess);
    }

    private static ItemStackHandler createHandlerFromStack(ItemStack stack, RegistryAccess registryAccess)
    {
        CompoundTag cfg = stack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        return createHandlerFromTag(cfg, registryAccess);
    }

    private static ItemStackHandler createHandlerFromTag(CompoundTag cfg, RegistryAccess registryAccess)
    {
        ItemStackHandler h = new ItemStackHandler(TOTAL_SLOTS);
        if (cfg != null)
        {
            CompoundTag itemsTag = cfg.contains("items") ? cfg.getCompound("items") : cfg;
            if (itemsTag.contains("Items"))
            {
                h.deserializeNBT(registryAccess, itemsTag);
            }
        }
        return h;
    }

    public ItemStackHandler getHandler()
    {
        return this.handler;
    }

    public List<GhostSlot> getGhostSlots()
    {
        return this.ghostSlots;
    }

    public int getCurrentPage()
    {
        return this.currentPage;
    }

    public void setCurrentPage(int page)
    {
        if (page >= 0 && page < MAX_PAGES)
        {
            this.currentPage = page;
        }
    }

    public int getActualSlotIndex(int visualIndex)
    {
        return currentPage * SLOTS_PER_PAGE + visualIndex;
    }

    public ItemStack getItemAtVisualSlot(int visualIndex)
    {
        int actualIndex = getActualSlotIndex(visualIndex);
        if (actualIndex >= 0 && actualIndex < handler.getSlots())
        {
            return handler.getStackInSlot(actualIndex);
        }
        return ItemStack.EMPTY;
    }

    public void setItemAtVisualSlot(int visualIndex, ItemStack stack)
    {
        int actualIndex = getActualSlotIndex(visualIndex);
        if (actualIndex >= 0 && actualIndex < handler.getSlots())
        {
            handler.setStackInSlot(actualIndex, stack);
        }
    }

    public void setFluidSlot(int index, String fluidId)
    {
        if (fluidId == null)
        {
            this.fluidMap.remove(index);
        }
        else
        {
            this.fluidMap.put(index, fluidId);
        }
    }

    public String getFluidSlot(int index)
    {
        return this.fluidMap.get(index);
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player)
    {
        if (slotId >= 0 && slotId < this.slots.size())
        {
            Slot slot = this.slots.get(slotId);
            // 锁住被编辑的工具：菜单开着时不许拿走/移动它
            if (!(slot instanceof GhostSlot) && slot.getItem() == this.wandStack)
            {
                return;
            }
            if (slot instanceof GhostSlot ghostSlot)
            {
                int visualIndex = ghostSlot.getVisualIndex();
                int actualIndex = getActualSlotIndex(visualIndex);
                ItemStack carried = this.getCarried();

                if (!carried.isEmpty())
                {
                    ItemStack copy = carried.copyWithCount(1);
                    this.handler.setStackInSlot(actualIndex, copy);
                    if (player.level().isClientSide)
                    {
                        PacketDistributor.sendToServer(new UpdateWandSlotPayload(actualIndex, copy));
                    }
                }
                else
                {
                    this.handler.setStackInSlot(actualIndex, ItemStack.EMPTY);
                    if (player.level().isClientSide)
                    {
                        PacketDistributor.sendToServer(new UpdateWandSlotPayload(actualIndex, ItemStack.EMPTY));
                    }
                }
                return;
            }
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);

        CompoundTag combined = new CompoundTag();
        combined.put("items", this.handler.serializeNBT(player.level().registryAccess()));

        CompoundTag ftag = new CompoundTag();
        for (var e : this.fluidMap.entrySet())
        {
            ftag.putString(Integer.toString(e.getKey()), e.getValue());
        }
        combined.put("fluids", ftag);

        // 保留 SelectedSlot / PlacementCount / DirectionMode
        CompoundTag existing = wandStack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
        if (existing != null)
        {
            if (existing.contains("SelectedSlot"))
            {
                combined.putInt("SelectedSlot", existing.getInt("SelectedSlot"));
            }
            if (existing.contains("PlacementCount"))
            {
                combined.putInt("PlacementCount", existing.getInt("PlacementCount"));
            }
            if (existing.contains("DirectionMode"))
            {
                combined.putInt("DirectionMode", existing.getInt("DirectionMode"));
            }
        }

        if (!player.level().isClientSide)
        {
            // 写回本菜单打开时的那件工具——绝不写到当前主手（快捷栏可能已切换）
            if (isSupported(wandStack))
            {
                wandStack.set(MeptDataComponents.PLACEMENT_CONFIG.get(), combined);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }

    public void handleUpdateSlot(Player player, int slotIndex, ItemStack stack)
    {
        if (slotIndex >= 0 && slotIndex < handler.getSlots())
        {
            handler.setStackInSlot(slotIndex, stack);
            saveToItemStack(player);
        }
    }

    private void saveToItemStack(Player player)
    {
        if (isSupported(wandStack))
        {
            CompoundTag combined = new CompoundTag();
            combined.put("items", this.handler.serializeNBT(player.level().registryAccess()));

            CompoundTag existing = wandStack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
            if (existing != null)
            {
                if (existing.contains("fluids"))
                {
                    combined.put("fluids", existing.getCompound("fluids"));
                }
                if (existing.contains("SelectedSlot"))
                {
                    combined.putInt("SelectedSlot", existing.getInt("SelectedSlot"));
                }
                if (existing.contains("PlacementCount"))
                {
                    combined.putInt("PlacementCount", existing.getInt("PlacementCount"));
                }
                if (existing.contains("DirectionMode"))
                {
                    combined.putInt("DirectionMode", existing.getInt("DirectionMode"));
                }
            }

            wandStack.set(MeptDataComponents.PLACEMENT_CONFIG.get(), combined);
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        // 按物品类型匹配而非栈引用（引用会被数据组件同步换掉导致菜单意外关闭）
        return isSupported(player.getMainHandItem()) || isSupported(player.getOffhandItem());
    }

    private static boolean isSupported(ItemStack stack)
    {
        return !stack.isEmpty()
                && (stack.getItem() instanceof ItemMEPlacementTool
                        || stack.getItem() instanceof ItemMultiblockPlacementTool);
    }
}
