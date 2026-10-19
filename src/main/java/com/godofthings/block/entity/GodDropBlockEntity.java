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
import com.godofthings.menu.GodDropMenu;
import com.godofthings.menu.GodDropConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * 神之掉落生物掉落物生产机方块实体。
 * - 无需能源，{@link #INPUT_SLOT_COUNT} 个刷怪蛋输入槽并行生产；每 20 tick 处理一轮所有非空刷怪蛋
 * - 放入刷怪蛋 → 按原版生物战利品表产出该生物**被击杀时的全部掉落物**
 *   （v5.1.6 起与原版同步：鸡 = 羽毛 + 生鸡肉，不再是固定的一种物品）
 * - 每种产物每周期 64 个（神之加速按并行倍率乘）
 * - 不消耗刷怪蛋（生产模板，按时间持续产出）
 * - 向下自动输出，内置无限储存；打掉不掉落
 *
 * <p>v5.18.0：槽位迁移到统一网格框架（{@link GodGridLayout}，17 输入 + 17 输出 + 1 加速槽），
 * 输入（刷怪蛋模板）在前 {@link #INPUT_SLOT_COUNT} 格、产物输出在后
 * {@link #OUTPUT_SLOT_COUNT} 格。原本的「内部无限储存」保留为输出槽装不下时的兜底缓冲
 * （NBT 键 {@code Inventory} 与格式都不变），所以产物照旧永不外吐。</p>
 */
public class GodDropBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity, MachineOwner, GodGridHost
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

    /**
     * 工作间隔（tick），可经 godofthings-machines.toml 调整。
     * <p>v5.12.1 起每次调用实时读配置（此前是 {@code static final} 在类加载时快照，
     * 改配置必须重启才生效；实时读后重进世界即可生效）。</p>
     */
    public static int workInterval()
    {
        return MachinesConfig.DROP_WORK_INTERVAL.get();
    }

    // v5.18.0：统一 17 输入 + 17 输出网格（照 PBG 无限离心机工厂布局，见 GodGridLayout）
    /** 刷怪蛋输入槽数量（网格第一行） */
    public static final int INPUT_SLOT_COUNT = GodGridLayout.INPUT_COUNT;
    /** 产物输出槽数量（网格第二行） */
    public static final int OUTPUT_SLOT_COUNT = GodGridLayout.OUTPUT_COUNT;
    public static final int OUTPUT_SLOT_START = INPUT_SLOT_COUNT;
    public static final int TOTAL_SLOTS = INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT;

    /**
     * 每 tick 对外搬运的轮数上限：每轮最多搬 {@link LongSlotHandler#VIEW_CAP}（99，对外单堆上限），
     * 即每格每 tick 最多 3 × 99 = 297（与神之熔炉一致，内部按 long 批量搬）。
     */
    private static final int TRANSFER_ROUNDS = 3;

    /**
     * 机器槽位：0..{@link #INPUT_SLOT_COUNT}-1 放刷怪蛋模板，其后 {@link #OUTPUT_SLOT_COUNT} 格是产物。
     * <p>自管 long 计数（{@link LongSlotHandler}）：单格容量 1,000,000、对外视图仍 ≤99，
     * 不再受 {@code ItemStack} 的 99 堆叠上限约束。输出格对 {@link #isItemValid} 返回 true
     * 只是为了放行机器内部写入；对外插入走各面 {@code SideHandler}（只进刷怪蛋模板槽，
     * 见 {@link #getSideCapability}），界面侧由 Slot#mayPlace 拦截。</p>
     */
    private final LongSlotHandler itemHandler = new LongSlotHandler(TOTAL_SLOTS)
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
                // v5.7.0：不再只认原版 SpawnEggItem —— 任何能解析出实体类型的刷怪蛋（含模组自建的）都收
                return com.godofthings.block.entity.machine.SpawnEggHelper.isSpawnEgg(stack);
            }
            return true;
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

    // 每个面一个模式，索引 = Direction.get3DDataValue()，取值见 FaceMode.getId()
    // 默认 OUTPUT：与「任意面都能取走产物」的旧行为一致（老存档没有 FaceModes 键时也落到这个默认）
    private final int[] faceModes = new int[6];

    private final IItemHandler[] sideHandlers = new IItemHandler[6];

    public GodDropBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_DROP_BE.get(), pos, state);
        innerStore.setOnChange(this::setChanged);
        for (int i = 0; i < 6; i++)
        {
            // 默认 OUTPUT：任意面都能被管道取走产物（旧的 pipeView 行为），插入需要玩家自己开「输入」
            faceModes[i] = FaceMode.OUTPUT.getId();
            sideHandlers[i] = new SideHandler(i);
        }
    }

    /** 机器槽位（17 输入 + 17 输出，自管 long 计数） */
    public LongSlotHandler getItemHandler()
    {
        return itemHandler;
    }

    /** 内置无限储存（兜底缓冲，物品种类数用于界面统计） */
    public InfiniteItemHandler getInnerStore()
    {
        return innerStore;
    }

    /**
     * 旧的「扁平」对外物品视图：输出槽在前、内部储存兜底在后，<b>只出不进</b>。
     * <p>能力注册已改为按面返回 {@code SideHandler}（见 {@link #getSideCapability}），这里保留兼容用。</p>
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

    /** 把产物推入 AE 网络（节流由 tick 控制）。网格输出格按 long 搬，内部储存走旧路径。 */
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
            pushGridSlotToAe(inv, source, slot);
        }
        // 内部储存兜底：抽空一格后后面的堆会前移，倒序遍历避免跳项
        for (int slot = innerStore.getSlots() - 1; slot >= 0; slot--)
        {
            pushSlotToAe(inv, source, innerStore, slot);
        }
    }

    /** 网格输出格按 long 推入 AE：每格每 tick 最多 TRANSFER_ROUNDS 轮 × 99（与神之熔炉一致）。 */
    private void pushGridSlotToAe(MEStorage inv, IActionSource source, int slot)
    {
        ItemStack key = itemHandler.key(slot);
        if (key.isEmpty())
        {
            return;
        }
        AEItemKey aeKey = AEItemKey.of(key);
        if (aeKey == null)
        {
            return;
        }
        for (int round = 0; round < TRANSFER_ROUNDS; round++)
        {
            long available = itemHandler.amount(slot);
            if (available <= 0L)
            {
                break;
            }
            long chunk = Math.min(available, LongSlotHandler.VIEW_CAP);
            long inserted = inv.insert(aeKey, chunk, Actionable.MODULATE, source);
            if (inserted <= 0L)
            {
                break;
            }
            itemHandler.extractLong(slot, inserted, false);
            if (inserted < chunk)
            {
                break;
            }
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

    public static void tick(Level level, BlockPos pos, BlockState state, GodDropBlockEntity be)
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
        if (tickCounter >= workInterval())
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
        ItemStack input = itemHandler.key(slot);
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
        // 不消耗刷怪蛋：生产模板，按时间持续产出（神之加速提升并行数量）
        for (ItemStack out : outputs)
        {
            if (!out.isEmpty())
            {
                insertToOutputs(out.copyWithCount(out.getCount() * mult));
            }
        }
    }

    /**
     * 产物先按 long 累加进 17 个输出槽（同物品自动叠），装不下再落进内部无限储存，
     * 最后才掉在机器上方（后两步沿用旧版「永不丢产物」的兜底）。
     */
    private void insertToOutputs(ItemStack stack)
    {
        ItemStack leftover = stack;
        for (int j = 0; j < OUTPUT_SLOT_COUNT && !leftover.isEmpty(); j++)
        {
            leftover = insertIntoOutput(OUTPUT_SLOT_START + j, leftover);
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

    /** 把整堆产物按 long 累加进某个输出格，返回放不下的余量。 */
    private ItemStack insertIntoOutput(int slot, ItemStack stack)
    {
        long inserted = itemHandler.insertLong(slot, stack, stack.getCount(), false);
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

    /** 根据刷怪蛋，按本模组设定产出对应掉落物（每周期 64 个）。
     *  <p>掷表逻辑已抽到 {@link com.godofthings.block.entity.machine.DropLootRoller}（那边能被 GameTest 直接测）。</p> */
    private List<ItemStack> produce(ItemStack input)
    {
        if (!(level instanceof ServerLevel serverLevel))
        {
            return List.of();
        }
        List<ItemStack> rolled = com.godofthings.block.entity.machine.DropLootRoller.roll(serverLevel, worldPosition, input);
        // 神之共鸣·掉落（v5.15.4）：战利品爆炸倍率（主人在线 + 穿齐全套 + 共鸣开关开）
        double bomb = com.godofthings.handler.ArmorSkillHandler.lootBombBoost(serverLevel, owner);
        if (bomb <= 1.0)
        {
            return rolled;
        }
        List<ItemStack> boosted = new java.util.ArrayList<>(rolled.size());
        for (ItemStack stack : rolled)
        {
            boosted.add(stack.isEmpty() ? stack
                    : stack.copyWithCount((int) Math.min(Integer.MAX_VALUE / 2L,
                            Math.round(stack.getCount() * bomb))));
        }
        return boosted;
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
            pushGridSlotInto(handler, OUTPUT_SLOT_START + j);
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

    /** 网格输出格按 long 推进下方容器：每格每 tick 最多 TRANSFER_ROUNDS 轮 × 99（与神之熔炉一致）。 */
    private void pushGridSlotInto(IItemHandler target, int slot)
    {
        ItemStack key = itemHandler.key(slot);
        if (key.isEmpty())
        {
            return;
        }
        for (int round = 0; round < TRANSFER_ROUNDS; round++)
        {
            long available = itemHandler.amount(slot);
            if (available <= 0L)
            {
                break;
            }
            ItemStack toPush = key.copyWithCount((int) Math.min(available, LongSlotHandler.VIEW_CAP));
            boolean movedAny = false;
            for (int s = 0; s < target.getSlots() && !toPush.isEmpty(); s++)
            {
                ItemStack leftover = target.insertItem(s, toPush, false);
                int moved = toPush.getCount() - leftover.getCount();
                if (moved > 0)
                {
                    itemHandler.extractLong(slot, moved, false);
                    movedAny = true;
                }
                toPush = leftover;
            }
            if (!movedAny)
            {
                break;
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
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_DROP_BE.get(),
                    (be, side) -> be.getSideCapability(side));
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_DROP_BE.get(),
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
        // 自管 long 槽专用的整理：按容量拆分合并，不会把超过 99 的存量截断丢量
        GodGridSorting.sort(itemHandler, OUTPUT_SLOT_START, OUTPUT_SLOT_COUNT, sortMode);
        setChanged();
        return sortMode;
    }

    /** 该机器槽的真实存量（GUI 右下角大数字用；客户端读到的是同步过来的近似值） */
    @Override
    public long gridAmount(int machineSlot)
    {
        return itemHandler.amount(machineSlot);
    }

    /** 本机网格槽自管 long 计数：开启数量同步与界面大数字 */
    @Override
    public boolean gridHasLongAmounts()
    {
        return true;
    }

    /**
     * 打开本机的六面 I/O 配置界面（界面左侧「面配置」页签）。
     */
    @Override
    public void gridOpenFaceConfig(ServerPlayer player)
    {
        player.openMenu(new MenuProvider()
        {
            @Override
            public Component getDisplayName()
            {
                return Component.translatable("container.godofthings.god_drop_config",
                        GodDropBlockEntity.this.getDisplayName());
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
            {
                return new GodDropConfigMenu(containerId, inventory, GodDropBlockEntity.this);
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

    /**
     * 某个面的包装 handler：
     * <ul>
     *   <li>槽 0 .. {@link #INPUT_SLOT_COUNT}-1 = 刷怪蛋模板槽（<b>只能插入</b>，必须是刷怪蛋）</li>
     *   <li>槽 {@link #OUTPUT_SLOT_START} .. {@link #TOTAL_SLOTS}-1 = 产物输出槽（<b>只能抽取</b>）</li>
     *   <li>槽 {@link #TOTAL_SLOTS} 起 = 内部无限储存兜底（<b>只能抽取</b>）</li>
     * </ul>
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
            return TOTAL_SLOTS + innerStore.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            if (slot < 0)
            {
                return ItemStack.EMPTY;
            }
            return slot < TOTAL_SLOTS
                    ? itemHandler.getStackInSlot(slot)
                    : innerStore.getStackInSlot(slot - TOTAL_SLOTS);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            // 只允许插进刷怪蛋模板槽（走原槽位校验）；输出槽与内部储存对外的插入一律拒绝
            if (slot < 0 || slot >= INPUT_SLOT_COUNT || !canInsert()
                    || !itemHandler.isItemValid(slot, stack))
            {
                return stack;
            }
            // 按 long 累加（模板槽容量 1,000,000），对外返回值仍是 ≤99 的余量
            long inserted = itemHandler.insertLong(slot, stack, stack.getCount(), simulate);
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
                return itemHandler.extractItem(slot, amount, simulate);
            }
            if (slot >= TOTAL_SLOTS)
            {
                return innerStore.extractItem(slot - TOTAL_SLOTS, amount, simulate);
            }
            return ItemStack.EMPTY; // 模板槽不许被抽走
        }

        @Override
        public int getSlotLimit(int slot)
        {
            if (slot < 0)
            {
                return 0;
            }
            return slot < TOTAL_SLOTS
                    ? itemHandler.getSlotLimit(slot)
                    : innerStore.getSlotLimit(slot - TOTAL_SLOTS);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return slot >= 0 && slot < INPUT_SLOT_COUNT && canInsert()
                    && itemHandler.isItemValid(slot, stack);
        }
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
        // v5.18.1：网格库存改成「自管 long」格式（单格可超 99），不再写旧的 ItemStackHandler 格式
        tag.put("GridInventoryLong", itemHandler.saveList(registries));
        tag.put("Inventory", innerStore.serializeNBT(registries));
        tag.put("AccelSlot", accelSlot.serializeNBT(registries));
        tag.putInt("TickCounter", tickCounter);
        tag.putBoolean("AeEnabled", aeEnabled);
        tag.putInt("SortMode", sortMode);
        tag.putIntArray("FaceModes", faceModes);
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
        // 自管 long 网格库存：新键 GridInventoryLong 优先；老存档读旧格式再灌进来（amount = count）
        if (tag.contains("GridInventoryLong", Tag.TAG_LIST))
        {
            itemHandler.loadList(tag.getList("GridInventoryLong", Tag.TAG_COMPOUND), registries);
        }
        else if (tag.contains("GridInventory"))
        {
            migrateLegacyGrid(tag.getCompound("GridInventory"), registries);
        }
        else if (tag.contains("InputSlot"))
        {
            // 旧存档（v5.17.x 及更早）：9 个刷怪蛋槽存在 InputSlot 里，索引 0-8 与新布局一致
            migrateLegacyGrid(tag.getCompound("InputSlot"), registries);
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
        if (tag.contains("FaceModes"))
        {
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
    }

    /**
     * 老存档迁移：旧格式是 {@code ItemStackHandler} 的 {@code {Size, Items:[{Slot, id, count}]}}，
     * 逐格灌进新 long handler（amount = count）。槽数按新布局的 34 槽读，越界的旧槽位自然被忽略
     * （旧 9 个刷怪蛋模板因此原样落在前 9 个输入格里）。
     */
    private void migrateLegacyGrid(CompoundTag inv, HolderLookup.Provider registries)
    {
        inv.putInt("Size", TOTAL_SLOTS);
        ItemStackHandler legacy = new ItemStackHandler(TOTAL_SLOTS);
        legacy.deserializeNBT(registries, inv);
        for (int i = 0; i < TOTAL_SLOTS; i++)
        {
            ItemStack stack = legacy.getStackInSlot(i);
            if (!stack.isEmpty())
            {
                itemHandler.setEntry(i, stack, stack.getCount());
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
        return Component.translatable("block.godofthings.god_drop");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodDropMenu(containerId, inventory, this);
    }
}
