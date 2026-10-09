package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.item.BlackBoxData;
import com.godofthings.item.GodBlackBoxItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
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

        /**
         * 不许把神之黑盒放进神之黑盒（套娃）。
         *
         * <p>槽位是 {@link SlotItemHandler}，它的 {@code mayPlace} 会问到这里，所以这一层能同时挡住
         * 手动拖放与 Shift 快捷移动。（注意：1.21 原版 {@code Slot#mayPlace} 直接 return true、
         * 不会去问 {@code Container#canPlaceItem}，所以光在容器/数据层写校验是拦不住的。）</p>
         */
        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return !(stack.getItem() instanceof GodBlackBoxItem);
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

        // 白名单过滤槽 5×3（前 3 列对齐 dispenser 贴图槽位区，向右扩 2 列）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 5; col++)
            {
                this.addSlot(new SlotItemHandler(filterHandler, row * 5 + col, 62 + col * 18, 17 + row * 18));
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
        // 记下的槽位已经不含黑盒了（被搬走 / 被替换）：**绝不去猜「物品栏里第一个黑盒」** ——
        // 玩家有多个黑盒时会静默读写到另一个黑盒上（背包那边踩过同样的坑，见 AGENTS.md 项目约定）。
        // 返回空，菜单随即判定失效并关闭。
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

    /**
     * 拦截界面里按 Q 丢出物品栏物品：直接按白名单/黑名单规则入盒或销毁，而非丢出到世界。
     * 界面场景丢出走 safeTake→player.drop→ItemTossEvent，但过滤槽 safeTake 会先触发
     * writeFilterToBox 改写 KEY_FILTER，导致后续 shouldKeep 时序误判，故在此直接处理。
     */
    @Override
    public void clicked(int slotId, int button, ClickType action, Player player)
    {
        // 数字键交换（SWAP）：过滤槽不许被换入神之黑盒。
        // 原版的 SWAP **不检查 Slot#mayPlace**，所以 isItemValid 那道拦不住它（背包那边实测过同样的漏洞）
        if (action == ClickType.SWAP && slotId >= 0 && slotId < BlackBoxData.FILTER_SLOTS
                && button >= 0 && button < 9
                && player.getInventory().getItem(button).getItem() instanceof GodBlackBoxItem)
        {
            return;
        }
        if (action == ClickType.THROW && this.getCarried().isEmpty()
                && slotId >= BlackBoxData.FILTER_SLOTS && slotId < this.slots.size())
        {
            ItemStack box = BlackBoxData.findEnabledBox(player);
            if (!box.isEmpty())
            {
                Slot slot = this.slots.get(slotId);
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty())
                {
                    boolean keep = BlackBoxData.shouldKeep(box, stack, player.level().registryAccess());
                    int count = button == 0 ? 1 : stack.getCount();
                    ItemStack taken = slot.safeTake(count, Integer.MAX_VALUE, player);
                    if (keep)
                    {
                        BlackBoxData.addToBox(box, taken, player.level().registryAccess());
                    }
                    this.broadcastChanges();
                    return;
                }
            }
        }
        super.clicked(slotId, button, action, player);
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
        // 便携菜单（无方块）：黑盒必须**还在玩家身上**（物品栏 / 副手）。
        // 扔到地上、放进箱子、被别的物品顶掉之后，界面必须自动关闭，不能留着一个能操作的空壳界面。
        // getBox() 只按开界面时记下的槽位解析、不做任何「找第一个黑盒」的兜底，
        // 所以这里返回 false 也就意味着「这个界面已经没有数据源了」。
        return !getBox().isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            // 神之黑盒一律不往过滤槽里搬（不许套娃）。index < FILTER_SLOTS 是「从过滤槽往外拿」，允许。
            if (index >= BlackBoxData.FILTER_SLOTS && stack.getItem() instanceof GodBlackBoxItem)
            {
                return ItemStack.EMPTY;
            }
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
