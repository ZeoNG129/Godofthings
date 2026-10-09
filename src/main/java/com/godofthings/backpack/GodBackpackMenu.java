package com.godofthings.backpack;

import com.godofthings.Godofthings;
import com.godofthings.network.GodBackpackSyncPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * 神之背包菜单：背包 9 列 × 6 行可见窗口（54 格）+ 玩家物品栏 36 格。
 *
 * <p><b>槽位顺序（界面依赖）</b>：菜单槽位 0..53 = 背包可见窗口（第 row 行第 col 列 =
 * 菜单槽位 {@code row*9+col}），54..89 = 玩家物品栏 3 行，90..98 = 快捷栏。
 * 界面用 {@code slots.get(i)}（i = 0..53）当网格格画影子 / 角标，
 * 用 {@link #visibleSlotToStorageIndex(int)} 换算到真实背包槽位。</p>
 *
 * <p><b>滚动</b>：120 格 = 14 行，可见 6 行 → {@link #MAX_SCROLL} = 8。
 * 可见窗口由内部 {@code VisibleWindow} 这个 54 格视图容器映射到「背包里 scroll 之后那一段」，
 * 滚动一变，同一批 Slot 指向的内容就换了；服务端随后 {@code broadcastChanges()} 把新内容推给客户端。
 * 最后一行的第 4..9 格（下标 120..125 那一带）没有对应槽位，映射返回 -1、内容恒为空。</p>
 *
 * <p><b>数据同步（选了「自定义同步包」这条路线，不用 DataSlot）</b>：
 * 滚动、排序方式、三个开关、搜索词、记忆格 / 忽略整理格全在
 * {@link GodBackpackSettings} 里，服务端改完就用 {@link GodBackpackSyncPayload} 推一份全量给客户端，
 * 客户端菜单把它存成副本（{@link #settings()} 在客户端返回这份副本）。
 * 槽位内容本身走原版容器同步（{@code broadcastChanges} / {@code ClientboundContainerSetSlotPacket}）。</p>
 *
 * <p><b>搜索过滤</b>：{@link #matchesSearch(ItemStack)} 只做判定，槽位数据保持真实
 * （界面自己决定怎么渲染不匹配的格子）；服务端在 {@link #quickMoveStack} 与两个转移按钮里跳过不匹配的物品。</p>
 */
public class GodBackpackMenu extends AbstractContainerMenu
{
    public static final int COLUMNS = 9;
    public static final int VISIBLE_ROWS = 6;
    /** 可见格数：菜单槽位 0..53 就是背包的可见窗口 */
    public static final int VISIBLE_SLOTS = COLUMNS * VISIBLE_ROWS;
    /** 最大滚动行数：120 格 = 14 行，可见 6 行 → 最多滚 8 行 */
    public static final int MAX_SCROLL = (GodBackpackItem.SIZE + COLUMNS - 1) / COLUMNS - VISIBLE_ROWS;

    public static final int BTN_SORT = 0;
    public static final int BTN_SORT_BY = 1;
    public static final int BTN_TO_BACKPACK = 2;
    public static final int BTN_TO_INVENTORY = 3;
    public static final int BTN_TOGGLE_IGNORE_DURABILITY = 4;
    public static final int BTN_TOGGLE_IGNORE_NBT = 5;
    public static final int BTN_TOGGLE_KEEP_SEARCH = 6;

    // ---- 槽位坐标（与 textures/gui/god_backpack.png 以及客户端的 GodBackpackScreen 对齐）----
    private static final int GRID_X = 8;
    private static final int GRID_Y = 37;
    private static final int PLAYER_INV_Y = 159;
    private static final int HOTBAR_Y = 217;

    /** 「背包不在物品栏里」时的兜底分支（测试直接塞一个堆叠进来） */
    private static final int SLOT_DIRECT = -2;
    /** 背包装备在 Curios 背部槽（反射查，见 {@link CuriosCompat}；客户端同样能解析） */
    public static final int SLOT_CURIOS = -3;

    private final Inventory playerInventory;
    /** 背包在哪：0..35 主物品栏 / -1 副手 / -3 Curios 背部槽 / -2 直接用传入的堆叠 */
    private final int backpackSlot;
    /** 传入的背包堆叠（不在物品栏里时的兜底，例如测试） */
    private final ItemStack directStack;

    /** 120 格真实内容 */
    private final GodBackpackContainer backpackContainer;
    /** 54 格可见窗口（映射到 backpackContainer 里 scroll 之后那一段） */
    private final Container visibleContainer;

    /** 滚动行数：服务端权威，客户端由同步包更新 */
    private int scroll;
    /** 客户端：服务端同步过来的设置副本；服务端恒为 null（直接读物品组件） */
    private GodBackpackSettings syncedSettings;
    /** 开界面后第一次 broadcastChanges 时补发一次全量同步 */
    private boolean initialSyncPending = true;

    /** 界面 / 测试直接给堆叠：能在物品栏里找到就按槽位解析，找不到就用这个堆叠本身 */
    public GodBackpackMenu(int containerId, Inventory playerInventory, ItemStack backpack)
    {
        this(containerId, playerInventory, backpack, findBackpackSlot(playerInventory, backpack));
    }

    /** 网络工厂用：客户端按服务端给的手持槽位（-1 = 副手）在物品栏里找回背包 */
    public GodBackpackMenu(int containerId, Inventory playerInventory, int backpackSlot)
    {
        this(containerId, playerInventory, ItemStack.EMPTY, backpackSlot);
    }

    private GodBackpackMenu(int containerId, Inventory playerInventory, ItemStack directStack, int backpackSlot)
    {
        super(Godofthings.GOD_BACKPACK_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.backpackSlot = backpackSlot;
        this.directStack = directStack == null ? ItemStack.EMPTY : directStack;
        this.backpackContainer = new GodBackpackContainer(this::resolveBackpack);
        this.visibleContainer = new VisibleWindow();

        // 背包可见 6×9（菜单槽位 0..53，必须排在最前：客户端界面按下标当网格格用）
        for (int row = 0; row < VISIBLE_ROWS; row++)
        {
            for (int col = 0; col < COLUMNS; col++)
            {
                this.addSlot(new Slot(this.visibleContainer, row * COLUMNS + col,
                        GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        // 玩家物品栏 3×9 + 快捷栏 1×9（背包自身所在的那一格用 PlayerSlot 锁住，不能拿走）
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < COLUMNS; col++)
            {
                this.addSlot(new PlayerSlot(playerInventory, col + row * COLUMNS + 9,
                        GRID_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < COLUMNS; col++)
        {
            this.addSlot(new PlayerSlot(playerInventory, col, GRID_X + col * 18, HOTBAR_Y));
        }

        if (playerInventory.player.level().isClientSide)
        {
            // 客户端：先拿本地物品组件里的设置兜底，服务端随后用同步包覆盖
            this.syncedSettings = GodBackpackItem.settings(resolveBackpack());
        }
    }

    // ------------------------------------------------------------------ 背包自身槽位锁定

    /**
     * 玩家物品栏槽位：**正在打开的那个背包所在的格子锁定**。
     *
     * <p>背包内容存在物品堆叠的组件里，菜单的一切读写都以「这个堆叠还在原位」为前提。
     * 界面开着却把背包拖走 / 丢出去 / 用数字键换到别处，菜单就失去了数据源
     * （轻则界面空白，重则把内容写到一个已经不在手上的堆叠上）。所以那一格不许拿。</p>
     */
    private final class PlayerSlot extends Slot
    {
        PlayerSlot(Container container, int index, int x, int y)
        {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player)
        {
            return !isLockedBackpackSlot(this.getContainerSlot());
        }
    }

    /** 该玩家物品栏下标是不是「正被打开的背包自己」（只有 0..35 会命中；副手 / Curios / 直传不锁） */
    private boolean isLockedBackpackSlot(int inventoryIndex)
    {
        return this.backpackSlot >= 0 && this.backpackSlot <= 35 && this.backpackSlot == inventoryIndex;
    }

    /** 被锁定的那一格在本菜单里的槽位 id（不在本菜单里时返回 -1） */
    private int lockedMenuSlot()
    {
        if (this.backpackSlot >= 9 && this.backpackSlot <= 35)
        {
            return 54 + (this.backpackSlot - 9);   // 主物品栏 3×9 → 菜单槽位 54..80
        }
        if (this.backpackSlot >= 0 && this.backpackSlot <= 8)
        {
            return 81 + this.backpackSlot;         // 快捷栏 → 菜单槽位 81..89
        }
        return -1;
    }

    /**
     * 拦下会「搬走背包自己」的点击：数字键快捷交换（SWAP）与创造模式中键复制（CLONE）
     * 都不经过 {@link Slot#mayPickup}，必须在这里单独挡一道。
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player)
    {
        int locked = lockedMenuSlot();
        if (locked >= 0 && slotId == locked
                && (clickType == ClickType.SWAP || clickType == ClickType.CLONE))
        {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    // ------------------------------------------------------------------ 背包解析

    /** 在物品栏里找这个堆叠所在的槽位（主物品栏 0..35 / 副手 -1）；找不到返回 SLOT_DIRECT */
    private static int findBackpackSlot(Inventory inventory, ItemStack backpack)
    {
        if (backpack == null || backpack.isEmpty())
        {
            return SLOT_DIRECT;
        }
        for (int i = 0; i < inventory.items.size(); i++)
        {
            if (inventory.items.get(i) == backpack)
            {
                return i;
            }
        }
        for (int i = 0; i < inventory.items.size(); i++)
        {
            if (ItemStack.matches(inventory.items.get(i), backpack))
            {
                return i;
            }
        }
        if (inventory.offhand.get(0) == backpack)
        {
            return GodBackpackItem.OFFHAND_SLOT;
        }
        return SLOT_DIRECT;
    }

    /**
     * 取当前的背包堆叠：先按开界面时记下的槽位找，找不到再退而求其次（背包被挪到别的格子），
     * 最后才用构造时直接传进来的堆叠（不在物品栏里的情况，例如测试）。
     */
    public ItemStack backpackStack()
    {
        return resolveBackpack();
    }

    private ItemStack resolveBackpack()
    {
        if (backpackSlot == GodBackpackItem.OFFHAND_SLOT)
        {
            ItemStack offhand = playerInventory.offhand.get(0);
            if (offhand.getItem() instanceof GodBackpackItem)
            {
                return offhand;
            }
        }
        else if (backpackSlot == SLOT_CURIOS)
        {
            // Curios 背部槽：没装 Curios / 反射失败都返回空，继续往下兜底找物品栏
            // （客户端同样装了 Curios，所以这一条在两侧都能解析；解析不到也不会崩）
            ItemStack curio = CuriosCompat.findBackpackInBackSlot(playerInventory.player);
            if (curio.getItem() instanceof GodBackpackItem)
            {
                return curio;
            }
        }
        else if (backpackSlot >= 0 && backpackSlot < playerInventory.items.size())
        {
            ItemStack stack = playerInventory.items.get(backpackSlot);
            if (stack.getItem() instanceof GodBackpackItem)
            {
                return stack;
            }
        }
        // 兜底 1：背包被挪到别的格子了 → 物品栏里第一个神之背包
        for (ItemStack stack : playerInventory.items)
        {
            if (stack.getItem() instanceof GodBackpackItem)
            {
                return stack;
            }
        }
        for (ItemStack stack : playerInventory.offhand)
        {
            if (stack.getItem() instanceof GodBackpackItem)
            {
                return stack;
            }
        }
        // 兜底 2：根本不在物品栏里
        return directStack;
    }

    // ------------------------------------------------------------------ 设置 / 同步

    /** 当前设置：服务端读物品组件；客户端读服务端同步过来的副本（没同步到之前用本地组件的） */
    public GodBackpackSettings settings()
    {
        if (this.syncedSettings != null)
        {
            return this.syncedSettings;
        }
        return GodBackpackItem.settings(resolveBackpack());
    }

    public String searchPhrase()
    {
        return settings().searchPhrase();
    }

    public int scroll()
    {
        return this.scroll;
    }

    public SortBy sortBy()
    {
        return settings().sortBy();
    }

    public boolean ignoreDurability()
    {
        return settings().ignoreDurability();
    }

    public boolean ignoreNbt()
    {
        return settings().ignoreNbt();
    }

    public boolean keepSearch()
    {
        return settings().keepSearch();
    }

    /** 客户端：应用服务端同步过来的设置与滚动（由 GodBackpackSyncPayload 调用） */
    public void applySync(GodBackpackSettings synced, int scroll)
    {
        this.syncedSettings = synced == null ? GodBackpackSettings.defaults() : synced;
        this.scroll = clampScroll(scroll);
    }

    /** 服务端：写入设置（物品组件）并推给客户端 */
    public void updateSettings(GodBackpackSettings newSettings)
    {
        if (newSettings == null)
        {
            return;
        }
        ItemStack backpack = resolveBackpack();
        if (backpack.isEmpty())
        {
            return;
        }
        GodBackpackItem.setSettings(backpack, newSettings);
        syncToClient();
    }

    /** 服务端 → 客户端：全量推一份设置 + 滚动（客户端拿它画记忆格影子 / 角标、按钮状态） */
    private void syncToClient()
    {
        if (playerInventory.player instanceof ServerPlayer serverPlayer)
        {
            PacketDistributor.sendToPlayer(serverPlayer,
                    new GodBackpackSyncPayload(this.containerId, settings(), this.scroll));
        }
    }

    @Override
    public void broadcastChanges()
    {
        // 服务端每 tick 都会调到这里：开界面后的第一次顺手补发一次全量同步
        // （客户端进来时只有本地物品组件里的旧设置，记忆格 / 开关要靠这次同步收敛）
        if (this.initialSyncPending && playerInventory.player instanceof ServerPlayer)
        {
            this.initialSyncPending = false;
            syncToClient();
        }
        super.broadcastChanges();
    }

    // ------------------------------------------------------------------ 可见窗口

    /** 第 visibleSlot 个可见格对应的背包槽位下标（scroll 之后）；-1 = 这个位置没有格子（最后一行只有 3 格） */
    public int visibleSlotToStorageIndex(int visibleSlot)
    {
        if (visibleSlot < 0 || visibleSlot >= VISIBLE_SLOTS)
        {
            return -1;
        }
        int storage = this.scroll * COLUMNS + visibleSlot;
        return storage < GodBackpackItem.SIZE ? storage : -1;
    }

    public static int clampScroll(int value)
    {
        return Math.max(0, Math.min(MAX_SCROLL, value));
    }

    /** 物品是否通过当前搜索词过滤（空搜索词 = 全通过；`@` 前缀按模组 id 匹配） */
    public boolean matchesSearch(ItemStack stack)
    {
        String phrase = searchPhrase();
        if (phrase == null || phrase.isBlank())
        {
            return true;
        }
        if (stack == null || stack.isEmpty())
        {
            return false;
        }
        String query = phrase.trim().toLowerCase(Locale.ROOT);
        if (query.startsWith("@"))
        {
            String mod = query.substring(1).trim();
            if (mod.isEmpty())
            {
                return true;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id.getNamespace().toLowerCase(Locale.ROOT).contains(mod);
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    /** 54 格可见窗口：把菜单槽位 0..53 映射到背包里 scroll 之后那一段 */
    private final class VisibleWindow implements Container
    {
        @Override
        public int getContainerSize()
        {
            return VISIBLE_SLOTS;
        }

        @Override
        public boolean isEmpty()
        {
            for (int i = 0; i < VISIBLE_SLOTS; i++)
            {
                if (!getItem(i).isEmpty())
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int visibleSlot)
        {
            int storage = visibleSlotToStorageIndex(visibleSlot);
            return storage < 0 ? ItemStack.EMPTY : backpackContainer.getItem(storage);
        }

        @Override
        public ItemStack removeItem(int visibleSlot, int amount)
        {
            int storage = visibleSlotToStorageIndex(visibleSlot);
            return storage < 0 ? ItemStack.EMPTY : backpackContainer.removeItem(storage, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int visibleSlot)
        {
            int storage = visibleSlotToStorageIndex(visibleSlot);
            return storage < 0 ? ItemStack.EMPTY : backpackContainer.removeItemNoUpdate(storage);
        }

        @Override
        public void setItem(int visibleSlot, ItemStack stack)
        {
            int storage = visibleSlotToStorageIndex(visibleSlot);
            if (storage >= 0)
            {
                backpackContainer.setItem(storage, stack);
            }
        }

        @Override
        public void setChanged()
        {
            backpackContainer.setChanged();
        }

        @Override
        public boolean stillValid(Player player)
        {
            return true;
        }

        @Override
        public void clearContent()
        {
            backpackContainer.clearContent();
        }

        @Override
        public int getMaxStackSize()
        {
            return backpackContainer.getMaxStackSize();
        }

        /** 可见格子沿用底层容器的放置校验（**不许把神之背包放进神之背包**） */
        @Override
        public boolean canPlaceItem(int visibleSlot, ItemStack stack)
        {
            int storage = visibleSlotToStorageIndex(visibleSlot);
            return storage >= 0 && backpackContainer.canPlaceItem(storage, stack);
        }
    }

    // ------------------------------------------------------------------ 交互

    @Override
    public boolean stillValid(Player player)
    {
        // 背包还在玩家身上（或被挪到别的格子）就还能用；被丢掉了就自动关界面
        return player == playerInventory.player && !resolveBackpack().isEmpty();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId)
    {
        if (player.level().isClientSide)
        {
            return false; // 客户端只发 ServerboundContainerButtonClickPacket，改动等服务端同步
        }
        switch (buttonId)
        {
            case BTN_SORT -> GodBackpackSorting.sort(backpackContainer, settings());
            case BTN_SORT_BY -> updateSettings(settings().withSortBy(settings().sortBy().next()));
            case BTN_TO_BACKPACK -> transferToBackpack(player);
            case BTN_TO_INVENTORY -> transferToInventory(player);
            case BTN_TOGGLE_IGNORE_DURABILITY -> updateSettings(settings().toggledIgnoreDurability());
            case BTN_TOGGLE_IGNORE_NBT -> updateSettings(settings().toggledIgnoreNbt());
            case BTN_TOGGLE_KEEP_SEARCH -> updateSettings(settings().toggledKeepSearch());
            default -> {
                return false;
            }
        }
        this.broadcastChanges();
        return true;
    }

    /** 服务端：切换某格的记忆（已经是记忆格 → 取消；否则有物品就记住它） */
    public void toggleMemory(int storageIndex)
    {
        if (storageIndex < 0 || storageIndex >= GodBackpackItem.SIZE)
        {
            return;
        }
        GodBackpackSettings current = settings();
        if (current.isMemory(storageIndex))
        {
            updateSettings(current.withMemory(storageIndex, ItemStack.EMPTY));
            return;
        }
        ItemStack stack = backpackContainer.getItem(storageIndex);
        if (stack.isEmpty())
        {
            return; // 空格不能记（界面在标记模式下也不会发这种请求）
        }
        updateSettings(current.withMemory(storageIndex, stack));
    }

    /** 服务端：切换某格是否「整理时原地不动」 */
    public void toggleNoSort(int storageIndex)
    {
        if (storageIndex < 0 || storageIndex >= GodBackpackItem.SIZE)
        {
            return;
        }
        updateSettings(settings().toggleNoSort(storageIndex));
    }

    /**
     * 服务端：**全选 / 取消全选「记忆」**（智能切换）。
     * <p>如果所有「有物品的格子」都已经是记忆格 → 全部取消；否则把所有有物品的格子设为记忆格。
     * 空格没有可记的物品，直接跳过（与单格标记的规则一致）。</p>
     */
    public void selectAllMemory()
    {
        GodBackpackSettings current = settings();
        boolean allMarked = true;
        for (int i = 0; i < GodBackpackItem.SIZE; i++)
        {
            if (!backpackContainer.getItem(i).isEmpty() && !current.isMemory(i))
            {
                allMarked = false;
                break;
            }
        }
        GodBackpackSettings next = current;
        for (int i = 0; i < GodBackpackItem.SIZE; i++)
        {
            ItemStack stack = backpackContainer.getItem(i);
            if (allMarked)
            {
                if (next.isMemory(i))
                {
                    next = next.withMemory(i, ItemStack.EMPTY);
                }
            }
            else if (!stack.isEmpty() && !next.isMemory(i))
            {
                next = next.withMemory(i, stack);
            }
        }
        updateSettings(next);
    }

    /** 服务端：**全选 / 取消全选「忽略整理」**（智能切换：全部已标记则全部取消，否则全部标记） */
    public void selectAllNoSort()
    {
        GodBackpackSettings current = settings();
        boolean allMarked = true;
        for (int i = 0; i < GodBackpackItem.SIZE; i++)
        {
            if (!current.isNoSort(i))
            {
                allMarked = false;
                break;
            }
        }
        GodBackpackSettings next = current;
        for (int i = 0; i < GodBackpackItem.SIZE; i++)
        {
            if (allMarked ? next.isNoSort(i) : !next.isNoSort(i))
            {
                next = next.toggleNoSort(i);
            }
        }
        updateSettings(next);
    }

    /** 服务端：设置搜索词（长度由 GodBackpackSettings 统一截断） */
    public void setSearchPhrase(String phrase)
    {
        GodBackpackSettings current = settings();
        String text = phrase == null ? "" : phrase;
        if (text.length() > GodBackpackSettings.MAX_SEARCH_LENGTH)
        {
            text = text.substring(0, GodBackpackSettings.MAX_SEARCH_LENGTH);
        }
        if (text.equals(current.searchPhrase()))
        {
            return;
        }
        updateSettings(current.withSearch(text));
    }

    /** 服务端：设置滚动行数（clamp 到 0..MAX_SCROLL，并立刻把新可见内容推给客户端） */
    public void setScroll(int newScroll)
    {
        this.scroll = clampScroll(newScroll);
        syncToClient();
        this.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot slot = index >= 0 && index < this.slots.size() ? this.slots.get(index) : null;
        if (slot == null || !slot.hasItem())
        {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        // 搜索词不匹配的物品不搬（界面把它们渲染成空，服务端也不能偷偷搬走）
        if (!matchesSearch(stack))
        {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.copy();

        if (index < VISIBLE_SLOTS)
        {
            // 背包 → 玩家物品栏
            if (!this.moveItemStackTo(stack, VISIBLE_SLOTS, this.slots.size(), true))
            {
                return ItemStack.EMPTY;
            }
        }
        else
        {
            // 玩家物品栏 → 背包（记忆格优先）
            if (isOpenBackpack(stack))
            {
                return ItemStack.EMPTY; // 别把正开着的背包塞进它自己
            }
            int before = stack.getCount();
            GodBackpackMemory.insert(backpackContainer, settings(), stack);
            if (stack.getCount() == before)
            {
                return ItemStack.EMPTY;
            }
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
        return result;
    }

    /** 这个堆叠是不是当前开着的那个背包（防止背包套自己） */
    private boolean isOpenBackpack(ItemStack stack)
    {
        ItemStack backpack = resolveBackpack();
        return !backpack.isEmpty() && stack == backpack;
    }

    /** 「存入背包」：把玩家物品栏（36 格，不含盔甲 / 副手）里匹配搜索词的物品尽量塞进背包，记忆格优先 */
    private void transferToBackpack(Player player)
    {
        GodBackpackSettings current = settings();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++)
        {
            ItemStack stack = inventory.items.get(slot);
            // ① 空的不搬；② 神之背包一律不搬（不许套娃 —— 这条路径走 GodBackpackMemory.insert，
            //    绕过了 Slot#mayPlace，必须单独拦）；③ 开了搜索时只搬匹配搜索词的
            if (stack.isEmpty() || stack.getItem() instanceof GodBackpackItem || !matchesSearch(stack))
            {
                continue;
            }
            GodBackpackMemory.insert(backpackContainer, current, stack);
            if (stack.isEmpty())
            {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        inventory.setChanged();
        backpackContainer.setChanged();
    }

    /** 「取到身上」：把背包里匹配搜索词的物品尽量塞回玩家物品栏（先并入同类堆叠，再找空格） */
    private void transferToInventory(Player player)
    {
        GodBackpackSettings current = settings();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < GodBackpackItem.SIZE; slot++)
        {
            ItemStack stack = backpackContainer.getItem(slot);
            if (stack.isEmpty() || !matchesSearch(stack))
            {
                continue;
            }
            ItemStack moving = stack.copy();
            // ① 并入玩家物品栏里已有的同类堆叠（ignoreDurability / ignoreNbt 打开时判定放宽）
            for (int i = 0; i < inventory.items.size() && !moving.isEmpty(); i++)
            {
                ItemStack existing = inventory.items.get(i);
                if (existing.isEmpty() || !GodBackpackMemory.matches(existing, moving,
                        current.ignoreDurability(), current.ignoreNbt()))
                {
                    continue;
                }
                int limit = Math.min(inventory.getMaxStackSize(), existing.getMaxStackSize());
                int space = limit - existing.getCount();
                if (space <= 0)
                {
                    continue;
                }
                int moved = Math.min(space, moving.getCount());
                existing.grow(moved);
                moving.shrink(moved);
            }
            // ② 剩下的找空格放
            for (int i = 0; i < inventory.items.size() && !moving.isEmpty(); i++)
            {
                if (!inventory.items.get(i).isEmpty())
                {
                    continue;
                }
                int moved = Math.min(inventory.getMaxStackSize(moving), moving.getCount());
                inventory.items.set(i, moving.copyWithCount(moved));
                moving.shrink(moved);
            }
            backpackContainer.setItem(slot, moving.isEmpty() ? ItemStack.EMPTY : moving);
        }
        inventory.setChanged();
        backpackContainer.setChanged();
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        if (player.level().isClientSide)
        {
            return;
        }
        backpackContainer.setChanged(); // 兜底：内容整体写回物品组件
        GodBackpackSettings current = settings();
        if (!current.keepSearch() && !current.searchPhrase().isEmpty())
        {
            // 没开「保留搜索词」→ 关界面就清空（开了就留在组件里，下次打开还是这个词）
            updateSettings(current.withSearch(""));
        }
    }
}
