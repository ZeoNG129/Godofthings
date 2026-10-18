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
import com.godofthings.config.MachinesConfig;
import com.godofthings.godgrid.GodGridHost;
import com.godofthings.godgrid.GodGridLayout;
import com.godofthings.godgrid.GodGridSorting;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.menu.GodSpawnEggMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
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

import java.util.List;

/**
 * 神之怪蛋方块实体：刷怪蛋复制机。
 * - 无需能源，{@link #INPUT_SLOT_COUNT} 个刷怪蛋输入槽并行复制；每 20 tick 处理一轮所有非空刷怪蛋
 * - 放入刷怪蛋 → 产出**同种**刷怪蛋（保留原物品的全部组件，所以模组刷怪蛋的实体数据一并复制）
 * - **兼容所有刷怪蛋**：原版 / NeoForge 系直接认；其它模组自建的刷怪蛋只要按原版约定写了
 *   ENTITY_DATA 组件也认（见 {@link com.godofthings.block.entity.machine.SpawnEggHelper}）
 * - 每种产物每周期 64 个（神之加速按并行倍率乘）
 * - 不消耗刷怪蛋（生产模板，按时间持续产出）
 * - 向下自动输出，内置无限储存；打掉不掉落
 *
 * <p>v5.18.0：槽位迁移到统一网格框架（{@link GodGridLayout}，17 输入 + 17 输出 + 1 加速槽），
 * 输入（刷怪蛋模板）在前 {@link #INPUT_SLOT_COUNT} 格、产物输出在后
 * {@link #OUTPUT_SLOT_COUNT} 格。原本的「内部无限储存」保留为输出槽装不下时的兜底缓冲
 * （NBT 键 {@code Inventory} 与格式都不变），所以产物照旧永不外吐。</p>
 */
public class GodSpawnEggBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity, MachineOwner, GodGridHost
{
    /** 主人（放置者）：神之共鸣判定用（主人在线 + 穿齐全套 + 共鸣开关开才生效）。 */
    @org.jetbrains.annotations.Nullable
    private java.util.UUID owner;

    @Override
    public void setOwner(@org.jetbrains.annotations.Nullable java.util.UUID owner)
    {
        this.owner = owner;
        setChanged();
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public java.util.UUID getOwner()
    {
        return owner;
    }

    // v5.18.0：统一 17 输入 + 17 输出网格（照 PBG 无限离心机工厂布局，见 GodGridLayout）
    /** 刷怪蛋输入槽数量（网格第一行） */
    public static final int INPUT_SLOT_COUNT = GodGridLayout.INPUT_COUNT;
    /** 复制产物输出槽数量（网格第二行） */
    public static final int OUTPUT_SLOT_COUNT = GodGridLayout.OUTPUT_COUNT;
    public static final int OUTPUT_SLOT_START = INPUT_SLOT_COUNT;
    public static final int TOTAL_SLOTS = INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT;

    /**
     * 机器槽位：0..{@link #INPUT_SLOT_COUNT}-1 放刷怪蛋模板，其后 {@link #OUTPUT_SLOT_COUNT} 格是产物。
     * <p>输出格对 {@link #isItemValid} 返回 true 只是为了放行机器内部写入；对外插入由
     * {@link #getPipeView()}（只出不进）与界面 Slot#mayPlace 拦截。</p>
     */
    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            if (slot < INPUT_SLOT_COUNT)
            {
                return com.godofthings.block.entity.machine.SpawnEggHelper.isSpawnEgg(stack);
            }
            return true;
        }

