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
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.item.ConfigurableMachine;
import com.godofthings.godgrid.GodGridHost;
import com.godofthings.godgrid.GodGridLayout;
import com.godofthings.godgrid.GodGridLayoutSpec;
import com.godofthings.godgrid.GodGridLayouts;
import com.godofthings.godgrid.GodGridSorting;
import com.godofthings.godgrid.LongSlotHandler;
import com.godofthings.menu.GodFurnaceConfigMenu;
import com.godofthings.menu.GodFurnaceMenu;
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
 *
 * <p><b>v5.1.3：本机的 AE 功能已按用户要求整体删除</b> —— 不再实现
 * {@code IGridConnectedBlockEntity}、不再注册 {@code AECapabilities.IN_WORLD_GRID_NODE_HOST}
 * 能力、没有网格节点、没有「AE 接入开关」、产物不再推入 ME 网络。
 * 因此它**既接不了 AE 线缆，也不可能被无线并网**（荒辰移晷之杖的「AE 网络连接」模式
 * 要先取到目标方块的这个能力，取不到就会直接拒绝，见 {@code AeDeviceLinker}）。
 * 其余 6 台（矿机 / 资源机 / 掉落机 / 砍杀 / 合成台 / 吸收）的 AE 并网不受影响。</p>
 */
public class GodFurnaceBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity, GodGridHost, ConfigurableMachine
{
    // v5.30.0：槽位数量跟随本机自己的 UI 原画（见 GodGridLayouts）。
    // 熔炉原画是「上排 17 格输入 + 下排 15 格输出」，故输出由 17 缩到 15。
    // 保留 static final 是刻意的：GameTest 与其它类直接用这些常量建 handler，
    // 改成实例字段会波及测试代码（测试总数必须保持 45 条不变）。
    private static final GodGridLayoutSpec LAYOUT = GodGridLayouts.of("god_furnace");
    public static final int INPUT_SLOT_COUNT = LAYOUT.inputCount();
    public static final int OUTPUT_SLOT_COUNT = LAYOUT.outputCount();
    public static final int TOTAL_SLOTS = INPUT_SLOT_COUNT + OUTPUT_SLOT_COUNT;
    public static final int OUTPUT_SLOT_START = INPUT_SLOT_COUNT;

