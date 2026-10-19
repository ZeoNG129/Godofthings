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
import com.godofthings.godgrid.LongSlotHandler;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.menu.GodResourceMenu;
import com.godofthings.menu.GodResourceConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
 * 神之资源系列方块实体（v5.13.0：原单一「神之资源」拆分为矿物 / 作物 / 方块三台）。
 *
 * <p>三台共用本类，仅 {@link GodResourceVariant} 不同（构造时传入）：
 * 各自的输入过滤与产出计算见 {@link GodResourceVariant#accepts} / {@link GodResourceVariant#produce}。</p>
 *
 * <p>其余行为与原神之资源一致：无需能源，输入槽（v5.18.0 起 17 格并行生产）且<b>不消耗模板</b>；
 * 快配方每 tick、慢配方每 20 tick；神之加速并行倍率；产物先进 17 格输出区、满了落进内置无限储存；
 * 向下自动输出；六个面各自可配置 NONE / INPUT / OUTPUT / BOTH（见 {@link FaceMode}）；AE2 并网（产物自动输出进网络）；打掉不掉落。</p>
 *
 * <p>v5.18.0：界面/容器迁移到 {@link com.godofthings.godgrid} 共用网格框架
 * （{@link GodGridLayout}：17 输入 + 17 输出 + 1 加速槽），本类实现 {@link GodGridHost}。</p>
 */
public class GodResourceBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity, MachineOwner, GodGridHost
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

    /** 输入槽数量（v5.18.0：9 → 17，与共用网格界面一致） */
    public static final int INPUT_SLOT_COUNT = GodGridLayout.INPUT_COUNT;

    /** 输出槽数量（17 格缓冲，产物先进这里，满了再落进内置无限储存） */
    public static final int OUTPUT_SLOT_COUNT = GodGridLayout.OUTPUT_COUNT;

    public static final int TOTAL_SLOTS = INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT;

    /** 输出区起点：输入在前、输出在后 */
    public static final int OUTPUT_SLOT_START = INPUT_SLOT_COUNT;

    /**
     * 本台机器的变体（矿物 / 作物 / 方块）——决定输入过滤与产出表。
     * <p><b>非 final</b>：新放置时由 {@code newBlockEntity} 按方块实例传入；
     * 旧存档读回时 BlockEntityType 工厂只能给 (pos, state)，故在工厂里按 state 判定（见 Godofthings 注册处）。</p>
     */
    private GodResourceVariant variant;

    /**
     * 网格槽位：前 {@link #INPUT_SLOT_COUNT} 格是生产模板（不消耗），其后
     * {@link #OUTPUT_SLOT_COUNT} 格是产物输出区（输入 i ↔ 输出 i 配对）。
     */
    private final LongSlotHandler gridHandler = new LongSlotHandler(TOTAL_SLOTS)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            // 输入格按变体过滤；输出格对机器内部写入放行（玩家放置由 Slot#mayPlace 拦，能力侧只暴露内置储存）
            return slot >= OUTPUT_SLOT_START || variant.accepts(stack);
        }
    };

    /** 内置无限储存：输出区塞不下的产物落这里（老行为，保证大倍率下也不丢产物） */
    private final InfiniteItemHandler itemHandler = new InfiniteItemHandler();

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

    // 每个面一个模式，索引 = Direction.get3DDataValue()，取值见 FaceMode.getId()
    // 默认 OUTPUT：与「任意面都能取走产物」的旧行为一致（老存档没有 FaceModes 键时也落到这个默认）
    private final int[] faceModes = new int[6];

    private final IItemHandler[] sideHandlers = new IItemHandler[6];

    /** 输出槽整理模式（0=名称 1=模组 2=数量），随方块实体持久化 */
    private int sortMode = GodGridSorting.MODE_NAME;

    private int tickCounter = 0;

    /** 是否接入 AE（并网后产物自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网）。 */
    private final AeGridNode aeNode = new AeGridNode(this);

    public GodResourceBlockEntity(GodResourceVariant variant, BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_RESOURCE_BE.get(), pos, state);
        this.variant = variant;
        itemHandler.setOnChange(this::setChanged);
        for (int i = 0; i < 6; i++)
        {
            // 默认 OUTPUT：任意面都能被管道取走产物（旧的 pipeView 行为），插入需要玩家自己开「输入」
            faceModes[i] = FaceMode.OUTPUT.getId();
            sideHandlers[i] = new SideHandler(i);
        }
    }

    /** 供 BlockEntityType 工厂用：先占位 DUPLICATE，真实变体由工厂按 state 判定后立即 setVariant（见 Godofthings 注册处） */
    public GodResourceBlockEntity(BlockPos pos, BlockState state)
    {
        this(GodResourceVariant.DUPLICATE, pos, state);
    }

    /** 工厂按 state 判定变体后回填。 */
    public void setVariant(GodResourceVariant variant)
    {
        this.variant = variant;
    }

    /** 本台机器的变体（矿物 / 作物 / 复制）。 */
    public GodResourceVariant getVariant()
    {
        return variant;
    }

    /** 网格槽位处理器（前 17 输入、后 17 输出，自管 long 计数），界面/容器统一从这里取。 */
    public LongSlotHandler getGridHandler()
    {
        return gridHandler;
    }

    public InfiniteItemHandler getItemHandler()
    {
        return itemHandler;
    }

    /** 神之加速槽（只接受神之加速，最多 64 个） */
    public ItemStackHandler getAccelSlot()
    {
        return accelSlot;
    }

    /** 并行倍率：每个神之加速 16 倍，最多一组（64 个）= 1024 倍。（倍率权威值见 {@link GodAcceleratorItem#PARALLEL_PER_ITEM}） */
    public int getParallelMultiplier()
    {
        return GodAcceleratorItem.multiplierFor(accelSlot.getStackInSlot(0).getCount());
    }

    /** 内置无限储存里的物品类型数 */
    public int getStorageCount()
    {
        return itemHandler.getStacks().size();
    }
    /**
     * 旧的「扁平」对外物品视图：前 {@link #OUTPUT_SLOT_COUNT} 格是网格输出格，其后是内置无限储存。
     * <p>能力注册已改为按面返回 {@code SideHandler}（见 {@link #getSideCapability}），这里保留给需要
     * 「输出优先、与面模式无关」顺序的代码与调试用；对外插入一律走输入槽，本视图只出不进。</p>
     */
    private final IItemHandler pipeView = new IItemHandler()
    {
        @Override
        public int getSlots()
        {
            return OUTPUT_SLOT_COUNT + itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return slot < OUTPUT_SLOT_COUNT
                    ? gridHandler.getStackInSlot(OUTPUT_SLOT_START + slot)
                    : itemHandler.getStackInSlot(slot - OUTPUT_SLOT_COUNT);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (slot < OUTPUT_SLOT_COUNT)
            {
                return stack; // 输出格只出不进
            }
            return itemHandler.insertItem(slot - OUTPUT_SLOT_COUNT, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return slot < OUTPUT_SLOT_COUNT
                    ? gridHandler.extractItem(OUTPUT_SLOT_START + slot, amount, simulate)
                    : itemHandler.extractItem(slot - OUTPUT_SLOT_COUNT, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return slot < OUTPUT_SLOT_COUNT
                    ? gridHandler.getSlotLimit(OUTPUT_SLOT_START + slot)
                    : itemHandler.getSlotLimit(slot - OUTPUT_SLOT_COUNT);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return slot >= OUTPUT_SLOT_COUNT;
        }
    };

    /** 方块对外的物品视图（输出格在前、内置无限储存在后） */
    public IItemHandler getPipeView()
    {
        return pipeView;
    }

    public boolean isAeEnabled()
    {
        return aeEnabled;
    }

    public void toggleAeEnabled()
    {
        this.aeEnabled = !this.aeEnabled;
        setChanged();
    }

    // ---- GodGridHost：共用网格界面所需的机器侧契约 ----

    @Override
    public IItemHandler gridItems()
    {
        return gridHandler;
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
        // 自管 long 槽专用的整理：按容量拆分合并，不会把超过 99 的存量截断丢量
        GodGridSorting.sort(gridHandler, OUTPUT_SLOT_START, OUTPUT_SLOT_COUNT, sortMode);
        setChanged();
        return sortMode;
    }

    /** 该机器槽的真实存量（GUI 右下角大数字用；客户端读到的是同步过来的近似值） */
    @Override
    public long gridAmount(int machineSlot)
    {
        return gridHandler.amount(machineSlot);
    }

    /** 本机网格槽自管 long 计数：开启数量同步与界面大数字 */
    @Override
    public boolean gridHasLongAmounts()
    {
        return true;
    }

    /**
     * 打开本机的六面 I/O 配置界面（界面左侧「面配置」页签）。
     * 标题按变体取方块名（矿物 / 作物 / 复制）。
     */
    @Override
    public void gridOpenFaceConfig(ServerPlayer player)
    {
        player.openMenu(new MenuProvider()
        {
            @Override
            public Component getDisplayName()
            {
                return Component.translatable("container.godofthings.god_resource_config",
                        GodResourceBlockEntity.this.getDisplayName());
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
            {
                return new GodResourceConfigMenu(containerId, inventory, GodResourceBlockEntity.this);
            }
        }, buf -> buf.writeBlockPos(getBlockPos()));
    }

    // ---- 面模式（照神之熔炉）----

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

    /** NeoForge 能力查询入口：按面模式返回受限 handler；side == null 或 NONE 面返回 null。 */
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


    @Override
    public int gridOperationsPerColumn()
    {
        return GodGridLayout.operationsPerColumn(accelSlot.getStackInSlot(0).getCount());
    }

    // ---- AE 网格节点（线缆直连并网，产物自动输出进 AE） ----

    @Override
    public IManagedGridNode getMainNode() { return aeNode.getMainNode(); }

    @Override
    public void saveChanges() { setChanged(); }

    /**
     * 把产物推入 AE 网络：先推网格输出格，再推内置无限储存。
     * 每 tick 全量搬运（v5.18.3）：每个输出槽把整个 amount 一次性 insert 进 ME 网络
     * （AE 的 storage.insert 接受大数量），网络按实际收下量结算，下 tick 再试余量。
     */
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
            long available = gridHandler.amount(slot);
            if (available <= 0L)
            {
                continue;
            }
            ItemStack key = gridHandler.key(slot);
            if (key.isEmpty())
            {
                continue;
            }
            AEItemKey aeKey = AEItemKey.of(key);
            if (aeKey == null)
            {
                continue;
            }
            long inserted = inv.insert(aeKey, available, Actionable.MODULATE, source);
            if (inserted > 0L)
            {
                gridHandler.extractLong(slot, inserted, false);
            }
        }
        for (int slot = 0; slot < getItemHandler().getSlots(); slot++)
        {
            ItemStack stack = getItemHandler().getStackInSlot(slot);
            if (stack.isEmpty())
            {
                continue;
            }
            long inserted = inv.insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, source);
            if (inserted > 0)
            {
                getItemHandler().extractItem(slot, (int) inserted, false);
            }
        }
    }

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

    public static void tick(Level level, BlockPos pos, BlockState state, GodResourceBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.tickServer();
    }

    private void tickServer()
    {
        // 快配方：每 tick 处理所有非空输入槽（变体各自的快配方资格见 GodResourceVariant.isFastRecipe）
        for (int i = 0; i < INPUT_SLOT_COUNT; i++)
        {
            ItemStack input = gridHandler.getStackInSlot(i);
            if (!input.isEmpty() && variant.isFastRecipe(input))
            {
                process(i); // 复制配方：每 tick 产 64 个
            }
        }
        // 慢配方：每 workInterval tick 处理一轮所有非空输入槽（实时读配置，改 toml 重进世界即生效）
        tickCounter++;
        if (tickCounter >= MachinesConfig.RESOURCE_WORK_INTERVAL.get())
        {
            tickCounter = 0;
            for (int i = 0; i < INPUT_SLOT_COUNT; i++)
            {
                ItemStack input = gridHandler.getStackInSlot(i);
                if (!input.isEmpty() && !variant.isFastRecipe(input))
                {
                    process(i);
                }
            }
        }
        pushDown();
        pushDown();
        // AE 产物输出：每 tick 全量搬运（不再每 20 tick 节流，v5.18.3）
        pushOutputToAe();
    }

    private void process(int slot)
    {
        ItemStack input = gridHandler.getStackInSlot(slot);
        if (input.isEmpty())
        {
            return;
        }
        List<ItemStack> outputs = variant.produce(input.getItem(), (net.minecraft.server.level.ServerLevel) level, worldPosition);
        if (outputs.isEmpty())
        {
            return; // 无效输入
        }
        int mult = getParallelMultiplier();
        // 神之共鸣·掉落（v5.15.4）：战利品爆炸倍率（主人在线 + 穿齐全套 + 共鸣开关开）
        double bomb = level instanceof net.minecraft.server.level.ServerLevel serverLevel
                ? com.godofthings.handler.ArmorSkillHandler.lootBombBoost(serverLevel, owner) : 1.0;
        // 不消耗原材料：输入只是生产模板，按时间持续产出（神之加速提升并行数量）
        for (ItemStack out : outputs)
        {
            if (!out.isEmpty())
            {
                ItemStack toInsert = out.copyWithCount((int) Math.min(Integer.MAX_VALUE / 2L,
                        Math.round(out.getCount() * mult * bomb)));
                // 产物先进输出格（输入 i ↔ 输出 i 配对），输出区塞不下再落进内置无限储存
                ItemStack leftover = insertIntoOutputs(slot, toInsert);
                if (!leftover.isEmpty())
                {
                    leftover = itemHandler.insertItem(-1, leftover, false);
                }
                if (!leftover.isEmpty())
                {
                    InfiniteItemHandler.dropRemainder(level, worldPosition, leftover);
                }
            }
        }
    }

    /**
     * 产物入输出格：先试与输入配对的那一格（输入 i ↔ 输出 i），再试其它输出格。
     * <p>按 long 累加（{@link LongSlotHandler#insertLong}），单格容量 int 上限，不会再被 99 截断。</p>
     * 返回塞不下的部分。
     */
    private ItemStack insertIntoOutputs(int inputSlot, ItemStack stack)
    {
        int paired = OUTPUT_SLOT_START + Math.floorMod(inputSlot, OUTPUT_SLOT_COUNT);
        ItemStack rest = insertIntoOutput(paired, stack);
        for (int j = 0; j < OUTPUT_SLOT_COUNT && !rest.isEmpty(); j++)
        {
            int idx = OUTPUT_SLOT_START + j;
            if (idx != paired)
            {
                rest = insertIntoOutput(idx, rest);
            }
        }
        return rest;
    }

    /** 把整堆产物按 long 累加进某一输出格，返回放不下的余量。 */
    private ItemStack insertIntoOutput(int outputSlot, ItemStack stack)
    {
        long inserted = gridHandler.insertLong(outputSlot, stack, stack.getCount(), false);
        if (inserted <= 0L)
        {
            return stack;
        }
        if (inserted >= stack.getCount())
        {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount((int) (stack.getCount() - inserted));
    }

    /** 向下自动输出到下方容器：先推网格输出格，再推内置无限储存 */
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
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, below, Direction.UP);
        if (handler == null)
        {
            return;
        }
        for (int j = 0; j < OUTPUT_SLOT_COUNT; j++)
        {
            pushSlotTo(handler, OUTPUT_SLOT_START + j);
        }
        while (true)
        {
            ItemStack stack = itemHandler.getStackInSlot(0);
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
                    itemHandler.extractItem(0, moved, false);
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

    /**
     * 把网格某一格的产物推进下方容器：每 tick 把该格整个 amount 一次搬完（v5.18.3 全量搬运）——
     * 整格取出后一次性尝试插入，搬不走的余量立刻写回原格，绝不丢物、绝不复制。
     */
    private void pushSlotTo(IItemHandler target, int slot)
    {
        ItemStack key = gridHandler.key(slot);
        long available = gridHandler.amount(slot);
        if (key.isEmpty() || available <= 0L)
        {
            return;
        }
        // 整格取出：搬不走的余量最后写回，绝不丢物、绝不复制
        gridHandler.extractLong(slot, available, false);
        long remaining = available;

        if (target instanceof LongSlotHandler ls)
        {
            // 本模组大容量槽：逐格全量 insertLong（对方按容量/同类合并截断）
            for (int s = 0; s < ls.getSlots() && remaining > 0L; s++)
            {
                remaining -= ls.insertLong(s, key, remaining, false);
            }
        }
        else if (target.getSlots() > 0 && target.getSlotLimit(0) > LongSlotHandler.VIEW_CAP)
        {
            // 其它大容量容器（单格上限 > 99）：整堆一次性插入
            ItemStack toPush = key.copyWithCount((int) remaining);
            for (int s = 0; s < target.getSlots() && !toPush.isEmpty(); s++)
            {
                ItemStack leftover = target.insertItem(s, toPush, false);
                remaining -= toPush.getCount() - leftover.getCount();
                toPush = leftover;
            }
        }
        else
        {
            // 普通容器：≤99 分片循环，直到搬完或该轮零进展（性能护栏，防无限热循环）
            while (remaining > 0L)
            {
                ItemStack toPush = key.copyWithCount((int) Math.min(remaining, LongSlotHandler.VIEW_CAP));
                boolean roundMoved = false;
                for (int s = 0; s < target.getSlots() && !toPush.isEmpty(); s++)
                {
                    ItemStack leftover = target.insertItem(s, toPush, false);
                    int moved = toPush.getCount() - leftover.getCount();
                    if (moved > 0)
                    {
                        remaining -= moved;
                        roundMoved = true;
                    }
                    toPush = leftover;
                }
                if (!roundMoved)
                {
                    break; // 这一轮零进展：目标已满，停
                }
            }
        }

        // 搬不走的余量立刻写回原格（insertLong 加回去）
        if (remaining > 0L)
        {
            gridHandler.insertLong(slot, key, remaining, false);
        }
    }

    // ---- capability：每个面按模式暴露受限的 IItemHandler（照神之熔炉）----
    // 1.21.1：BlockEntity 不可覆写 getCapability，能力统一在 RegisterCapabilitiesEvent（MOD 总线）注册。
    // faceModes 运行时可改，SideHandler 每次调用动态读取当前模式，故无需失效缓存。

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_RESOURCE_BE.get(),
                    (be, side) -> be.getSideCapability(side));
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                    Godofthings.GOD_RESOURCE_BE.get(),
                    (be, side) -> be);
        }
    }

    /**
     * 某个面的包装 handler：
     * <ul>
     *   <li>槽 0 .. {@link #INPUT_SLOT_COUNT}-1 = 网格输入槽（生产模板，<b>只能插入</b>，按变体 {@code accepts} 过滤）</li>
     *   <li>槽 {@link #OUTPUT_SLOT_START} .. {@link #TOTAL_SLOTS}-1 = 网格输出槽（<b>只能抽取</b>）</li>
     *   <li>槽 {@link #TOTAL_SLOTS} 起 = 内置无限储存兜底（<b>只能抽取</b>）</li>
     * </ul>
     * 输入槽不许被抽走（模板是机器配置），输出槽与内置储存不许被外部插入。
     */
    private class SideHandler implements IItemHandler
    {
        private final int dirIndex;

        SideHandler(int dirIndex)
        {
            this.dirIndex = dirIndex;
        }

        private boolean canInsert()
        {
            FaceMode mode = FaceMode.fromId(faceModes[dirIndex]);
            return mode == FaceMode.INPUT || mode == FaceMode.BOTH;
        }

        private boolean canExtract()
        {
            FaceMode mode = FaceMode.fromId(faceModes[dirIndex]);
            return mode == FaceMode.OUTPUT || mode == FaceMode.BOTH;
        }

        @Override
        public int getSlots()
        {
            return TOTAL_SLOTS + itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            if (slot < 0)
            {
                return ItemStack.EMPTY;
            }
            return slot < TOTAL_SLOTS
                    ? gridHandler.getStackInSlot(slot)
                    : itemHandler.getStackInSlot(slot - TOTAL_SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            // 只允许插进输入槽；输出槽与内置储存对外的插入一律拒绝
            if (slot < 0 || slot >= INPUT_SLOT_COUNT || !canInsert() || !variant.accepts(stack))
            {
                return stack;
            }
            // 按 long 累加（模板槽同样受益：仓库里堆多少都能一次抽进来）
            long inserted = gridHandler.insertLong(slot, stack, stack.getCount(), simulate);
            if (inserted <= 0L)
            {
                return stack;
            }
            return inserted >= stack.getCount() ? ItemStack.EMPTY
                    : stack.copyWithCount((int) (stack.getCount() - inserted));
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (slot < 0 || !canExtract())
            {
                return ItemStack.EMPTY;
            }
            if (slot >= OUTPUT_SLOT_START && slot < TOTAL_SLOTS)
            {
                return gridHandler.extractItem(slot, amount, simulate);
            }
            if (slot >= TOTAL_SLOTS)
            {
                return itemHandler.extractItem(slot - TOTAL_SLOTS, amount, simulate);
            }
            return ItemStack.EMPTY; // 输入格是生产模板，不许被抽走
        }

        @Override
        public int getSlotLimit(int slot)
        {
            if (slot < 0)
            {
                return 0;
            }
            return slot < TOTAL_SLOTS
                    ? gridHandler.getSlotLimit(slot)
                    : itemHandler.getSlotLimit(slot - TOTAL_SLOTS);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return slot >= 0 && slot < INPUT_SLOT_COUNT && canInsert() && variant.accepts(stack);
        }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        if (owner != null)
        {
            tag.putUUID("Owner", owner);
        }
        // v5.18.1 起网格槽（17 输入 + 17 输出）改成「自管 long」格式：单格可超 99。
        // 每条 {Slot:int, Item:count=1 的物品, Amount:long}，见 LongSlotHandler#saveList。
        tag.put("GridInventoryLong", gridHandler.saveList(provider));
        tag.put("AccelSlot", accelSlot.serializeNBT(provider));
        tag.put("Inventory", itemHandler.serializeNBT(provider));
        tag.putInt("TickCounter", tickCounter);
        tag.putBoolean("AeEnabled", aeEnabled);
        tag.putInt("SortMode", sortMode);
        tag.putIntArray("FaceModes", faceModes);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        if (tag.hasUUID("Owner"))
        {
            owner = tag.getUUID("Owner");
        }
        // 自管 long 网格库存：新键 GridInventoryLong 优先；老存档读旧格式再灌进来（amount = count）
        if (tag.contains("GridInventoryLong", Tag.TAG_LIST))
        {
            gridHandler.loadList(tag.getList("GridInventoryLong", Tag.TAG_COMPOUND), provider);
        }
        else if (tag.contains("GridInventory"))
        {
            migrateLegacyGrid(tag.getCompound("GridInventory"), provider);
        }
        else if (tag.contains("InputSlot"))
        {
            // 旧存档（9 格输入模板）照常读回：物品落在前 9 个输入格
            migrateLegacyGrid(tag.getCompound("InputSlot"), provider);
        }
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(provider, tag.getCompound("AccelSlot"));
        }
        if (tag.contains("Inventory"))
        {
            itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
        }
        tickCounter = tag.getInt("TickCounter");
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
        // 排序模式（旧存档无此键 → 名称排序）
        int storedSort = tag.contains("SortMode") ? tag.getInt("SortMode") : 0;
        this.sortMode = Math.floorMod(storedSort, GodGridSorting.MODE_COUNT_TOTAL);
        if (tag.contains("FaceModes"))
        {
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
    }

    /**
     * 老存档迁移：旧格式是 {@code ItemStackHandler} 的 {@code {Size, Items:[{Slot, id, count}]}}，
     * 逐格灌进新 long handler（amount = count）。槽数按新布局的 34 槽读，越界的旧槽位自然被忽略
     * （旧 9 格输入模板因此原样落在前 9 个输入格里）。
     */
    private void migrateLegacyGrid(CompoundTag inv, HolderLookup.Provider provider)
    {
        inv.putInt("Size", TOTAL_SLOTS);
        ItemStackHandler legacy = new ItemStackHandler(TOTAL_SLOTS);
        legacy.deserializeNBT(provider, inv);
        for (int i = 0; i < TOTAL_SLOTS; i++)
        {
            ItemStack stack = legacy.getStackInSlot(i);
            if (!stack.isEmpty())
            {
                gridHandler.setEntry(i, stack, stack.getCount());
            }
        }
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
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        // 三台共用 BE 类型：按变体取对应方块的注册名显示（check-lang 校验该字面量）
        return switch (variant)
        {
            case ORE -> Component.translatable("block.godofthings.god_ore_machine");
            case CROP -> Component.translatable("block.godofthings.god_crop_machine");
            case DUPLICATE -> Component.translatable("block.godofthings.god_duplicate_machine");
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodResourceMenu(containerId, inventory, this);
    }
}
