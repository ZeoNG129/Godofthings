package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
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

import java.util.Optional;

/**
 * 神之熔炉方块实体。
 * - 无燃料，3 输入 + 3 输出，每个输入槽每 tick 最多把一整组(64)熔炼完毕
 * - 六个面各自可配置 NONE / INPUT(自动抽入) / OUTPUT(自动推出)，见 {@link FaceMode}
 * - 每 tick 先自动传输补料，再熔炼
 */
public class GodFurnaceBlockEntity extends BlockEntity implements MenuProvider
{
    public static final int INPUT_SLOT_COUNT = 6;
    public static final int OUTPUT_SLOT_COUNT = 6;
    public static final int TOTAL_SLOTS = INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT;
    public static final int OUTPUT_SLOT_START = INPUT_SLOT_COUNT;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        // 规则：不能熔炼的物品不允许进入输入槽；输出槽对内部熔炼写入放行（外部插入由 SideHandler/mayPlace 拦截）
        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            if (slot < INPUT_SLOT_COUNT)
            {
                return isSmeltable(stack);
            }
            return true;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (slot < INPUT_SLOT_COUNT && !isSmeltable(stack))
            {
                return stack;
            }
            // 输出槽：仅内部熔炼写入，外部调用走 SideHandler（只允许输入槽）
            return super.insertItem(slot, stack, simulate);
        }
    };

    // 每个面一个模式，索引 = Direction.get3DDataValue()，取值见 FaceMode.getId()
    private final int[] faceModes = new int[6];

    private final IItemHandler[] sideHandlers = new IItemHandler[6];
    @SuppressWarnings("unchecked")
    private final LazyOptional<IItemHandler>[] sideCaps = new LazyOptional[6];

    public GodFurnaceBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_FURNACE_BE.get(), pos, state);
        for (int i = 0; i < 6; i++)
        {
            final int idx = i;
            sideHandlers[idx] = new SideHandler(idx);
            sideCaps[idx] = LazyOptional.of(() -> sideHandlers[idx]);
        }
    }

    public ItemStackHandler getItemHandler()
    {
        return itemHandler;
    }

    /** 判断物品是否拥有熔炼配方。不能熔炼的物品不允许进入输入槽。 */
    public boolean isSmeltable(ItemStack stack)
    {
        if (stack.isEmpty() || level == null)
        {
            return false;
        }
        SimpleContainer probe = new SimpleContainer(stack);
        // 兼容原版熔炉/高炉/烟熏炉配方
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, probe, level).isPresent()
                || level.getRecipeManager().getRecipeFor(RecipeType.BLASTING, probe, level).isPresent()
                || level.getRecipeManager().getRecipeFor(RecipeType.SMOKING, probe, level).isPresent();
    }

    // ---- 面模式 ----

    public int getFaceMode(Direction dir)
    {
        return faceModes[dir.get3DDataValue()];
    }

    public void setFaceMode(Direction dir, int mode)
    {
        faceModes[dir.get3DDataValue()] = ((mode % 4) + 4) % 4;
        setChanged();
    }

    public void cycleFaceMode(Direction dir)
    {
        setFaceMode(dir, getFaceMode(dir) + 1);
    }

    // ---- capability：每个面按模式暴露受限的 IItemHandler ----

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
        for (LazyOptional<IItemHandler> cap : sideCaps)
        {
            cap.invalidate();
        }
    }

    /**
     * 某个面的包装 handler：INPUT 面只能插入输入槽，OUTPUT 面只能从输出槽提取。
     */
    private class SideHandler implements IItemHandler
    {
        private final int dirIndex;

        SideHandler(int dirIndex)
        {
            this.dirIndex = dirIndex;
        }

        private FaceMode mode()
        {
            return FaceMode.fromId(faceModes[dirIndex]);
        }

        @Override
        public int getSlots()
        {
            return TOTAL_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (slot < 0 || slot >= INPUT_SLOT_COUNT)
            {
                return stack; // 只能插入输入槽
            }
            return (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH)
                    ? itemHandler.insertItem(slot, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (slot < OUTPUT_SLOT_START || slot >= TOTAL_SLOTS)
            {
                return ItemStack.EMPTY; // 只能从输出槽提取
            }
            return (mode() == FaceMode.OUTPUT || mode() == FaceMode.BOTH)
                    ? itemHandler.extractItem(slot, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return slot < INPUT_SLOT_COUNT
                    && (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH)
                    && GodFurnaceBlockEntity.this.isSmeltable(stack);
        }
    }

    // ---- tick：先自动传输，再熔炼 ----

    public static void tick(Level level, BlockPos pos, BlockState state, GodFurnaceBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.autoTransfer();
        be.smelt();
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

            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos))
            {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null)
            {
                continue;
            }

            LazyOptional<IItemHandler> neighborCap = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
            if (mode == FaceMode.INPUT.getId())
            {
                neighborCap.ifPresent(this::pullFrom);
            }
            else if (mode == FaceMode.OUTPUT.getId())
            {
                neighborCap.ifPresent(this::pushTo);
            }
            else if (mode == FaceMode.BOTH.getId())
            {
                // 同一个面既自动抽入原料，又自动推出产物
                neighborCap.ifPresent(handler ->
                {
                    pullFrom(handler);
                    pushTo(handler);
                });
            }
        }
    }

    /** 从邻居抽取可熔炼物品到任意有空位的输入槽。每 tick 每面只处理一个源槽。 */
    private void pullFrom(IItemHandler neighbor)
    {
        for (int s = 0; s < neighbor.getSlots(); s++)
        {
            ItemStack src = neighbor.getStackInSlot(s);
            if (src.isEmpty() || !isSmeltable(src))
            {
                continue;
            }

            int targetSlot = findInputSlotFor(src);
            if (targetSlot < 0)
            {
                continue;
            }

            ItemStack leftoverSim = itemHandler.insertItem(targetSlot, src, true);
            int canMove = src.getCount() - leftoverSim.getCount();
            if (canMove <= 0)
            {
                continue;
            }

            ItemStack extracted = neighbor.extractItem(s, canMove, true);
            if (extracted.isEmpty())
            {
                continue;
            }
            int toMove = Math.min(extracted.getCount(), canMove);
            if (toMove <= 0)
            {
                continue;
            }

            ItemStack remaining = itemHandler.insertItem(targetSlot, extracted, false);
            int placed = toMove - remaining.getCount();
            if (placed > 0)
            {
                neighbor.extractItem(s, placed, false);
                setChanged();
            }
            break; // 每 tick 每面只处理 1 个槽
        }
    }

    /** 找到可接受该原料的输入槽（同物品优先，否则找空槽）；没有则 -1 */
    private int findInputSlotFor(ItemStack stack)
    {
        int emptySlot = -1;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++)
        {
            ItemStack cur = itemHandler.getStackInSlot(i);
            if (cur.isEmpty())
            {
                if (emptySlot < 0)
                {
                    emptySlot = i;
                }
            }
            else if (ItemStack.isSameItemSameTags(cur, stack))
            {
                return i;
            }
        }
        return emptySlot;
    }

    /** 把任意有产物的输出格物品推送给邻居，直到清空或插不下。 */
    private void pushTo(IItemHandler neighbor)
    {
        for (int out = OUTPUT_SLOT_START; out < TOTAL_SLOTS; out++)
        {
            ItemStack output = itemHandler.getStackInSlot(out);
            if (output.isEmpty())
            {
                continue;
            }
            ItemStack toPush = output.copy();
            for (int s = 0; s < neighbor.getSlots(); s++)
            {
                ItemStack leftover = neighbor.insertItem(s, toPush, false);
                int moved = toPush.getCount() - leftover.getCount();
                if (moved > 0)
                {
                    itemHandler.extractItem(out, moved, false);
                }
                toPush = leftover;
                if (toPush.isEmpty())
                {
                    break;
                }
            }
        }
        setChanged();
    }

    // ---- 熔炼：无燃料，每个输入槽每 tick 最多熔一整组 ----

    private void smelt()
    {
        for (int i = 0; i < INPUT_SLOT_COUNT; i++)
        {
            smeltSlot(i);
        }
    }

    private void smeltSlot(int inputSlot)
    {
        ItemStack input = itemHandler.getStackInSlot(inputSlot);
        if (input.isEmpty())
        {
            return;
        }

        // 兼容原版熔炉/高炉/烟熏炉配方
        Container inputContainer = new SingleSlotContainer(inputSlot);
        Optional<? extends AbstractCookingRecipe> recipe =
                level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, inputContainer, level);
        if (recipe.isEmpty())
        {
            recipe = level.getRecipeManager().getRecipeFor(RecipeType.BLASTING, inputContainer, level);
        }
        if (recipe.isEmpty())
        {
            recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMOKING, inputContainer, level);
        }
        if (recipe.isEmpty())
        {
            return;
        }

        ItemStack resultTemplate = recipe.get().getResultItem(level.registryAccess());
        if (resultTemplate.isEmpty())
        {
            return;
        }

        // 固定配对：输入槽 i → 输出槽 (INPUT_SLOT_COUNT + i)，6 对管线完全并行
        int outputSlot = INPUT_SLOT_COUNT + inputSlot;
        ItemStack cur = itemHandler.getStackInSlot(outputSlot);
        if (!cur.isEmpty() && !ItemStack.isSameItemSameTags(cur, resultTemplate))
        {
            // 对应输出槽被不同物品占住：回退到其他空槽
            outputSlot = findOutputSlotFor(resultTemplate);
            if (outputSlot < 0)
            {
                return;
            }
        }

        ItemStack output = itemHandler.getStackInSlot(outputSlot);
        int limit = itemHandler.getSlotLimit(outputSlot);
        int outputFree = output.isEmpty() ? limit : limit - output.getCount();
        if (outputFree <= 0)
        {
            return;
        }

        int toSmelt = Math.min(input.getCount(), outputFree);
        ItemStack result = resultTemplate.copy();
        result.setCount(toSmelt);
        ItemStack leftover = itemHandler.insertItem(outputSlot, result, false);
        int placed = toSmelt - leftover.getCount();
        if (placed > 0)
        {
            itemHandler.extractItem(inputSlot, placed, false);
            setChanged();
        }
    }

    /** 找到可放入该产物的输出格（同物品优先，否则找空槽）；没有则 -1 */
    private int findOutputSlotFor(ItemStack result)
    {
        int emptySlot = -1;
        for (int i = OUTPUT_SLOT_START; i < TOTAL_SLOTS; i++)
        {
            ItemStack cur = itemHandler.getStackInSlot(i);
            if (cur.isEmpty())
            {
                if (emptySlot < 0)
                {
                    emptySlot = i;
                }
            }
            else if (ItemStack.isSameItemSameTags(cur, result))
            {
                return i;
            }
        }
        return emptySlot;
    }

    /** 单个输入槽的容器包装，供 getRecipeFor 使用 */
    private class SingleSlotContainer implements Container
    {
        private final int slot;

        SingleSlotContainer(int slot)
        {
            this.slot = slot;
        }

        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return itemHandler.getStackInSlot(slot).isEmpty(); }
        @Override public ItemStack getItem(int index) { return itemHandler.getStackInSlot(slot); }
        @Override public ItemStack removeItem(int index, int amount) { return itemHandler.extractItem(slot, amount, false); }
        @Override public ItemStack removeItemNoUpdate(int index) { return ItemStack.EMPTY; }
        @Override public void setItem(int index, ItemStack stack) { itemHandler.setStackInSlot(slot, stack); }
        @Override public void setChanged() { GodFurnaceBlockEntity.this.setChanged(); }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { itemHandler.setStackInSlot(slot, ItemStack.EMPTY); }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.putIntArray("FaceModes", faceModes);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        if (tag.contains("Inventory"))
        {
            CompoundTag inv = tag.getCompound("Inventory");
            int oldSize = inv.contains("Size") ? inv.getInt("Size") : itemHandler.getSlots();
            // 强制使用新布局的 12 槽（6 输入 + 6 输出）
            inv.putInt("Size", TOTAL_SLOTS);
            itemHandler.deserializeNBT(inv);
            // 兼容旧版布局：
            // - 旧 2 槽（1输入+1输出）：旧输出在 slot1 → 迁到新输出区
            // - 旧 6 槽（3输入+3输出）：旧输出在 slot3-5 → 迁到新输出区(slot6+)
            // 迁移规则：扫描所有"曾是输出但现在落在输入区"的槽位，把物品移到新输出区空槽
            if (oldSize < TOTAL_SLOTS)
            {
                int oldOutputStart = Math.max(1, oldSize / 2); // 旧布局输出区起点（2槽→1, 6槽→3）
                for (int legacy = oldOutputStart; legacy < Math.min(oldSize, INPUT_SLOT_COUNT); legacy++)
                {
                    ItemStack stack = itemHandler.getStackInSlot(legacy);
                    if (!stack.isEmpty())
                    {
                        int target = findOutputSlotFor(stack);
                        if (target < 0)
                        {
                            target = firstEmptyOutputSlot();
                        }
                        if (target >= 0)
                        {
                            itemHandler.setStackInSlot(target, stack);
                            itemHandler.setStackInSlot(legacy, ItemStack.EMPTY);
                        }
                    }
                }
            }
        }
        if (tag.contains("FaceModes"))
        {
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
    }

    /** 第一个空输出槽；没有则 -1 */
    private int firstEmptyOutputSlot()
    {
        for (int i = OUTPUT_SLOT_START; i < TOTAL_SLOTS; i++)
        {
            if (itemHandler.getStackInSlot(i).isEmpty())
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag tag = super.getUpdateTag();
        tag.putIntArray("FaceModes", faceModes);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        super.handleUpdateTag(tag);
        if (tag.contains("FaceModes"))
        {
            System.arraycopy(tag.getIntArray("FaceModes"), 0, faceModes, 0, 6);
        }
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("container.godofthings.god_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodFurnaceMenu(containerId, inventory, this);
    }
}
