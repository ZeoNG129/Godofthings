package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.item.BlackBoxData;
import com.godofthings.item.GodBlackBoxItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之黑盒配置菜单：开关按钮 + 3×3 白名单过滤槽 + 玩家物品栏。
 * <p>
 * 便携菜单（无方块），过滤槽与开关状态均直接读写玩家物品栏里黑盒 ItemStack 的
 * CUSTOM_DATA；过滤槽变更在服务端实时写回黑盒，关闭界面时兜底再写一次。
 */
public class GodBlackBoxMenu extends AbstractContainerMenu
{
    private final Inventory playerInv;
    private final int boxSlot;

    /** 客户端由网络包同步的开关状态缓存（DataSlot 机制）。 */
    private int cachedEnabled = 0;
    /** 客户端由网络包同步的过滤模式缓存：0=白名单，1=黑名单。 */
    private int cachedMode = BlackBoxData.MODE_WHITELIST;

    private final ItemStackHandler filterHandler = new ItemStackHandler(BlackBoxData.FILTER_SLOTS)
    {
        // 白名单槽位兼作存储：堆叠上限无限（数量可突破 64/99，真实数量显示在槽位）
        @Override
        public int getSlotLimit(int slot)
        {
            return Integer.MAX_VALUE;
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack)
        {
            return Integer.MAX_VALUE;
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            super.onContentsChanged(slot);
            if (!playerInv.player.level().isClientSide)
            {
                writeFilterToBox();
            }
        }
    };

    private final DataSlot enabledSlot = new DataSlot()
    {
        @Override
        public int get()
        {
            if (playerInv.player.level().isClientSide)
            {
                return cachedEnabled;
            }
            return BlackBoxData.isEnabled(getBox()) ? 1 : 0;
        }

        @Override
        public void set(int value)
        {
            cachedEnabled = value;
        }
    };

    private final DataSlot modeSlot = new DataSlot()
    {
        @Override
        public int get()
        {
            if (playerInv.player.level().isClientSide)
            {
                return cachedMode;
            }
            return BlackBoxData.isWhitelistMode(getBox()) ? BlackBoxData.MODE_WHITELIST : BlackBoxData.MODE_BLACKLIST;
        }

        @Override
        public void set(int value)
        {
            cachedMode = value;
        }
    };

    public GodBlackBoxMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv, extraData.readVarInt());
    }

    public GodBlackBoxMenu(int containerId, Inventory playerInv, int boxSlot)
    {
        super(Godofthings.GOD_BLACK_BOX_MENU.get(), containerId);
        this.playerInv = playerInv;
        this.boxSlot = boxSlot;

        if (playerInv.player.level().isClientSide)
        {
            // 客户端：用本地黑盒的当前状态初始化缓存（后续由网络包同步收敛）
            this.cachedEnabled = BlackBoxData.isEnabled(getBox()) ? 1 : 0;
            this.cachedMode = BlackBoxData.isWhitelistMode(getBox()) ? BlackBoxData.MODE_WHITELIST : BlackBoxData.MODE_BLACKLIST;
        }
        else
        {
            // 服务端：从黑盒读白名单填充过滤槽
            List<ItemStack> filter = BlackBoxData.getFilter(getBox(), playerInv.player.level().registryAccess());
            for (int i = 0; i < BlackBoxData.FILTER_SLOTS; i++)
            {
                filterHandler.setStackInSlot(i, i < filter.size() ? filter.get(i) : ItemStack.EMPTY);
            }
        }

        // 白名单过滤槽 3×3（复用投掷器 dispenser 贴图的 3×3 槽位区）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 3; col++)
            {
                this.addSlot(new SlotItemHandler(filterHandler, row * 3 + col, 62 + col * 18, 17 + row * 18));
            }
        }

        // 玩家物品栏 3×9 + 快捷栏 1×9
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }

        this.addDataSlot(enabledSlot);
        this.addDataSlot(modeSlot);
    }

    public boolean isEnabled()
    {
        return enabledSlot.get() == 1;
    }

    public boolean isWhitelistMode()
    {
        return modeSlot.get() == BlackBoxData.MODE_WHITELIST;
    }

    /** 客户端点击开关后的乐观更新（立即反馈，服务端 broadcastChanges 随后收敛）。 */
    public void toggleEnabledLocal()
    {
        this.cachedEnabled = this.cachedEnabled == 1 ? 0 : 1;
    }

    /** 客户端点击模式按钮后的乐观更新。 */
    public void toggleModeLocal()
    {
        this.cachedMode = this.cachedMode == BlackBoxData.MODE_WHITELIST
                ? BlackBoxData.MODE_BLACKLIST
                : BlackBoxData.MODE_WHITELIST;
    }

    private ItemStack getBox()
    {
        if (boxSlot == -1)
        {
            ItemStack off = playerInv.offhand.get(0);
            if (off.getItem() instanceof GodBlackBoxItem)
            {
                return off;
            }
        }
        else if (boxSlot >= 0 && boxSlot < playerInv.items.size())
        {
            ItemStack s = playerInv.items.get(boxSlot);
            if (s.getItem() instanceof GodBlackBoxItem)
            {
                return s;
            }
        }
        // 兜底：黑盒被移动到其他槽，遍历找第一个
        for (ItemStack s : playerInv.items)
        {
            if (s.getItem() instanceof GodBlackBoxItem)
            {
                return s;
            }
        }
        for (ItemStack s : playerInv.offhand)
        {
            if (s.getItem() instanceof GodBlackBoxItem)
            {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private void writeFilterToBox()
    {
        ItemStack box = getBox();
        if (box.isEmpty())
        {
            return;
        }
        List<ItemStack> filter = new ArrayList<>();
        for (int i = 0; i < BlackBoxData.FILTER_SLOTS; i++)
        {
            ItemStack s = filterHandler.getStackInSlot(i);
            if (!s.isEmpty())
            {
                filter.add(s);
            }
        }
        BlackBoxData.setFilter(box, filter, playerInv.player.level().registryAccess());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (buttonId == 0 || buttonId == 1)
        {
            ItemStack box = getBox();
            if (!box.isEmpty() && !player.level().isClientSide)
            {
                if (buttonId == 0)
                {
                    BlackBoxData.setEnabled(box, !BlackBoxData.isEnabled(box));
                }
                else
                {
                    BlackBoxData.setMode(box, BlackBoxData.isWhitelistMode(box)
                            ? BlackBoxData.MODE_BLACKLIST
                            : BlackBoxData.MODE_WHITELIST);
                }
                this.broadcastChanges();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true; // 便携菜单，无方块
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < BlackBoxData.FILTER_SLOTS)
            {
                if (!this.moveItemStackTo(stack, BlackBoxData.FILTER_SLOTS, this.slots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.moveItemStackTo(stack, 0, BlackBoxData.FILTER_SLOTS, false))
            {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty())
            {
                slot.setByPlayer(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount())
            {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        if (!player.level().isClientSide)
        {
            writeFilterToBox();
        }
    }
}
