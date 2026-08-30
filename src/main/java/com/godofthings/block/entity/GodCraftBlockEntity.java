package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodCraftMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 神之合成：自动合成工作台。
 * - 3×3 合成格 + 1 输出槽
 * - 每 tick 尝试合成（需先开启开关）
 * - 输出槽放不下则停止（不消耗原料）
 * - 锁定配方：锁定后只合成锁定配方，合成格只接受锁定模板物品
 * - 六个面可配置输入/输出
 */
public class GodCraftBlockEntity extends BlockEntity implements MenuProvider
{
    public static final int INPUT_SLOTS = 9;
    public static final int TOTAL_SLOTS = 10; // 0-8 合成格, 9 输出

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
    private net.minecraft.resources.ResourceLocation lockedRecipeId = null;

    /** 配方模板：8 个命名槽位，每个保存一份 9 格配方（仅记录物品类型，不存数量） */
    public static final int TEMPLATE_COUNT = 8;
    private final ItemStack[][] templates = new ItemStack[TEMPLATE_COUNT][INPUT_SLOTS];

    private final int[] faceModes = new int[6];
    private final IItemHandler[] sideHandlers = new IItemHandler[6];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] sideCaps = new LazyOptional[6];

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
            sideCaps[idx] = LazyOptional.of(() -> sideHandlers[idx]);
        }
    }

    public ItemStackHandler getInputSlots() { return inputSlots; }
    public ItemStackHandler getOutputSlot() { return outputSlot; }

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

    private net.minecraft.resources.ResourceLocation findCurrentRecipeId()
    {
        if (level == null)
        {
            return null;
        }
        TransientCraftingContainer inv = makeCraftingContainer();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, inv, level)
                .map(CraftingRecipe::getId).orElse(null);
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

    private TransientCraftingContainer makeCraftingContainer()
    {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++)
        {
            items.set(i, inputSlots.getStackInSlot(i));
        }
        return new TransientCraftingContainer(null, 3, 3, items);
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
        return true;
    }

    /** 统计背包中与模板物品相同（含 NBT）的总数量。 */
    private static int countInInventory(Inventory inv, ItemStack tpl)
    {
        int count = 0;
        for (int s = 0; s < inv.getContainerSize(); s++)
        {
            ItemStack st = inv.getItem(s);
            if (ItemStack.isSameItemSameTags(st, tpl))
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
            if (ItemStack.isSameItemSameTags(st, tpl))
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
        TransientCraftingContainer inv = new TransientCraftingContainer(null, 3, 3, items);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, inv, level)
                .map(r -> r.assemble(inv, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    // ---- 面模式 ----

    public int getFaceMode(Direction dir) { return faceModes[dir.get3DDataValue()]; }
    public void setFaceMode(Direction dir, int mode)
    {
        faceModes[dir.get3DDataValue()] = ((mode % 4) + 4) % 4;
        setChanged();
    }
    public void cycleFaceMode(Direction dir) { setFaceMode(dir, getFaceMode(dir) + 1); }

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
            LazyOptional<IItemHandler> cap = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
            if (mode == FaceMode.INPUT.getId() || mode == FaceMode.BOTH.getId())
            {
                cap.ifPresent(this::pullInput);
            }
            if (mode == FaceMode.OUTPUT.getId() || mode == FaceMode.BOTH.getId())
            {
                cap.ifPresent(this::pushOutput);
            }
        }
    }

    private void pullInput(IItemHandler neighbor)
    {
        for (int s = 0; s < neighbor.getSlots(); s++)
        {
            ItemStack src = neighbor.getStackInSlot(s);
            if (src.isEmpty()) continue;
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                // 锁定后：只往锁定模板非空且匹配的槽位补充，空模板槽位不塞任何物品
                if (locked)
                {
                    ItemStack tpl = lockedItems[i];
                    if (tpl.isEmpty())
                    {
                        continue; // 锁定模板该槽为空 → 不放入
                    }
                    if (!ItemStack.isSameItem(tpl, src))
                    {
                        continue; // 不是锁定物品 → 不放入
                    }
                }
                ItemStack cur = inputSlots.getStackInSlot(i);
                if (cur.isEmpty() || ItemStack.isSameItem(cur, src))
                {
                    ItemStack leftover = inputSlots.insertItem(i, src, true);
                    int can = src.getCount() - leftover.getCount();
                    if (can > 0)
                    {
                        inputSlots.insertItem(i, neighbor.extractItem(s, can, false), false);
                        return;
                    }
                }
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
        // 锁定后：合成格必须与锁定模板一致
        if (!matchesLockedTemplate())
        {
            return;
        }
        TransientCraftingContainer craftInv = makeCraftingContainer();
        var recipeOpt = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftInv, level);
        if (recipeOpt.isEmpty())
        {
            return;
        }
        CraftingRecipe recipe = recipeOpt.get();
        if (locked && lockedRecipeId != null && !lockedRecipeId.equals(recipe.getId()))
        {
            return;
        }
        ItemStack result = recipe.assemble(craftInv, level.registryAccess());
        if (result.isEmpty())
        {
            return;
        }
        // 找能完整放下产物的位置：优先输出槽，其次面配置输出方向的相邻容器
        boolean canStore = outputSlot.insertItem(0, result, true).getCount() < result.getCount()
                || canPushResultFully(result);
        if (!canStore)
        {
            return; // 输出槽满 + 所有输出方向容器都放不下 → 停止，不消耗原料
        }
        // 消耗原料
        for (int i = 0; i < 9; i++)
        {
            ItemStack need = craftInv.getItem(i);
            if (need.isEmpty()) continue;
            inputSlots.extractItem(i, 1, false);
        }
        // 优先放入输出槽，放不下则直接推给相邻容器
        ItemStack remaining = outputSlot.insertItem(0, result, false);
        if (remaining.getCount() > 0)
        {
            pushResultToNeighbors(remaining);
        }
        setChanged();
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
            IItemHandler handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
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
            IItemHandler handler = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
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

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null)
        {
            int idx = side.get3DDataValue();
            if (faceModes[idx] != FaceMode.NONE.getId())
            {
                return sideCaps[idx].cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        for (LazyOptional<IItemHandler> c : sideCaps) c.invalidate();
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
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("Inputs", inputSlots.serializeNBT());
        tag.put("Output", outputSlot.serializeNBT());
        tag.putIntArray("FaceModes", faceModes);
        tag.putBoolean("Locked", locked);
        tag.putBoolean("Enabled", enabled);
        CompoundTag li = new CompoundTag();
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            if (!lockedItems[i].isEmpty())
            {
                li.put("L" + i, lockedItems[i].save(new CompoundTag()));
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
                    slotTag.put("I" + i, templates[t][i].save(new CompoundTag()));
                }
            }
            tm.put("T" + t, slotTag);
        }
        tag.put("Templates", tm);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        if (tag.contains("Inputs")) inputSlots.deserializeNBT(tag.getCompound("Inputs"));
        if (tag.contains("Output")) outputSlot.deserializeNBT(tag.getCompound("Output"));
        if (tag.contains("FaceModes"))
        {
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
        locked = tag.getBoolean("Locked");
        enabled = tag.getBoolean("Enabled");
        if (tag.contains("LockedItems"))
        {
            CompoundTag li = tag.getCompound("LockedItems");
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                if (li.contains("L" + i))
                {
                    lockedItems[i] = ItemStack.of(li.getCompound("L" + i));
                }
            }
        }
        if (tag.contains("LockedRecipe"))
        {
            lockedRecipeId = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("LockedRecipe"));
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
                            ? ItemStack.of(slotTag.getCompound("I" + i)) : ItemStack.EMPTY;
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag tag = super.getUpdateTag();
        tag.put("Inputs", inputSlots.serializeNBT());
        tag.put("Output", outputSlot.serializeNBT());
        tag.putBoolean("Locked", locked);
        tag.putBoolean("Enabled", enabled);
        CompoundTag li = new CompoundTag();
        for (int i = 0; i < INPUT_SLOTS; i++)
        {
            if (!lockedItems[i].isEmpty())
            {
                li.put("L" + i, lockedItems[i].save(new CompoundTag()));
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
                    slotTag.put("I" + i, templates[t][i].save(new CompoundTag()));
                }
            }
            tm.put("T" + t, slotTag);
        }
        tag.put("Templates", tm);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        super.handleUpdateTag(tag);
        if (tag.contains("Inputs")) inputSlots.deserializeNBT(tag.getCompound("Inputs"));
        if (tag.contains("Output")) outputSlot.deserializeNBT(tag.getCompound("Output"));
        locked = tag.getBoolean("Locked");
        enabled = tag.getBoolean("Enabled");
        if (tag.contains("LockedItems"))
        {
            CompoundTag li = tag.getCompound("LockedItems");
            for (int i = 0; i < INPUT_SLOTS; i++)
            {
                if (li.contains("L" + i))
                {
                    lockedItems[i] = ItemStack.of(li.getCompound("L" + i));
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
                            ? ItemStack.of(slotTag.getCompound("I" + i)) : ItemStack.EMPTY;
                }
            }
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
