package com.godofthings.block.entity;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.godofthings.Godofthings;
import com.godofthings.ae2.AeGridNode;
import com.godofthings.menu.GodCraftMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 神之合成：自动合成工作台。
 * - 3×3 合成格 + 1 输出槽
 * - 每 tick 尝试合成（需先开启开关）
 * - 输出槽放不下则停止（不消耗原料）
 * - 锁定配方：锁定后只合成锁定配方，合成格只接受锁定模板物品
 * - 六个面可配置输入/输出
 */
public class GodCraftBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity
{
    public static final int INPUT_SLOTS = 9;
    public static final int TOTAL_SLOTS = 10; // 0-8 合成格, 9 输出
    /**
     * 每 tick 最多合成次数（原料不足或输出放不下时提前停止）。
     * 256 会让每次合成都触发 getRecipeFor 遍历全部合成配方，大量堆叠时严重掉 TPS；
     * 降到 8（160 次/秒，远超原版漏斗 2.5 次/秒）在性能与吞吐间取得平衡。
     */
    public static final int MAX_CRAFTS_PER_TICK = 8;

    private final ItemStackHandler inputSlots = new ItemStackHandler(INPUT_SLOTS)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        // 锁定配方后：合成格完全按锁定模板——非空槽位只能放对应物品，空槽位禁止放任何物品
        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            if (locked)
            {
                ItemStack tpl = lockedItems[slot];
                if (tpl.isEmpty())
                {
                    return false; // 锁定模板该槽为空 → 禁止放入任何物品
                }
                return ItemStack.isSameItem(tpl, stack); // 只允许放对应锁定物品
            }
            return true;
        }
    };
    private final ItemStackHandler outputSlot = new ItemStackHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    private final ItemStack[] lockedItems = new ItemStack[INPUT_SLOTS];
    private boolean locked = false;
    private boolean enabled = false;
    private ResourceLocation lockedRecipeId = null;

    /** 配方模板：8 个命名槽位，每个保存一份 9 格配方（仅记录物品类型，不存数量） */
    public static final int TEMPLATE_COUNT = 8;
    private final ItemStack[][] templates = new ItemStack[TEMPLATE_COUNT][INPUT_SLOTS];

    private final int[] faceModes = new int[6];
    private final IItemHandler[] sideHandlers = new IItemHandler[6];

    /** 是否接入 AE（并网后产物自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网）。 */
    private final AeGridNode aeNode = new AeGridNode(this);
    private int aeTick = 0;

    public GodCraftBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_CRAFT_BE.get(), pos, state);
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            lockedItems[i] = ItemStack.EMPTY;
        }
        for (int t = 0; t < TEMPLATE_COUNT; t++)
        {
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                templates[t][i] = ItemStack.EMPTY;
            }
        }
        for (int i = 0; i < 6; i++)
        {
            final int idx = i;
            sideHandlers[idx] = new SideHandler(idx);
        }
    }

    public ItemStackHandler getInputSlots() { return inputSlots; }
    public ItemStackHandler getOutputSlot() { return outputSlot; }

    // ---- AE 接入（网格节点：线缆直连并网，产物主动输出进 AE） ----

    public boolean isAeEnabled()
    {
        return aeEnabled;
    }

    public void toggleAeEnabled()
    {
        this.aeEnabled = !this.aeEnabled;
        setChanged();
    }

    @Override
    public IManagedGridNode getMainNode() { return aeNode.getMainNode(); }

    @Override
    public void saveChanges() { setChanged(); }

    /** 把输出槽产物推入 AE 网络（节流由 tick 控制）。 */
    private void pushOutputToAe()
    {
        if (!aeEnabled || !aeNode.isActive())
        {
            return;
        }
        IStorageService storage = aeNode.getStorage();
        if (storage == null)
        {
            return;
        }
        MEStorage inv = storage.getInventory();
        IActionSource source = aeNode.actionSource();
        ItemStack stack = outputSlot.getStackInSlot(0);
        if (stack.isEmpty())
        {
            return;
        }
        long inserted = inv.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
        if (inserted > 0)
        {
            outputSlot.extractItem(0, (int) inserted, false);
        }
    }

    /** 从 AE 网络拉取锁定模板原料，补齐合成格（开启 AE + 锁定模板时自动合成）。 */
    private void aeAutoCraft()
    {
        if (!aeEnabled || !aeNode.isActive() || !locked)
        {
            return;
        }
        IStorageService storage = aeNode.getStorage();
        if (storage == null)
        {
            return;
        }
        MEStorage inv = storage.getInventory();
        IActionSource source = aeNode.actionSource();
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            ItemStack tpl = lockedItems[i];
            if (tpl.isEmpty())
            {
                continue;
            }
            ItemStack cur = inputSlots.getStackInSlot(i);
            int have = (!cur.isEmpty() && ItemStack.isSameItem(cur, tpl)) ? cur.getCount() : 0;
            int need = tpl.getCount() - have;
            if (need <= 0)
            {
                continue;
            }
            AEItemKey key = AEItemKey.of(tpl);
            long extracted = inv.extract(key, need, Actionable.MODULATE, source);
            if (extracted > 0)
            {
                ItemStack got = tpl.copyWithCount((int) extracted);
                inputSlots.insertItem(i, got, false);
            }
        }
    }

    // ---- 开关 ----

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; setChanged(); }
    public void toggleEnabled() { setEnabled(!enabled); }

    // ---- 锁定 ----

    public boolean isLocked() { return locked; }
    public boolean isLocked(int slot) { return locked; }
    public ItemStack getLockedItem(int slot)
    {
        return slot >= 0 && slot < INPUT_SLOTS ? lockedItems[slot] : ItemStack.EMPTY;
    }

    public void setLocked(int slot, boolean lock)
    {
        if (lock)
        {
            // 记录当前 9 格配方作为锁定模板，并记录配方 ID
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                lockedItems[i] = inputSlots.getStackInSlot(i).copy();
            }
            lockedRecipeId = findCurrentRecipeId();
            locked = true;
        }
        else
        {
            locked = false;
            lockedRecipeId = null;
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                lockedItems[i] = ItemStack.EMPTY;
            }
        }
        setChanged();
    }

    private ResourceLocation findCurrentRecipeId()
    {
        if (level == null)
        {
            return null;
        }
        CraftingInput inv = makeCraftingInput();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, inv, level)
                .map(RecipeHolder::id).orElse(null);
    }

    private boolean matchesLockedTemplate()
    {
        if (!locked)
        {
            return true;
        }
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            ItemStack cur = inputSlots.getStackInSlot(i);
            ItemStack tpl = lockedItems[i];
            if (tpl.isEmpty())
            {
                if (!cur.isEmpty())
                {
                    return false;
                }
            }
            else if (cur.isEmpty() || !ItemStack.isSameItem(cur, tpl))
            {
                return false;
            }
        }
        return true;
    }

    /** 把当前 9 格合成格构造成 1.21.1 的 CraftingInput（供配方查询/合成用）。 */
    private CraftingInput makeCraftingInput()
    {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++)
        {
            items.set(i, inputSlots.getStackInSlot(i));
        }
        return CraftingInput.of(3, 3, items);
    }

    // ---- 配方模板（保存/加载 8 个命名配方，NBT 持久化） ----

    /** 把当前 9 格合成格保存为模板 slotIndex（0-7）。空模板返回 false。 */
    public boolean saveTemplate(int slotIndex)
    {
        if (slotIndex < 0 || slotIndex >= TEMPLATE_COUNT)
        {
            return false;
        }
        boolean empty = true;
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            templates[slotIndex][i] = inputSlots.getStackInSlot(i).copy();
            if (!templates[slotIndex][i].isEmpty())
            {
                empty = false;
            }
        }
        setChanged();
        syncToClient();
        return !empty;
    }

    /**
     * 从玩家背包取材料填充合成格（防刷物品）：
     * 1. 原子预检：背包中每种模板材料数量必须足够，否则返回 false 且不改动合成格；
     * 2. 合成格现有物品先退回玩家背包（放不下则掉落）；
     * 3. 按模板逐格从背包扣除材料并填入合成格。
     * 模板为空返回 false。
     */
    public boolean loadTemplateFromInventory(Player player, int slotIndex)
    {
        if (slotIndex < 0 || slotIndex >= TEMPLATE_COUNT || !hasTemplate(slotIndex))
        {
            return false;
        }
        Inventory inv = player.getInventory();
        // 原子预检：材料是否足够
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            ItemStack tpl = templates[slotIndex][i];
            if (!tpl.isEmpty() && countInInventory(inv, tpl) < tpl.getCount())
            {
                return false;
            }
        }
        // 合成格现有物品退回背包（放不下则掉落）
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            ItemStack cur = inputSlots.getStackInSlot(i);
            if (!cur.isEmpty())
            {
                inputSlots.setStackInSlot(i, ItemStack.EMPTY);
                if (!inv.add(cur) && level != null && !level.isClientSide)
                {
                    Containers.dropItemStack(level, getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5,
                            getBlockPos().getZ() + 0.5, cur);
                }
            }
        }
        // 从背包扣除材料并填充合成格
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            ItemStack tpl = templates[slotIndex][i];
            if (tpl.isEmpty())
            {
                continue;
            }
            int needed = tpl.getCount();
            takeFromInventory(inv, tpl, needed);
            ItemStack filled = tpl.copy();
            filled.setCount(needed);
            inputSlots.setStackInSlot(i, filled);
        }
        setChanged();
        syncToClient();
        return true;
    }

    /** 统计背包中与模板物品相同（含 NBT）的总数量。 */
    private static int countInInventory(Inventory inv, ItemStack tpl)
    {
        int count = 0;
        for (int s = 0; s < inv.getContainerSize(); s++)
        {
            ItemStack st = inv.getItem(s);
            if (ItemStack.isSameItemSameComponents(st, tpl))
            {
                count += st.getCount();
            }
        }
        return count;
    }

    /** 从背包逐格扣除指定数量（预检已保证足够）。 */
    private static void takeFromInventory(Inventory inv, ItemStack tpl, int amount)
    {
        for (int s = 0; s < inv.getContainerSize() && amount > 0; s++)
        {
            ItemStack st = inv.getItem(s);
            if (ItemStack.isSameItemSameComponents(st, tpl))
            {
                int take = Math.min(amount, st.getCount());
                st.shrink(take);
                amount -= take;
            }
        }
    }

    public boolean hasTemplate(int slotIndex)
    {
        if (slotIndex < 0 || slotIndex >= TEMPLATE_COUNT)
        {
            return false;
        }
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            if (!templates[slotIndex][i].isEmpty())
            {
                return true;
            }
        }
        return false;
    }

    /** 模板 slotIndex 的第 i 格物品（供客户端 GUI 配方展示）。 */
    public ItemStack getTemplateItem(int slotIndex, int i)
    {
        if (slotIndex < 0 || slotIndex >= TEMPLATE_COUNT || i < 0 || i >= INPUT_SLOTS)
        {
            return ItemStack.EMPTY;
        }
        return templates[slotIndex][i];
    }

    /** 模板槽位中第一个非空物品（供客户端 GUI 展示图标），无则返回空。 */
    public ItemStack getTemplatePreview(int slotIndex)
    {
        if (slotIndex < 0 || slotIndex >= TEMPLATE_COUNT)
        {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            if (!templates[slotIndex][i].isEmpty())
            {
                return templates[slotIndex][i];
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 模板 slotIndex 的合成结果预览（客户端/服务端皆可调用）：
     * 把模板 9 格物品放入临时合成容器，查询 CRAFTING 配方并 assemble。
     * 无有效配方或模板为空时返回空 ItemStack。
     */
    public ItemStack getTemplateResult(int slotIndex)
    {
        if (slotIndex < 0 || slotIndex >= TEMPLATE_COUNT || !hasTemplate(slotIndex) || level == null)
        {
            return ItemStack.EMPTY;
        }
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            ItemStack tpl = templates[slotIndex][i];
            if (!tpl.isEmpty())
            {
                items.set(i, tpl.copy());
            }
        }
        CraftingInput inv = CraftingInput.of(3, 3, items);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, inv, level)
                .map(r -> r.value().assemble(inv, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    // ---- 面模式 ----

    public int getFaceMode(Direction dir) { return faceModes[dir.get3DDataValue()]; }
    public void setFaceMode(Direction dir, int mode)
    {
        faceModes[dir.get3DDataValue()] = ((mode % 4) + 4) % 4;
        setChanged();
    }
    public void cycleFaceMode(Direction dir) { setFaceMode(dir, getFaceMode(dir) + 1); }

    @Override
    public void onLoad()
    {
        super.onLoad();
        aeNode.create(level, worldPosition);
    }

    @Override
    public void setRemoved()
    {
        aeNode.destroy();
        super.setRemoved();
    }

    // ---- tick ----

    public static void tick(Level level, BlockPos pos, BlockState state, GodCraftBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.autoTransfer();
        if (be.enabled)
        {
            be.tryCraft();
        }
        // AE 自动合成 + 产物输出节流：每 20 tick（1 秒）拉一次原料、推一次产物
        be.aeTick++;
        if (be.aeTick >= 20)
        {
            be.aeTick = 0;
            be.aeAutoCraft();
            be.pushOutputToAe();
        }
    }

    private void autoTransfer()
    {
        for (Direction dir : Direction.values())
        {
            int mode = getFaceMode(dir);
            if (mode == FaceMode.NONE.getId())
            {
                continue;
            }
            BlockPos np = worldPosition.relative(dir);
            if (!level.isLoaded(np)) continue;
            BlockEntity neighbor = level.getBlockEntity(np);
            if (neighbor == null) continue;
            IItemHandler cap = level.getCapability(Capabilities.ItemHandler.BLOCK, np, dir.getOpposite());
            if (cap == null) continue;
            if (mode == FaceMode.INPUT.getId() || mode == FaceMode.BOTH.getId())
            {
                pullInput(cap);
            }
            if (mode == FaceMode.OUTPUT.getId() || mode == FaceMode.BOTH.getId())
            {
                pushOutput(cap);
            }
        }
    }

    /** 自动输入：把邻居容器里的物品「均分到每一格」。
     *  <p>
     *  采用低水位优先分配：每次给当前数量最少的格子补 1 个，让所有格子数量始终均衡，
     *  避免供给慢时退化成「第一格先满」的顺序填充（顺序填充会导致合成格长期不完整、
     *  合成断续）。锁定模式只往锁定模板非空且匹配的槽位均分。 */
    private void pullInput(IItemHandler neighbor)
    {
        for (int s = 0; s < neighbor.getSlots(); s++)
        {
            ItemStack src = neighbor.getStackInSlot(s);
            if (src.isEmpty()) continue;

            // 1) 收集本物品可放入的槽位，并模拟探测每格还能收多少
            List<Integer> slots = new ArrayList<>();
            int[] room = new int[INPUT_SLOTS];
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                // 锁定后：只往锁定模板非空且匹配的槽位补充，空模板槽位不塞任何物品
                if (locked)
                {
                    ItemStack tpl = lockedItems[i];
                    if (tpl.isEmpty() || !ItemStack.isSameItem(tpl, src))
                    {
                        continue; // 模板该槽为空 / 不是锁定物品 → 不放入
                    }
                }
                ItemStack cur = inputSlots.getStackInSlot(i);
                if (cur.isEmpty() || ItemStack.isSameItem(cur, src))
                {
                    ItemStack leftover = inputSlots.insertItem(i, src.copy(), true);
                    int can = src.getCount() - leftover.getCount();
                    if (can > 0)
                    {
                        room[i] = can;
                        slots.add(i);
                    }
                }
            }
            if (slots.isEmpty()) continue;

            // 2) 一次性抽取所有可用槽位能吸收的总量
            int totalRoom = 0;
            for (int i : slots)
            {
                totalRoom += room[i];
            }
            ItemStack pulled = neighbor.extractItem(s, Math.min(src.getCount(), totalRoom), false);
            if (pulled.isEmpty()) continue;

            // 3) 均分到每一格：低水位优先——每次给当前数量最少的格子补 1 个，
            //    让所有格子数量始终均衡。旧算法按 总量/格数 一次摊派，在邻居
            //    供料慢（每 tick 只来 1 个）时会退化成「第一格先满」的顺序填充，
            //    导致合成格长期不完整、合成断续。
            int remaining = pulled.getCount();
            while (remaining > 0)
            {
                int minSlot = -1;
                int minCount = Integer.MAX_VALUE;
                for (int i : slots)
                {
                    if (room[i] <= 0) continue;
                    int count = inputSlots.getStackInSlot(i).getCount();
                    if (count < minCount)
                    {
                        minCount = count;
                        minSlot = i;
                    }
                }
                if (minSlot < 0) break; // 所有可用槽已满
                ItemStack piece = pulled.copy();
                piece.setCount(1);
                inputSlots.insertItem(minSlot, piece, false);
                room[minSlot]--;
                remaining--;
            }
        }
    }

    private void pushOutput(IItemHandler neighbor)
    {
        ItemStack out = outputSlot.getStackInSlot(0);
        if (out.isEmpty()) return;
        ItemStack leftover = neighbor.insertItem(0, out.copy(), false);
        int moved = out.getCount() - leftover.getCount();
        if (moved > 0)
        {
            outputSlot.extractItem(0, moved, false);
        }
    }

    /** 尝试合成：先确认输出能放下，能放才消耗原料（输出满了自动停） */
    private void tryCraft()
    {
        if (!(level instanceof ServerLevel))
        {
            return;
        }
        // 循环合成多次，提高吞吐（原料不足或输出放不下时提前停止）
        for (int n = 0; n < MAX_CRAFTS_PER_TICK; n++)
        {
            if (!craftOnce())
            {
                break;
            }
        }
    }

    /** 单次合成尝试；成功合成返回 true，否则 false。 */
    private boolean craftOnce()
    {
        // 锁定后：合成格必须与锁定模板一致
        if (!matchesLockedTemplate())
        {
            return false;
        }
        CraftingInput craftInv = makeCraftingInput();
        RecipeHolder<CraftingRecipe> holder = findRecipe(craftInv);
        if (holder == null)
        {
            return false;
        }
        ItemStack result = holder.value().assemble(craftInv, level.registryAccess());
        if (result.isEmpty())
        {
            return false;
        }
        // 先模拟：输出槽能放下多少，剩余部分必须能完整推给相邻容器，否则停止（不消耗原料），
        // 避免产物部分入槽后剩余被静默丢弃
        ItemStack afterOutput = outputSlot.insertItem(0, result, true);
        if (!afterOutput.isEmpty() && !canPushResultFully(afterOutput))
        {
            return false; // 输出槽 + 所有输出方向容器都放不下完整产物 → 停止，不消耗原料
        }
        // 消耗原料（按原始 9 格逐格扣除，与旧版逻辑一致）
        for (int i = 0; i < 9; i++)
        {
            ItemStack need = inputSlots.getStackInSlot(i);
            if (need.isEmpty()) continue;
            inputSlots.extractItem(i, 1, false);
        }
        // 优先放入输出槽，剩余部分推给相邻容器（canPushResultFully 已保证能完整放下）
        ItemStack remaining = outputSlot.insertItem(0, result, false);
        if (!remaining.isEmpty())
        {
            pushResultToNeighbors(remaining);
        }
        setChanged();
        return true;
    }

    /**
     * 查找当前合成格的配方。
     * <p>锁定配方时直接按 {@link #lockedRecipeId} 用 byKey 取配方，避免每 tick 都
     * getRecipeFor 遍历全部合成配方做矩阵匹配（这是神之合成掉 TPS 的主要热点之一）；
     * 非锁定模式才用 getRecipeFor 匹配当前合成格。
     */
    @Nullable
    private RecipeHolder<CraftingRecipe> findRecipe(CraftingInput craftInv)
    {
        if (locked && lockedRecipeId != null)
        {
            var byKey = level.getRecipeManager().byKey(lockedRecipeId);
            if (byKey.isPresent() && byKey.get().value() instanceof CraftingRecipe cr)
            {
                return new RecipeHolder<>(lockedRecipeId, cr);
            }
            return null;
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftInv, level).orElse(null);
    }

    /** 检查产物能否被面配置为输出方向的相邻容器完全接收 */
    private boolean canPushResultFully(ItemStack result)
    {
        for (Direction dir : Direction.values())
        {
            int mode = getFaceMode(dir);
            if (mode != FaceMode.OUTPUT.getId() && mode != FaceMode.BOTH.getId())
            {
                continue;
            }
            BlockPos np = worldPosition.relative(dir);
            if (!level.isLoaded(np)) continue;
            BlockEntity neighbor = level.getBlockEntity(np);
            if (neighbor == null) continue;
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, np, dir.getOpposite());
            if (handler == null) continue;
            ItemStack test = result.copy();
            for (int slot = 0; slot < handler.getSlots(); slot++)
            {
                test = handler.insertItem(slot, test, true);
                if (test.isEmpty())
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** 把产物推给面配置为输出方向的相邻容器 */
    private void pushResultToNeighbors(ItemStack result)
    {
        ItemStack toPush = result.copy();
        for (Direction dir : Direction.values())
        {
            int mode = getFaceMode(dir);
            if (mode != FaceMode.OUTPUT.getId() && mode != FaceMode.BOTH.getId())
            {
                continue;
            }
            BlockPos np = worldPosition.relative(dir);
            if (!level.isLoaded(np)) continue;
            BlockEntity neighbor = level.getBlockEntity(np);
            if (neighbor == null) continue;
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, np, dir.getOpposite());
            if (handler == null) continue;
            for (int slot = 0; slot < handler.getSlots(); slot++)
            {
                ItemStack leftover = handler.insertItem(slot, toPush, false);
                int moved = toPush.getCount() - leftover.getCount();
                toPush = leftover;
                if (toPush.isEmpty())
                {
                    return;
                }
            }
        }
    }

    // ---- capability ----

    /** 面能力：输入面只插合成格，输出面只取输出槽；faceMode 为 NONE 的面不暴露能力。 */
    public IItemHandler getSideCapability(@Nullable Direction side)
    {
        if (side == null)
        {
            return null;
        }
        int idx = side.get3DDataValue();
        if (faceModes[idx] != FaceMode.NONE.getId())
        {
            return sideHandlers[idx];
        }
        return null;
    }

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_CRAFT_BE.get(),
                    (be, side) -> be.getSideCapability(side));
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_CRAFT_BE.get(),
                    (be, side) -> be);
        }
    }

    /** 面包装 handler：输入面只插合成格，输出面只取输出槽 */
    private class SideHandler implements IItemHandler
    {
        private final int dirIndex;
        SideHandler(int dirIndex) { this.dirIndex = dirIndex; }
        private FaceMode mode() { return FaceMode.fromId(faceModes[dirIndex]); }

        @Override public int getSlots() { return TOTAL_SLOTS; }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            if (slot >= 0 && slot < INPUT_SLOTS) return inputSlots.getStackInSlot(slot);
            if (slot == 9) return outputSlot.getStackInSlot(0);
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (slot >= 0 && slot < INPUT_SLOTS
                    && (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH))
            {
                return inputSlots.insertItem(slot, stack, simulate);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (slot == 9 && (mode() == FaceMode.OUTPUT || mode() == FaceMode.BOTH))
            {
                return outputSlot.extractItem(0, amount, simulate);
            }
            return ItemStack.EMPTY;
        }

        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack)
        {
            return slot >= 0 && slot < INPUT_SLOTS;
        }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        tag.put("Inputs", inputSlots.serializeNBT(provider));
        tag.put("Output", outputSlot.serializeNBT(provider));
        tag.putIntArray("FaceModes", faceModes);
        tag.putBoolean("Locked", locked);
        tag.putBoolean("Enabled", enabled);
        tag.putBoolean("AeEnabled", aeEnabled);
        CompoundTag li = new CompoundTag();
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            if (!lockedItems[i].isEmpty())
            {
                li.put("L" + i, lockedItems[i].save(provider));
            }
        }
        tag.put("LockedItems", li);
        if (lockedRecipeId != null)
        {
            tag.putString("LockedRecipe", lockedRecipeId.toString());
        }
        CompoundTag tm = new CompoundTag();
        for (int t = 0; t < TEMPLATE_COUNT; t++)
        {
            CompoundTag slotTag = new CompoundTag();
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                if (!templates[t][i].isEmpty())
                {
                    slotTag.put("I" + i, templates[t][i].save(provider));
                }
            }
            tm.put("T" + t, slotTag);
        }
        tag.put("Templates", tm);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        if (tag.contains("Inputs")) inputSlots.deserializeNBT(provider, tag.getCompound("Inputs"));
        if (tag.contains("Output")) outputSlot.deserializeNBT(provider, tag.getCompound("Output"));
        if (tag.contains("FaceModes"))
        {
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
        locked = tag.getBoolean("Locked");
        enabled = tag.getBoolean("Enabled");
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
        if (tag.contains("LockedItems"))
        {
            CompoundTag li = tag.getCompound("LockedItems");
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                if (li.contains("L" + i))
                {
                    lockedItems[i] = ItemStack.parseOptional(provider, li.getCompound("L" + i));
                }
            }
        }
        if (tag.contains("LockedRecipe"))
        {
            lockedRecipeId = ResourceLocation.tryParse(tag.getString("LockedRecipe"));
        }
        if (tag.contains("Templates"))
        {
            CompoundTag tm = tag.getCompound("Templates");
            for (int t = 0; t < TEMPLATE_COUNT; t++)
            {
                CompoundTag slotTag = tm.getCompound("T" + t);
                for (int i = 0; i < INPUT_SLOTS; i++)
                {
                    templates[t][i] = slotTag.contains("I" + i)
                            ? ItemStack.parseOptional(provider, slotTag.getCompound("I" + i)) : ItemStack.EMPTY;
                }
            }
        }
    }

    /** 推送方块实体数据到客户端，让模板面板/面模式即时刷新（无需区块重载） */
    private void syncToClient()
    {
        if (level != null && !level.isClientSide)
        {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    // 1.21.1：覆写 getUpdatePacket 使 sendBlockUpdated 能推送 getUpdateTag 到客户端
    // （默认实现返回 null，模板保存/加载后客户端预览无法即时刷新）
    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider)
    {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.put("Inputs", inputSlots.serializeNBT(provider));
        tag.put("Output", outputSlot.serializeNBT(provider));
        tag.putBoolean("Locked", locked);
        tag.putBoolean("Enabled", enabled);
        CompoundTag li = new CompoundTag();
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            if (!lockedItems[i].isEmpty())
            {
                li.put("L" + i, lockedItems[i].save(provider));
            }
        }
        tag.put("LockedItems", li);
        CompoundTag tm = new CompoundTag();
        for (int t = 0; t < TEMPLATE_COUNT; t++)
        {
            CompoundTag slotTag = new CompoundTag();
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                if (!templates[t][i].isEmpty())
                {
                    slotTag.put("I" + i, templates[t][i].save(provider));
                }
            }
            tm.put("T" + t, slotTag);
        }
        tag.put("Templates", tm);
        tag.putIntArray("FaceModes", faceModes);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.handleUpdateTag(tag, provider);
        if (tag.contains("Inputs")) inputSlots.deserializeNBT(provider, tag.getCompound("Inputs"));
        if (tag.contains("Output")) outputSlot.deserializeNBT(provider, tag.getCompound("Output"));
        locked = tag.getBoolean("Locked");
        enabled = tag.getBoolean("Enabled");
        if (tag.contains("LockedItems"))
        {
            CompoundTag li = tag.getCompound("LockedItems");
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                if (li.contains("L" + i))
                {
                    lockedItems[i] = ItemStack.parseOptional(provider, li.getCompound("L" + i));
                }
            }
        }
        if (tag.contains("Templates"))
        {
            CompoundTag tm = tag.getCompound("Templates");
            for (int t = 0; t < TEMPLATE_COUNT; t++)
            {
                CompoundTag slotTag = tm.getCompound("T" + t);
                for (int i = 0; i < INPUT_SLOTS; i++)
                {
                    templates[t][i] = slotTag.contains("I" + i)
                            ? ItemStack.parseOptional(provider, slotTag.getCompound("I" + i)) : ItemStack.EMPTY;
                }
            }
        }
        if (tag.contains("FaceModes"))
        {
            System.arraycopy(tag.getIntArray("FaceModes"), 0, faceModes, 0, Math.min(6, tag.getIntArray("FaceModes").length));
        }
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("container.godofthings.god_craft");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodCraftMenu(containerId, inventory, this);
    }
}