        @Override
        public void setSize(int size)
        {
            // 槽数固定为 TOTAL_SLOTS：旧世界（2.0.5 / v5.17.x）保存的 NBT 里 Size=1 或 9，
            // deserializeNBT 会调 setSize 缩槽，导致 tickServer 遍历越界崩溃，忽略之。
            if (size != TOTAL_SLOTS)
            {
                return;
            }
            super.setSize(size);
        }
    };

    /** 内部无限储存：输出槽装不下时的兜底缓冲（NBT 键 {@code Inventory}，格式与旧版一致） */
    private final InfiniteItemHandler innerStore = new InfiniteItemHandler();

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

    /** 是否接入 AE（并网后产物自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网）。 */
    private final AeGridNode aeNode = new AeGridNode(this);
    private int aeTick = 0;

    private int tickCounter = 0;

    /** 输出槽整理模式（0=名称 1=模组 2=数量），随方块实体持久化 */
    private int sortMode = GodGridSorting.MODE_NAME;

    public GodSpawnEggBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_SPAWN_EGG_BE.get(), pos, state);
        innerStore.setOnChange(this::setChanged);
    }

    /** 机器槽位（17 输入 + 17 输出） */
    public ItemStackHandler getItemHandler()
    {
        return itemHandler;
    }

    /** 内置无限储存（兜底缓冲，物品种类数用于界面统计） */
    public InfiniteItemHandler getInnerStore()
    {
        return innerStore;
    }

    /**
     * 管道 / 漏斗视图：输出槽在前、内部储存兜底在后，<b>只出不进</b>
     * （旧版直接把内部储存暴露给能力系统，外部可以往里塞任意物品）。
     */
    public IItemHandler getPipeView()
    {
        return pipeView;
    }

    private final IItemHandler pipeView = new IItemHandler()
    {
        @Override
        public int getSlots()
        {
            return OUTPUT_SLOT_COUNT + innerStore.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return slot < OUTPUT_SLOT_COUNT
                    ? itemHandler.getStackInSlot(OUTPUT_SLOT_START + slot)
                    : innerStore.getStackInSlot(slot - OUTPUT_SLOT_COUNT);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            return stack; // 产物只出不进
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return slot < OUTPUT_SLOT_COUNT
                    ? itemHandler.extractItem(OUTPUT_SLOT_START + slot, amount, simulate)
                    : innerStore.extractItem(slot - OUTPUT_SLOT_COUNT, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return slot < OUTPUT_SLOT_COUNT
                    ? itemHandler.getSlotLimit(OUTPUT_SLOT_START + slot)
                    : innerStore.getSlotLimit(slot - OUTPUT_SLOT_COUNT);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return false;
        }
    };

    public boolean isAeEnabled()
    {
        return aeEnabled;
    }

    public void toggleAeEnabled()
    {
        this.aeEnabled = !this.aeEnabled;
        setChanged();
    }

    // ---- AE 网格节点（线缆直连并网，产物自动输出进 AE） ----

    @Override
    public IManagedGridNode getMainNode() { return aeNode.getMainNode(); }

    @Override
    public void saveChanges() { setChanged(); }

    /** 把产物推入 AE 网络（节流由 tick 控制）。 */
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
        for (int slot = OUTPUT_SLOT_START; slot < TOTAL_SLOTS; slot++)
        {
            pushSlotToAe(inv, source, itemHandler, slot);
        }
        // 内部储存兜底：抽空一格后后面的堆会前移，倒序遍历避免跳项
        for (int slot = innerStore.getSlots() - 1; slot >= 0; slot--)
        {
            pushSlotToAe(inv, source, innerStore, slot);
        }
    }

    private static void pushSlotToAe(MEStorage inv, IActionSource source, IItemHandler from, int slot)
    {
        ItemStack stack = from.getStackInSlot(slot);
        if (stack.isEmpty())
        {
            return;
        }
        long inserted = inv.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
        if (inserted > 0)
        {
            from.extractItem(slot, (int) Math.min(inserted, Integer.MAX_VALUE), false);
        }
    }

    /** 神之加速槽（只接受神之加速，最多 64 个） */
    public ItemStackHandler getAccelSlot()
    {
        return accelSlot;
    }

    /** 并行倍率：每个神之加速 16 倍，最多一组（64 个）= 1024 倍。无加速时为 1。（倍率权威值见 {@link GodAcceleratorItem#PARALLEL_PER_ITEM}） */
    public int getParallelMultiplier()
    {
        return GodAcceleratorItem.multiplierFor(accelSlot.getStackInSlot(0).getCount());
    }

    /** 内置无限储存里的物品种类数（界面统计用） */
    public int getStorageCount()
    {
        return innerStore.getStacks().size();
    }

    // ---- 生命周期：创建/销毁 AE 网格节点 ----

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

    // ---- 每 tick 逻辑 ----

    public static void tick(Level level, BlockPos pos, BlockState state, GodSpawnEggBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.tickServer();
    }

    private void tickServer()
    {
        tickCounter++;
        if (tickCounter >= MachinesConfig.SPAWN_EGG_WORK_INTERVAL.get())
        {
            tickCounter = 0;
            for (int i = 0; i < INPUT_SLOT_COUNT; i++)
            {
                process(i);
            }
        }
        pushDown();
        // AE 产物输出节流：每 20 tick（1 秒）推一次
        aeTick++;
        if (aeTick >= 20)
        {
            aeTick = 0;
            pushOutputToAe();
        }
    }

    private void process(int slot)
    {
        ItemStack input = itemHandler.getStackInSlot(slot);
        if (input.isEmpty())
        {
            return;
        }
        List<ItemStack> outputs = produce(input);
        if (outputs.isEmpty())
        {
            return;
        }
        int mult = getParallelMultiplier();
        // 神之共鸣·掉落（v5.15.4）：战利品爆炸倍率（主人在线 + 穿齐全套 + 共鸣开关开）
        double bomb = level instanceof net.minecraft.server.level.ServerLevel serverLevel
                ? com.godofthings.handler.ArmorSkillHandler.lootBombBoost(serverLevel, owner) : 1.0;
        // 不消耗刷怪蛋：生产模板，按时间持续产出（神之加速提升并行数量）
        for (ItemStack out : outputs)
        {
            if (!out.isEmpty())
            {
                insertToOutputs(out.copyWithCount((int) Math.min(Integer.MAX_VALUE / 2L,
                        Math.round(out.getCount() * mult * bomb))));
            }
        }
    }

    /**
     * 产物先写进 17 个输出槽（同物品自动叠），装不下再落进内部无限储存，
     * 最后才掉在机器上方（后两步沿用旧版「永不丢产物」的兜底）。
     */
    private void insertToOutputs(ItemStack stack)
    {
        ItemStack leftover = stack;
        for (int j = 0; j < OUTPUT_SLOT_COUNT && !leftover.isEmpty(); j++)
        {
            leftover = itemHandler.insertItem(OUTPUT_SLOT_START + j, leftover.copy(), false);
        }
        if (!leftover.isEmpty())
        {
            leftover = innerStore.insertItem(-1, leftover, false);
        }
        if (!leftover.isEmpty())
        {
            InfiniteItemHandler.dropRemainder(level, worldPosition, leftover);
        }
    }

    /**
     * 复制刷怪蛋：产出与输入**同种**的刷怪蛋（每周期 64 个，神之加速按倍率乘）。
     *
     * <p>用 {@code copyWithCount} 复制整份物品栈，所以输入蛋上挂的组件（实体数据等）一并带过去 ——
     * 模组刷怪蛋的实体类型、甚至被改过名字/加了数据的蛋都能原样复制。
     * 输入不是刷怪蛋时（正常插不进来，兜底）返回空表。</p>
     */
    private List<ItemStack> produce(ItemStack input)
    {
        return com.godofthings.block.entity.machine.SpawnEggHelper.duplicate(input);
    }

    /** 向下自动输出到下方容器：先推 17 个输出槽，再推内部储存（旧版逻辑）。 */
    private void pushDown()
    {
        BlockPos below = worldPosition.below();
        if (!level.isLoaded(below))
        {
            return;
        }
        BlockEntity neighbor = level.getBlockEntity(below);
        if (neighbor == null)
        {
            return;
        }
        // 1.21.1：BE 不再覆盖 getCapability；Level.getCapability(BlockCapability, BlockPos, side) 直接返回能力对象（null = 无能力）
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, below, Direction.UP);
        if (handler == null)
        {
            return;
        }
        for (int j = 0; j < OUTPUT_SLOT_COUNT; j++)
        {
            pushSlotInto(handler, itemHandler, OUTPUT_SLOT_START + j);
        }
        // 内部储存兜底：抽空槽 0 后后面的堆会前移，所以反复推槽 0（旧版写法）
        while (true)
        {
            ItemStack stack = innerStore.getStackInSlot(0);
            if (stack.isEmpty())
            {
                break;
            }
            ItemStack toPush = stack.copy();
            boolean anyMoved = false;
            for (int s = 0; s < handler.getSlots(); s++)
            {
                ItemStack leftover = handler.insertItem(s, toPush, false);
                int moved = toPush.getCount() - leftover.getCount();
                if (moved > 0)
                {
                    innerStore.extractItem(0, moved, false);
                    anyMoved = true;
                }
                toPush = leftover;
                if (toPush.isEmpty())
                {
                    break;
                }
            }
            if (!anyMoved)
            {
                break;
            }
        }
    }

    /** 把 source 的某个槽尽量推进 target（推不动或推空为止）。 */
    private static void pushSlotInto(IItemHandler target, IItemHandler source, int slot)
    {
        while (true)
        {
            ItemStack stack = source.getStackInSlot(slot);
            if (stack.isEmpty())
            {
                return;
            }
            ItemStack toPush = stack.copy();
            boolean anyMoved = false;
            for (int s = 0; s < target.getSlots() && !toPush.isEmpty(); s++)
            {
                ItemStack leftover = target.insertItem(s, toPush, false);
                int moved = toPush.getCount() - leftover.getCount();
                if (moved > 0)
                {
                    source.extractItem(slot, moved, false);
                    anyMoved = true;
                }
                toPush = leftover;
            }
            if (!anyMoved)
            {
                return;
            }
        }
    }

    // ---- capability 注册：任意面都能取走产物 ----
    // 1.21.1：BE 不再覆盖 getCapability/LazyOptional，能力经 RegisterCapabilitiesEvent（MOD 总线）集中注册

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistrar
    {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_SPAWN_EGG_BE.get(),
                    (be, side) -> be.getPipeView());
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_SPAWN_EGG_BE.get(),
                    (be, side) -> be);
        }
    }

    // ---- GodGridHost：共用网格界面所需的机器侧契约 ----

    @Override
    public IItemHandler gridItems()
    {
        return itemHandler;
    }

    @Override
    public IItemHandler gridAccel()
    {
        return accelSlot;
    }

    @Override
    public boolean gridAeEnabled()
    {
        return aeEnabled;
    }

    @Override
    public void gridToggleAe()
    {
        toggleAeEnabled();
    }

    @Override
    public int gridSortMode()
    {
        return sortMode;
    }

    @Override
    public int gridSortOutputs()
    {
        sortMode = GodGridSorting.nextMode(sortMode);
        GodGridSorting.sort(itemHandler, OUTPUT_SLOT_START, OUTPUT_SLOT_COUNT, sortMode);
        setChanged();
        return sortMode;
    }

    /**
     * 本机没有六面 I/O 配置（不按面抽入/推出，只在下方输出），因此面配置按钮不打开任何界面。
     */
    @Override
    public void gridOpenFaceConfig(ServerPlayer player)
    {
        // 无面配置菜单：保留空实现以满足 GodGridHost 契约
    }

    @Override
    public boolean gridSupportsFaceConfig()
    {
        // 本机没有六面配置：界面上不显示左侧页签
        return false;
    }

    @Override
    public int gridOperationsPerColumn()
    {
        return GodGridLayout.operationsPerColumn(accelSlot.getStackInSlot(0).getCount());
    }

    // ---- NBT（1.20.5+：save/load 需 HolderLookup.Provider） ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        if (owner != null)
        {
            tag.putUUID("Owner", owner);
        }
        tag.put("GridInventory", itemHandler.serializeNBT(registries));
        tag.put("Inventory", innerStore.serializeNBT(registries));
        tag.put("AccelSlot", accelSlot.serializeNBT(registries));
        tag.putInt("TickCounter", tickCounter);
        tag.putBoolean("AeEnabled", aeEnabled);
        tag.putInt("SortMode", sortMode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("Owner"))
        {
            owner = tag.getUUID("Owner");
        }
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
        if (tag.contains("GridInventory"))
        {
            itemHandler.deserializeNBT(registries, tag.getCompound("GridInventory"));
        }
        else if (tag.contains("InputSlot"))
        {
            // 旧存档（v5.17.x 及更早）：9 个刷怪蛋槽存在 InputSlot 里，索引 0-8 与新布局一致
            itemHandler.deserializeNBT(registries, tag.getCompound("InputSlot"));
        }
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(registries, tag.getCompound("AccelSlot"));
        }
        if (tag.contains("Inventory"))
        {
            innerStore.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        tickCounter = tag.getInt("TickCounter");
        // 排序模式（旧存档无此键 → 名称排序）
        int storedSort = tag.contains("SortMode") ? tag.getInt("SortMode") : 0;
        this.sortMode = Math.floorMod(storedSort, GodGridSorting.MODE_COUNT_TOTAL);
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_spawn_egg");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodSpawnEggMenu(containerId, inventory, this);
    }
}