    /**
     * 每 tick 输入抽取（pullFrom）的轮数上限：每轮最多搬 {@link LongSlotHandler#VIEW_CAP}（99，对外单堆上限），
     * 即每个源槽每 tick 最多 3 × 99 = 297。输出侧（pushTo / pushOutputToAe）不再设轮数上限：
     * 每 tick 把输出槽整个 amount 一次搬完（见 {@link #pushLongSlotTo}）。
     */
    private static final int TRANSFER_ROUNDS = 3;

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
     * 神之熔炉网格 34 格是「单格容量突破 99」的试点（v5.18.2 起六台网格机器统一）：
     * 改用自管 long 计数的 {@link LongSlotHandler}
     * （原版 {@code ItemStackHandler} 受 {@code ItemStack.CODEC} 的 {@code intRange(1,99)} 限制，
     * 单格最多 99，超了存档 / 掉落 / 发包都会崩）。单格容量默认 int 上限 2,147,483,647，对外视图仍是 ≤99 的堆。
     */
    private final LongSlotHandler itemHandler = new LongSlotHandler(TOTAL_SLOTS)
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
    };

    // 每个面一个模式，索引 = Direction.get3DDataValue()，取值见 FaceMode.getId()
    private final int[] faceModes = new int[6];

    private final IItemHandler[] sideHandlers = new IItemHandler[6];

    /** 是否接入 AE（v5.15.5 恢复：并网后熔炼产物自动输出进 AE 网络，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网，照抄资源三机）。 */
    private final AeGridNode aeNode = new AeGridNode(this);

    public GodFurnaceBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_FURNACE_BE.get(), pos, state);
        for (int i = 0; i < 6; i++)
        {
            final int idx = i;
            sideHandlers[idx] = new SideHandler(idx);
        }
    }

    /**
     * 网格槽处理器：自管 long 计数，单格容量默认 int 上限 2,147,483,647（{@link LongSlotHandler#DEFAULT_CAPACITY}），
     * 对外视图仍是 ≤99 的 ItemStack（保证别的模组管道 / AE / 容器同步拿不到非法堆）。
     */
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
    public String gridLayoutId()
    {
        return "god_furnace";
    }

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

    /** 该机器槽的真实存量（GUI 右下角大数字用；客户端读到的是同步过来的近似值） */
    @Override
    public long gridAmount(int machineSlot)
    {
        return itemHandler.amount(machineSlot);
    }

    /** 开启数量同步与界面大数字（六台网格机器均为 true） */
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
                return Component.translatable("gui.godofthings.face_config");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p)
            {
                return new GodFurnaceConfigMenu(containerId, inventory, GodFurnaceBlockEntity.this);
            }
        }, buf -> buf.writeBlockPos(getBlockPos()));
    }

    /**
     * 单轮处理量（GUI 加速槽提示与 smeltSlot 共用同一口径）：加速槽满 64 个神之加速 → int 上限
     * 2,147,483,647，否则 = 内置 64 × 并行倍率。见 {@link GodGridLayout#roundBatch}。
     */
    @Override
    public int gridOperationsPerColumn()
    {
        return (int) GodGridLayout.roundBatch(
                accelSlot.getStackInSlot(0).getCount(), GodGridLayout.BUILTIN_PARALLEL, getParallelMultiplier());
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
            // v5.15.5：恢复 AE 并网（照抄资源三机）——线缆直连、占一个频道、产物自动输出进网络。
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_FURNACE_BE.get(),
                    (be, side) -> be);
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
        // AE 产物输出：每 tick 全量搬运（不再每 20 tick 节流，v5.18.3）
        be.pushOutputToAe();
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

    /**
     * 从邻居抽取可熔炼物品到任意有空位的输入槽。
     * 每 tick 每面、每个源槽最多 {@link #TRANSFER_ROUNDS} 轮 × 99（对外单堆上限），
     * 输入槽容量 int 上限时能快速把原料囤起来。
     */
    private void pullFrom(IItemHandler neighbor)
    {
        for (int s = 0; s < neighbor.getSlots(); s++)
        {
            boolean moved = false;
            for (int round = 0; round < TRANSFER_ROUNDS; round++)
            {
                ItemStack src = neighbor.getStackInSlot(s);
                if (src.isEmpty() || !isSmeltable(src))
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

    /** 找到可接受该原料的输入槽（同物品且还有空位优先，否则找空槽）；没有则 -1 */
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
            // 1.21.1：isSameItemSameTags → isSameItemSameComponents
            else if (itemHandler.room(i) > 0L
                    && ItemStack.isSameItemSameComponents(itemHandler.key(i), stack))
            {
                return i;
            }
        }
        return emptySlot;
    }

    /**
     * 把输出格的产物推给邻居：每 tick 每个输出槽把整个 amount 一次搬完（v5.18.3 全量搬运）——
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

    // ---- AE 网格节点（v5.15.5 恢复：线缆直连并网，产物自动输出进 AE，占一个频道） ----

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

    /** 是否接入 AE（并网后熔炼产物自动输出进 AE 网络）。 */
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
     * 把输出槽产物推入 AE 网络（只推输出槽 17-33；输入槽是熔炼队列不推）。
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



    // ---- 熔炼：无燃料，每个输入槽每 tick 最多熔掉一列并行额度（按 long 计算） ----

    private void smelt()
    {
        for (int i = 0; i < INPUT_SLOT_COUNT; i++)
        {
            smeltSlot(i);
        }
    }

    private void smeltSlot(int inputSlot)
    {
        ItemStack inputKey = itemHandler.key(inputSlot);
        long inputAmount = itemHandler.amount(inputSlot);
        if (inputKey.isEmpty() || inputAmount <= 0L)
        {
            return;
        }

        // 1.21.1：配方探测入参由 Container 改为 RecipeInput，且烹饪配方泛型固定绑定 SingleRecipeInput
        //（AbstractCookingRecipe implements Recipe<SingleRecipeInput>），SingleSlotContainer 包装不再可用，
        // 改用原版 SingleRecipeInput 快照（matches() 只读 getItem，语义不变）
        SingleRecipeInput inputContainer = new SingleRecipeInput(inputKey.copyWithCount(1));
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

        // 固定配对：输入槽 i → 输出槽 (INPUT_SLOT_COUNT + i)，17 对管线完全并行
        int outputSlot = INPUT_SLOT_COUNT + inputSlot;
        ItemStack curKey = itemHandler.key(outputSlot);
        // 1.21.1：isSameItemSameTags → isSameItemSameComponents
        if (!curKey.isEmpty() && !ItemStack.isSameItemSameComponents(curKey, resultTemplate))
        {
            // 对应输出槽被不同物品占住：回退到其他空槽
            outputSlot = findOutputSlotFor(resultTemplate);
            if (outputSlot < 0)
            {
                return;
            }
        }

        // 配方每次熔炼的产出数量（原版多为 1，模组配方可能 >1，如 1 矿 → 2 锭）
        long perResult = Math.max(1, resultTemplate.getCount());
        // 输出槽剩余容量按 long 算：单格容量 int 上限，不再受 ItemStack 的 99 上限约束
        long outputFree = itemHandler.room(outputSlot);
        // v5.18.9 容量预判：输出格剩余容量连一份产物都放不下 → 本轮直接跳过
        //（不消耗原料、不产出、也绝不把产物掉到世界里——熔炉没有内置储存，17 个输出槽就是唯一去处）
        if (outputFree < perResult)
        {
            return;
        }
        // 单列每 tick 的并行额度（long）：加速槽满 64 个神之加速 → int 上限 2,147,483,647，
        // 否则 = 内置 64 × 神之加速倍率（见 GodGridLayout#roundBatch）
        long columnBudget = GodGridLayout.roundBatch(
                accelSlot.getStackInSlot(0).getCount(), GodGridLayout.BUILTIN_PARALLEL, getParallelMultiplier());
        long toSmelt = Math.min(Math.min(inputAmount, outputFree / perResult), columnBudget);
        if (toSmelt <= 0L)
        {
            return;
        }
        long produced = toSmelt * perResult;
        long inserted = itemHandler.insertLong(outputSlot, resultTemplate, produced, false);
        // 只有真正写进输出槽的产物才扣输入（l 累加按 long，不经过 ItemStack.getCount()）
        long consumed = inserted / perResult;
        if (consumed > 0L)
        {
            itemHandler.extractLong(inputSlot, consumed, false);
            setChanged();
        }
    }

    /** 找到可放入该产物的输出格（同物品且还有空位优先，否则找空槽）；没有则 -1 */
    private int findOutputSlotFor(ItemStack result)
    {
        int emptySlot = -1;
        for (int i = OUTPUT_SLOT_START; i < TOTAL_SLOTS; i++)
        {
            if (itemHandler.isEmpty(i))
            {
                if (emptySlot < 0)
                {
                    emptySlot = i;
                }
            }
            // 1.21.1：isSameItemSameTags → isSameItemSameComponents
            else if (itemHandler.room(i) > 0L
                    && ItemStack.isSameItemSameComponents(itemHandler.key(i), result))
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
        // v5.18.1：网格库存改成「自管 long」格式（单格可超 99），不再写旧的 ItemStackHandler 格式。
        // 每条 {Slot:int, Item:count=1 的物品, Amount:long}，见 LongSlotHandler#saveList。
        tag.put("GridInventoryLong", itemHandler.saveList(registries));
        tag.put("AccelSlot", accelSlot.serializeNBT(registries));
        tag.putIntArray("FaceModes", faceModes);
        tag.putBoolean("AeEnabled", aeEnabled);
        tag.putInt("SortMode", sortMode);
    }

    // 1.21.1（1.20.5+ 破坏性变更）：load(CompoundTag) → loadAdditional(CompoundTag, HolderLookup.Provider)
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        // v5.1.3：AE 已移除，旧存档里的 "AeEnabled" 键直接忽略（不再读取）。
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(registries, tag.getCompound("AccelSlot"));
        }
        // 新格式优先；老存档没有该键 → 读旧的 ItemStackHandler 格式再灌进新 handler（amount = count）
        if (tag.contains("GridInventoryLong", Tag.TAG_LIST))
        {
            itemHandler.loadList(tag.getList("GridInventoryLong", Tag.TAG_COMPOUND), registries);
        }
        else if (tag.contains("Inventory"))
        {
            migrateLegacyInventory(tag.getCompound("Inventory"), registries);
        }
        if (tag.contains("FaceModes"))
        {
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
        // AE 接入开关（v5.15.5 恢复；旧存档缺键默认开）
        this.aeEnabled = !tag.contains("AeEnabled") || tag.getBoolean("AeEnabled");
        // 排序模式（旧存档无此键 → 名称排序）
        int storedSort = tag.contains("SortMode") ? tag.getInt("SortMode") : 0;
        this.sortMode = Math.floorMod(storedSort, GodGridSorting.MODE_COUNT_TOTAL);
    }

    /**
     * 老存档迁移：旧格式是 {@code ItemStackHandler} 的 {@code {Size, Items:[{Slot, id, count≤99}]}}，
     * 读出来后逐格灌进新 handler（amount = count），并保留旧的「2 / 6 槽布局 → 34 槽布局」搬运规则。
     */
    private void migrateLegacyInventory(CompoundTag inv, HolderLookup.Provider registries)
    {
        int oldSize = inv.contains("Size") ? inv.getInt("Size") : itemHandler.getSlots();
        // 强制使用新布局的 34 槽（17 输入 + 17 输出）
        inv.putInt("Size", TOTAL_SLOTS);
        // 1.21.1：ItemStackHandler serializeNBT/deserializeNBT 需传 HolderLookup.Provider
        ItemStackHandler legacy = new ItemStackHandler(TOTAL_SLOTS);
        legacy.deserializeNBT(registries, inv);
        for (int i = 0; i < TOTAL_SLOTS; i++)
        {
            itemHandler.setStackInSlot(i, legacy.getStackInSlot(i));
        }
        if (oldSize >= TOTAL_SLOTS)
        {
            return;
        }
        // 兼容旧版布局：
        // - 旧 2 槽（1输入+1输出）：旧输出在 slot1 → 迁到新输出区
        // - 旧 6 槽（3输入+3输出）：旧输出在 slot3-5 → 迁到新输出区(slot17+)
        int oldOutputStart = Math.max(1, oldSize / 2); // 旧布局输出区起点（2槽→1, 6槽→3）
        for (int legacySlot = oldOutputStart; legacySlot < Math.min(oldSize, INPUT_SLOT_COUNT); legacySlot++)
        {
            ItemStack key = itemHandler.key(legacySlot);
            if (key.isEmpty())
            {
                continue;
            }
            long amount = itemHandler.amount(legacySlot);
            int target = findOutputSlotFor(key);
            if (target < 0)
            {
                target = firstEmptyOutputSlot();
            }
            if (target >= 0)
            {
                itemHandler.setEntry(target, key, amount);
                itemHandler.clear(legacySlot);
            }
        }
    }

    /** 第一个空输出槽；没有则 -1 */
    private int firstEmptyOutputSlot()
    {
        for (int i = OUTPUT_SLOT_START; i < TOTAL_SLOTS; i++)
        {
            if (itemHandler.isEmpty(i))
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
