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
import com.godofthings.godgrid.GodGridHost;
import com.godofthings.godgrid.GodGridLayout;
import com.godofthings.godgrid.GodGridSorting;
import com.godofthings.godgrid.LongSlotHandler;
import com.godofthings.block.entity.machine.LogStripper;
import com.godofthings.config.MachinesConfig;
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.item.ConfigurableMachine;
import com.godofthings.menu.GodPeelerMenu;
import com.godofthings.menu.GodPeelerConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

/**
 * 神之去皮方块实体（v5.15.3：加入与神之熔炉相同的六面输入/输出配置）。
 *
 * <p>17 个输入槽（0-16）+ 17 个输出槽（17-33，一一对应：输入 i → 输出 {@link #OUTPUT_SLOT_START} + i）+
 * 1 个神之加速槽（提升并行数量与槽位容量）。无需能源。槽位布局见 {@link GodGridLayout}。</p>
 *
 * <p>每 {@link MachinesConfig#PEELER_WORK_INTERVAL} tick 处理一轮：<b>每个</b>非空输入槽把一批原木去皮塞进对应输出槽
 * （同物品可叠、容量够才动）。批次数 = 内置 64 × 并行倍率；<b>加速槽放满 64 个神之加速时该批次数为 int 上限
 * 2,147,483,647</b>（实际处理量受输入存量约束），见 {@link GodGridLayout#roundBatch}。</p>
 *
 * <p><b>面配置（与神之熔炉同一套）</b>：六个面各自可配置
 * {@link FaceMode#NONE}（不启用）/ {@link FaceMode#INPUT}（自动从相邻容器抽入原木）/
 * {@link FaceMode#OUTPUT}（自动把成品推给相邻容器）/ {@link FaceMode#BOTH}（同一面既抽入又推出），
 * 默认全部 NONE——漏斗 / 管道要在界面右上角齿轮的面配置里开启对应面才会互动。
 * 面配置界面右上角齿轮按钮打开（菜单按钮 6），见 {@link GodPeelerMenu}。</p>
 *
 * <p>打掉时物品一并消失，不掉落（同神之资源系列做法）。</p>
 */
public class GodPeelerBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity, GodGridHost, ConfigurableMachine
{
    public static final int INPUT_SLOT_COUNT = GodGridLayout.INPUT_COUNT;
    public static final int OUTPUT_SLOT_COUNT = GodGridLayout.OUTPUT_COUNT;
    public static final int OUTPUT_SLOT_START = INPUT_SLOT_COUNT;
    public static final int TOTAL_SLOTS = INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT;

    /**
     * 每 tick 输入抽取（pullFrom）的轮数上限：每轮最多搬 {@link LongSlotHandler#VIEW_CAP}（99，对外单堆上限），
     * 即每个源槽每 tick 最多 3 × 99 = 297。输出侧（pushTo / pushOutputToAe）不再设轮数上限：
     * 每 tick 把输出槽整个 amount 一次搬完（见 {@link #pushLongSlotTo}）。
     */
    private static final int TRANSFER_ROUNDS = 3;

    /** 旧布局（v5.15.x）：9 输入 + 9 输出，仅用于读档迁移 */
    private static final int LEGACY_INPUT_SLOT_COUNT = 9;

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

