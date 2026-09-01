package com.godofthings.block.entity;

import com.godofthings.Godofthings;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.menu.GodFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
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

    /** 神之加速槽：放入神之加速提升并行数量（最多一组 64 个 = 1024 倍） */
    private final ItemStackHandler accelSlot = new ItemStackHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return stack.getItem() instanceof GodAcceleratorItem;
        }
    };

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

        // 神之加速：输入/输出槽容量随并行倍率提升，突破单堆 64 上限
        @Override
        public int getSlotLimit(int slot)
        {
            return 64 * getParallelMultiplier();
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack)
        {
            // 不乘物品 maxStackSize，允许单堆超过 64（神之加速并行突破）
            return getSlotLimit(slot);
        }
    };

    // 每个面一个模式，索引 = Direction.get3DDataValue()，取值见 FaceMode.getId()
    private final int[] faceModes = new int[6];

    private final IItemHandler[] sideHandlers = new IItemHandler[6];

    public GodFurnaceBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_FURNACE_BE.get(), pos, state);
        for (int i = 0; i < 6; i++)
        {
            final int idx = i;
            sideHandlers[idx] = new SideHandler(idx);
        }
    }

    public ItemStackHandler getItemHandler()
    {
        return itemHandler;
    }

    /** 神之加速槽（只接受神之加速，最多 64 个） */
    public ItemStackHandler getAccelSlot()
    {
        return accelSlot;
    }

    /** 并行倍率：每个神之加速 16 倍，最多一组（64 个）= 1024 倍。无加速时为 1。 */
    public int getParallelMultiplier()
    {
        int count = accelSlot.getStackInSlot(0).getCount();
        return count <= 0 ? 1 : count * 16;
    }

    /** 判断物品是否拥有熔炼配方。不能熔炼的物品不允许进入输入槽。 */
    public boolean isSmeltable(ItemStack stack)
    {
        if (stack.isEmpty() || level == null)
        {
            return false;
        }
        // 1.21.1：getRecipeFor 入参由 Container 改为 RecipeInput，单物品探测用原版 SingleRecipeInput
        SingleRecipeInput probe = new SingleRecipeInput(stack);
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

    // NeoForge 1.21.1：BlockEntity 不可覆写 getCapability（LazyOptional 机制已移除），
    // 能力统一在 RegisterCapabilitiesEvent（MOD 总线）注册，见下方 CapabilityRegistration。
    // 注意：faceModes 在运行时可改，SideHandler 每次调用动态读取当前模式，故无需失效缓存。

    /** NeoForge 能力查询入口：按面模式返回受限 handler；side == null 或 NONE 面返回 null（与旧版 getCapability 逻辑一致）。 */
    @Nullable
    IItemHandler getSideCapability(@Nullable Direction side)
    {
        if (side == null)
        {
            return null;
        }
        int idx = side.get3DDataValue();
        return faceModes[idx] != FaceMode.NONE.getId() ? sideHandlers[idx] : null;
    }

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_FURNACE_BE.get(),
                    (be, side) -> be.getSideCapability(side));
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

            // NeoForge 1.21.1：邻居能力查询改为 Level.getCapability(BlockCapability, BlockPos, side)，null = 无能力
            IItemHandler neighborCap = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (neighborCap == null)
            {
                continue;
            }

            if (mode == FaceMode.INPUT.getId())
            {
                pullFrom(neighborCap);
            }
            else if (mode == FaceMode.OUTPUT.getId())
            {
                pushTo(neighborCap);
            }
            else if (mode == FaceMode.BOTH.getId())
            {
                // 同一个面既自动抽入原料，又自动推出产物
                pullFrom(neighborCap);
                pushTo(neighborCap);
            }
        }
    }

    /** 从邻居抽取可熔炼物品到任意有空位的输入槽。每 tick 每面遍历所有源槽（配合神之加速让输入槽积累更多原料）。 */
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
            // 1.21.1：isSameItemSameTags → isSameItemSameComponents
            else if (ItemStack.isSameItemSameComponents(cur, stack))
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

        // 1.21.1：配方探测入参由 Container 改为 RecipeInput，且烹饪配方泛型固定绑定 SingleRecipeInput
        //（AbstractCookingRecipe implements Recipe<SingleRecipeInput>），SingleSlotContainer 包装不再可用，
        // 改用原版 SingleRecipeInput 快照（matches() 只读 getItem，语义不变）
        SingleRecipeInput inputContainer = new SingleRecipeInput(input);
        // 兼容原版熔炉/高炉/烟熏炉配方
        Optional<? extends RecipeHolder<? extends AbstractCookingRecipe>> recipe =
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

        // 1.21.1：getRecipeFor 返回 Optional<RecipeHolder<T>>，配方本体经 .value() 获取
        ItemStack resultTemplate = recipe.get().value().getResultItem(level.registryAccess());
        if (resultTemplate.isEmpty())
        {
            return;
        }

        // 固定配对：输入槽 i → 输出槽 (INPUT_SLOT_COUNT + i)，6 对管线完全并行
        int outputSlot = INPUT_SLOT_COUNT + inputSlot;
        ItemStack cur = itemHandler.getStackInSlot(outputSlot);
        // 1.21.1：isSameItemSameTags → isSameItemSameComponents
        if (!cur.isEmpty() && !ItemStack.isSameItemSameComponents(cur, resultTemplate))
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

        // 配方每次熔炼的产出数量（原版多为 1，模组配方可能 >1，如 1 矿 → 2 锭）
        int perResult = Math.max(1, resultTemplate.getCount());
        int toSmelt = Math.min(input.getCount(), outputFree / perResult);
        if (toSmelt <= 0)
        {
            return;
        }
        ItemStack result = resultTemplate.copy();
        result.setCount(toSmelt * perResult);
        ItemStack leftover = itemHandler.insertItem(outputSlot, result, false);
        int placedResult = result.getCount() - leftover.getCount();
        if (placedResult > 0)
        {
            itemHandler.extractItem(inputSlot, placedResult / perResult, false);
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
            // 1.21.1：isSameItemSameTags → isSameItemSameComponents
            else if (ItemStack.isSameItemSameComponents(cur, result))
            {
                return i;
            }
        }
        return emptySlot;
    }

    // ---- NBT ----

    // 1.21.1（1.20.5+ 破坏性变更）：save(CompoundTag) → saveAdditional(CompoundTag, HolderLookup.Provider)
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.put("AccelSlot", accelSlot.serializeNBT(registries));
        tag.putIntArray("FaceModes", faceModes);
    }

    // 1.21.1（1.20.5+ 破坏性变更）：load(CompoundTag) → loadAdditional(CompoundTag, HolderLookup.Provider)
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(registries, tag.getCompound("AccelSlot"));
        }
        if (tag.contains("Inventory"))
        {
            CompoundTag inv = tag.getCompound("Inventory");
            int oldSize = inv.contains("Size") ? inv.getInt("Size") : itemHandler.getSlots();
            // 强制使用新布局的 12 槽（6 输入 + 6 输出）
            inv.putInt("Size", TOTAL_SLOTS);
            // 1.21.1：ItemStackHandler serializeNBT/deserializeNBT 需传 HolderLookup.Provider
            itemHandler.deserializeNBT(registries, inv);
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

    // 1.21.1：getUpdateTag / handleUpdateTag 均携带 HolderLookup.Provider
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries)
    {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putIntArray("FaceModes", faceModes);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.handleUpdateTag(tag, registries);
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