    /**
     * 网格槽位（17 输入 + 17 输出），自管 long 计数（{@link LongSlotHandler}）：
     * 单格容量默认 int 上限 2,147,483,647，对外视图仍 ≤99，不再受 {@code ItemStack} 的 99 堆叠上限约束。
     */
    private final LongSlotHandler itemHandler = new LongSlotHandler(TOTAL_SLOTS)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }

        // 规则：输入槽只收可去皮原木；输出槽对机器内部写入放行（对外插入由 SideHandler / Slot#mayPlace 拦）
        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            if (slot < INPUT_SLOT_COUNT)
            {
                return LogStripper.isStrippable(stack);
            }
            return true;
        }
    };

    // 每个面一个模式，索引 = Direction.get3DDataValue()，取值见 FaceMode.getId()
    private final int[] faceModes = new int[6];

    private final IItemHandler[] sideHandlers = new IItemHandler[6];

    /** 是否接入 AE（v5.15.9 加入：并网后去皮产物自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网，照抄神之熔炉）。 */
    private final AeGridNode aeNode = new AeGridNode(this);

    private int tickCounter = 0;

    public GodPeelerBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_PEELER_BE.get(), pos, state);
        for (int i = 0; i < 6; i++)
        {
            final int idx = i;
            sideHandlers[idx] = new SideHandler(idx);
        }
    }

    public LongSlotHandler getItemHandler()
    {
        return itemHandler;
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

    // ---- GodGridHost：共用网格界面所需的机器侧契约 ----

    /** 输出槽整理模式（0=名称 1=模组 2=数量），随方块实体持久化 */
    private int sortMode = GodGridSorting.MODE_NAME;

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

    @Override
    public void gridOpenFaceConfig(ServerPlayer player)
    {
        player.openMenu(new MenuProvider()
        {
            @Override
            public Component getDisplayName()
            {
                return Component.translatable("container.godofthings.god_peeler");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
            {
                return new GodPeelerConfigMenu(containerId, inventory, GodPeelerBlockEntity.this);
            }
        }, buf -> buf.writeBlockPos(getBlockPos()));
    }

    /**
     * 单轮处理量（GUI 加速槽提示与去皮批次共用同一口径）：加速槽满 64 个神之加速 → int 上限
     * 2,147,483,647，否则 = 内置 64 × 并行倍率。见 {@link GodGridLayout#roundBatch}。
     */
    @Override
    public int gridOperationsPerColumn()
    {
        return (int) GodGridLayout.roundBatch(
                accelSlot.getStackInSlot(0).getCount(), GodGridLayout.BUILTIN_PARALLEL, getParallelMultiplier());
    }

    // ---- 面模式（与神之熔炉同一套）----

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

    /** NeoForge 能力查询入口：按面模式返回受限 handler；side == null 或 NONE 面返回 null（与神之熔炉逻辑一致）。 */
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
        public static void registerCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_PEELER_BE.get(),
                    (be, side) -> be.getSideCapability(side));
            // v5.15.9：加入 AE 并网（照抄神之熔炉）——线缆直连、占一个频道、产物自动输出进网络。
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_PEELER_BE.get(),
                    (be, side) -> be);
        }
    }

    /**
     * 某个面的包装 handler：INPUT 面只能插入输入槽，OUTPUT 面只能从输出槽提取（照抄神之熔炉）。
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
            if (slot < 0 || slot >= INPUT_SLOT_COUNT
                    || (mode() != FaceMode.INPUT && mode() != FaceMode.BOTH))
            {
                return stack; // 只能插入输入槽，且必须是 INPUT / BOTH 面
            }
            // 按 long 累加（内部容量 int 上限），对外返回值仍是 ≤99 的余量
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
                    && LogStripper.isStrippable(stack);
        }
    }

    // ---- 每 tick 逻辑：先自动抽推，再去皮 ----

    // ---- AE 网格节点（v5.15.9 加入：线缆直连并网，产物自动输出进 AE，占一个频道） ----

    @Override
    public IManagedGridNode getMainNode()
    {
        return aeNode.getMainNode();
    }

    @Override
    public void saveChanges()
    {
        setChanged();
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

    /** 是否接入 AE（并网后去皮产物自动输出进 AE 网络）。 */
    public boolean isAeEnabled()
    {
        return aeEnabled;
    }

    public void toggleAeEnabled()
    {
        this.aeEnabled = !this.aeEnabled;
        setChanged();
    }

    // ---- ConfigurableMachine：给「神之配置」物品读写（六面模式 / AE 开关 / 加速数量）----

    @Override
    public int[] getFaceModesSnapshot()
    {
        return faceModes.clone();
    }

    @Override
    public void applyFaceModes(int[] modes)
    {
        if (modes == null)
        {
            return;
        }
        for (int i = 0; i < faceModes.length && i < modes.length; i++)
        {
            faceModes[i] = ((modes[i] % 4) + 4) % 4;
        }
        setChanged();
    }

    @Override
    public void setAeEnabled(boolean enabled)
    {
        if (this.aeEnabled != enabled)
        {
            this.aeEnabled = enabled;
            setChanged();
        }
    }

    @Override
    public int acceleratorCount()
    {
        return accelSlot.getStackInSlot(0).getCount();
    }

    @Override
    public int addAccelerators(int want)
    {
        if (want <= 0)
        {
            return 0;
        }
        ItemStack current = accelSlot.getStackInSlot(0);
        int room = accelSlot.getSlotLimit(0) - (current.isEmpty() ? 0 : current.getCount());
        int add = Math.min(want, room);
        if (add <= 0)
        {
            return 0;
        }
        ItemStack merged = current.isEmpty()
                ? new ItemStack(Godofthings.GOD_ACCELERATOR.get(), add)
                : current.copyWithCount(current.getCount() + add);
        accelSlot.setStackInSlot(0, merged);
        setChanged();
        return add;
    }
    /**
     * 把输出槽产物推入 AE 网络（只推输出槽 17-33；输入槽是待去皮队列不推）。
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
            long available = itemHandler.amount(slot);
            if (available <= 0L)
            {
                continue;
            }
            ItemStack key = itemHandler.key(slot);
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
                itemHandler.extractLong(slot, inserted, false);
            }
        }
    }
    public static void tick(Level level, BlockPos pos, BlockState state, GodPeelerBlockEntity be)
    {
        if (!level.isClientSide)
        {
            be.autoTransfer();
            be.tickServer();
            // AE 产物输出：每 tick 全量搬运（不再每 20 tick 节流，v5.18.3）
            be.pushOutputToAe();
        }
    }

    private void tickServer()
    {
        boolean hasWork = false;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++)
        {
            if (!itemHandler.isEmpty(i))
            {
                hasWork = true;
                break;
            }
        }
        if (!hasWork)
        {
            tickCounter = 0;
            return;
        }
        // 每 workInterval tick 处理一轮：每个输入槽并行去皮（实时读配置，改 toml 重进世界即生效）
        tickCounter++;
        if (tickCounter >= MachinesConfig.PEELER_WORK_INTERVAL.get())
        {
            tickCounter = 0;
            // 本批数量（long）：加速槽满 64 个神之加速 → int 上限 2,147,483,647，否则 = 内置 64 × 并行倍率
            long batch = GodGridLayout.roundBatch(
                    accelSlot.getStackInSlot(0).getCount(), GodGridLayout.BUILTIN_PARALLEL, getParallelMultiplier());
            for (int i = 0; i < INPUT_SLOT_COUNT; i++)
            {
                convertOne(itemHandler, i, OUTPUT_SLOT_START + i, batch);
            }
        }
    }

    /** 每 tick 按各面模式自动抽入原木 / 推出成品（照抄神之熔炉 autoTransfer，过滤条件换成可去皮原木）。 */
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
                // 同一个面既自动抽入原木，又自动推出成品
                pullFrom(neighborCap);
                pushTo(neighborCap);
            }
        }
    }

    /**
     * 从邻居抽取可去皮原木到任意有空位的输入槽（同物品优先，否则找空槽；照抄神之熔炉 pullFrom）。
     * <p>按 long 搬：每 tick 每个源槽最多 {@link #TRANSFER_ROUNDS} 轮，每轮最多 99（对外单堆上限），
     * 输入槽容量 int 上限时能快速把原木囤起来。</p>
     */
    private void pullFrom(IItemHandler neighbor)
    {
        for (int s = 0; s < neighbor.getSlots(); s++)
        {
            boolean moved = false;
            for (int round = 0; round < TRANSFER_ROUNDS; round++)
            {
                ItemStack src = neighbor.getStackInSlot(s);
                if (src.isEmpty() || !LogStripper.isStrippable(src))
                {
                    break;
                }

                int targetSlot = findInputSlotFor(src);
                if (targetSlot < 0)
                {
                    break;
                }
                long room = itemHandler.room(targetSlot);
                int chunk = (int) Math.min(Math.min(room, LongSlotHandler.VIEW_CAP), src.getCount());
                if (chunk <= 0)
                {
                    break;
                }

                // 先模拟能插多少，再真正抽取：中途被拒也不会让物品凭空消失
                long canMove = itemHandler.insertLong(targetSlot, src, chunk, true);
                if (canMove <= 0L)
                {
                    break;
                }
                ItemStack extracted = neighbor.extractItem(s, (int) canMove, false);
                if (extracted.isEmpty())
                {
                    break;
                }

                long inserted = itemHandler.insertLong(targetSlot, extracted, extracted.getCount(), false);
                if (inserted < extracted.getCount())
                {
                    // 兜底：放不下的余量退回邻居，绝不丢
                    neighbor.insertItem(s,
                            extracted.copyWithCount((int) (extracted.getCount() - inserted)), false);
                }
                if (inserted <= 0L)
                {
                    break;
                }
                moved = true;
            }
            if (moved)
            {
                setChanged();
            }
        }
    }

    /** 找到可接受该原木的输入槽（同物品且还有空位优先，否则找空槽）；没有则 -1（照抄神之熔炉）。 */
    private int findInputSlotFor(ItemStack stack)
    {
        int emptySlot = -1;
        for (int i = 0; i < INPUT_SLOT_COUNT; i++)
        {
            if (itemHandler.isEmpty(i))
            {
                if (emptySlot < 0)
                {
                    emptySlot = i;
                }
            }
            else if (itemHandler.room(i) > 0L
                    && ItemStack.isSameItemSameComponents(itemHandler.key(i), stack))
            {
                return i;
            }
        }
        return emptySlot;
    }

    /**
     * 把输出槽的去皮原木推给邻居：每 tick 每个输出槽把整个 amount 一次搬完（v5.18.3 全量搬运）——
     * 整格取出后一次性尝试插入邻居，搬不走的余量立刻写回原格，绝不丢物、绝不复制。
     */
    private void pushTo(IItemHandler neighbor)
    {
        for (int out = OUTPUT_SLOT_START; out < TOTAL_SLOTS; out++)
        {
            pushLongSlotTo(neighbor, out);
        }
    }

    /**
     * 把输出格某一格的全部数量推进相邻容器（每 tick 全量搬运，v5.18.3）：
     * <ul>
     *   <li>整格取出（{@link LongSlotHandler#extractLong}）；</li>
     *   <li>目标若是 {@link LongSlotHandler}（或单格上限 &gt; 99 的大容量容器）：直接全量插；</li>
     *   <li>普通容器：按 ≤99 分片循环插入，直到搬完或该轮零进展（防死循环，上限 = 剩余 amount）；</li>
     *   <li>搬不走的余量立刻 {@link LongSlotHandler#insertLong} 写回原格。</li>
     * </ul>
     * @return 是否搬动了任何物品
     */
    private boolean pushLongSlotTo(IItemHandler target, int slot)
    {
        ItemStack key = itemHandler.key(slot);
        long available = itemHandler.amount(slot);
        if (key.isEmpty() || available <= 0L)
        {
            return false;
        }
        // 整格取出：搬不走的余量最后写回，绝不丢物、绝不复制
        itemHandler.extractLong(slot, available, false);
        long remaining = available;
        boolean movedAny = false;

        if (target instanceof LongSlotHandler ls)
        {
            // 本模组大容量槽：逐格全量 insertLong（对方按容量/同类合并截断）
            for (int s = 0; s < ls.getSlots() && remaining > 0L; s++)
            {
                long inserted = ls.insertLong(s, key, remaining, false);
                if (inserted > 0L)
                {
                    remaining -= inserted;
                    movedAny = true;
                }
            }
        }
        else if (target.getSlots() > 0 && target.getSlotLimit(0) > LongSlotHandler.VIEW_CAP)
        {
            // 其它大容量容器（单格上限 > 99）：整堆一次性插入
            ItemStack toPush = key.copyWithCount((int) remaining);
            for (int s = 0; s < target.getSlots() && !toPush.isEmpty(); s++)
            {
                ItemStack leftover = target.insertItem(s, toPush, false);
                int moved = toPush.getCount() - leftover.getCount();
                if (moved > 0)
                {
                    remaining -= moved;
                    movedAny = true;
                }
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
                        movedAny = true;
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
            itemHandler.insertLong(slot, key, remaining, false);
        }
        return movedAny;
    }

    /**
     * 纯逻辑：把输入槽最多 {@code batch} 个原木去皮放进对应输出槽
     * （输出为空或同物品且未满才动；供 GameTest 直接调用）。
     *
     * <p>按 long 结算：输入存量读 {@link LongSlotHandler#amount(int)}、输出空位用
     * {@link LongSlotHandler#room(int)}，单格容量 int 上限，不会再被 99 截断。</p>
     *
     * <p>v5.18.8 起 {@code batch} 是「本批处理量」（不再叫 mult）：加速槽满 64 个神之加速时
     * 由 {@link GodGridLayout#roundBatch} 给出 int 上限 2,147,483,647，实际处理量仍受输入存量约束。</p>
     *
     * @return 实际去皮的数量（0 = 没动）
     */
    public static long convertOne(LongSlotHandler handler, int inputSlot, int outputSlot, long batch)
    {
        if (handler == null || batch <= 0L)
        {
            return 0L;
        }
        ItemStack in = handler.key(inputSlot);
        long inAmount = handler.amount(inputSlot);
        if (in.isEmpty() || inAmount <= 0L)
        {
            return 0L;
        }
        ItemStack result = LogStripper.strip(in);
        if (result.isEmpty())
        {
            return 0L;
        }
        ItemStack out = handler.key(outputSlot);
        if (!out.isEmpty() && !ItemStack.isSameItem(out, result))
        {
            return 0L;
        }
        // 本批数量受输入存量与输出空位约束（全程 long，满 64 个加速时 batch = int 上限也不会溢出）
        // v5.18.9 确认：本机没有掉落兜底——输出槽放不下的部分既不产出也不掉到世界里，
        // 本轮 n = 0 直接跳过，输出槽腾出空位后下一轮再处理。
        long n = Math.min(Math.min(batch, inAmount), handler.room(outputSlot));
        if (n <= 0L)
        {
            return 0L;
        }
        // 先按 long 写产物，再按实际写进去的数量扣输入（不经过 ItemStack.getCount() 的 99 上限）
        long inserted = handler.insertLong(outputSlot, result, n, false);
        if (inserted <= 0L)
        {
            return 0L;
        }
        handler.extractLong(inputSlot, inserted, false);
        return inserted;
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
        // v5.18.1：网格库存改成「自管 long」格式（单格可超 99），不再写旧的 ItemStackHandler 格式
        tag.put("GridInventoryLong", itemHandler.saveList(provider));
        tag.put("AccelSlot", accelSlot.serializeNBT(provider));
        tag.putIntArray("FaceModes", faceModes);
        tag.putBoolean("AeEnabled", aeEnabled);
        tag.putInt("TickCounter", tickCounter);
        tag.putInt("SortMode", sortMode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
        // 自管 long 网格库存：新键 GridInventoryLong 优先；老存档读旧格式再灌进来（amount = count），
        // 旧存档不丢。旧 9+9 布局（Size=18）的输出槽 9-17 会在迁移时整体右移到新输出区。
        if (tag.contains("GridInventoryLong", Tag.TAG_LIST))
        {
            itemHandler.loadList(tag.getList("GridInventoryLong", Tag.TAG_COMPOUND), provider);
        }
        else if (tag.contains("Inventory"))
        {
            migrateLegacyInventory(tag.getCompound("Inventory"), provider);
        }
        else if (tag.contains("InputSlot") || tag.contains("OutputSlot"))
        {
            // v5.15.0（1 输入 + 1 输出）旧存档迁移：单槽内容搬进新布局的槽 0 / 槽 17
            ItemStackHandler legacyIn = new ItemStackHandler(1);
            ItemStackHandler legacyOut = new ItemStackHandler(1);
            if (tag.contains("InputSlot"))
            {
                legacyIn.deserializeNBT(provider, tag.getCompound("InputSlot"));
            }
            if (tag.contains("OutputSlot"))
            {
                legacyOut.deserializeNBT(provider, tag.getCompound("OutputSlot"));
            }
            setLegacyEntry(0, legacyIn.getStackInSlot(0));
            setLegacyEntry(OUTPUT_SLOT_START, legacyOut.getStackInSlot(0));
        }
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(provider, tag.getCompound("AccelSlot"));
        }
        if (tag.contains("FaceModes"))
        {
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
        // AE 接入开关（v5.15.9 加入；旧存档缺键默认开）
        this.aeEnabled = !tag.contains("AeEnabled") || tag.getBoolean("AeEnabled");
        tickCounter = tag.getInt("TickCounter");
        // 排序模式（旧存档无此键 → 名称排序）
        int storedSort = tag.contains("SortMode") ? tag.getInt("SortMode") : 0;
        this.sortMode = Math.floorMod(storedSort, GodGridSorting.MODE_COUNT_TOTAL);
    }

    /**
     * 老存档迁移：旧格式是 {@code ItemStackHandler} 的 {@code {Size, Items:[{Slot, id, count}]}}，
     * 读完逐格灌进新 long handler（amount = count）。旧 9+9 布局（Size=18）的输出槽 9-17
     * 先整体右移到新输出区，否则它们会落在新的输入槽位上（输入槽只收可去皮原木，会卡在那里不动）。
     */
    private void migrateLegacyInventory(CompoundTag inv, HolderLookup.Provider provider)
    {
        int oldSize = inv.contains("Size") ? inv.getInt("Size") : TOTAL_SLOTS;
        if (oldSize == LEGACY_INPUT_SLOT_COUNT * 2)
        {
            remapLegacyOutputSlots(inv);
        }
        // 槽数固定为 TOTAL_SLOTS：旧存档的 Size 不采纳，越界的旧槽位自然被忽略
        inv.putInt("Size", TOTAL_SLOTS);
        ItemStackHandler legacy = new ItemStackHandler(TOTAL_SLOTS);
        legacy.deserializeNBT(provider, inv);
        for (int i = 0; i < TOTAL_SLOTS; i++)
        {
            setLegacyEntry(i, legacy.getStackInSlot(i));
        }
    }

    /** 把旧 ItemStackHandler 读出的单格内容灌进新 long handler（amount = count） */
    private void setLegacyEntry(int slot, ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return;
        }
        itemHandler.setEntry(slot, stack, stack.getCount());
    }

    /**
     * 旧 9+9 存档迁移：把 ItemStackHandler NBT 里 Slot 9-17 的条目整体右移到新输出区
     * （{@link #OUTPUT_SLOT_START} 起），否则旧产物会落进新的输入槽位。
     */
    private static void remapLegacyOutputSlots(CompoundTag inv)
    {
        int shift = OUTPUT_SLOT_START - LEGACY_INPUT_SLOT_COUNT;
        ListTag items = inv.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++)
        {
            CompoundTag entry = items.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= LEGACY_INPUT_SLOT_COUNT && slot < LEGACY_INPUT_SLOT_COUNT * 2)
            {
                entry.putInt("Slot", slot + shift);
            }
        }
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_peeler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodPeelerMenu(containerId, inventory, this);
    }
}
