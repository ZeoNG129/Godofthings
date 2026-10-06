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
import com.godofthings.item.GodAcceleratorItem;
import com.godofthings.menu.GodMinerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.List;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * 神之矿机方块实体。
 * - 无需能源，点「开始」后向下挖掘方形区域（半径 1-1600）
 * - 挖掘方式：一竖列一竖列地挖（每列从矿机下方一直钻到世界底部，再钻下一列）
 * - 除基岩外所有方块（含液体）都会被挖掉，液体收集进内置无限液体罐
 * - 默认速度约 4 块/每tick（约 1 竖列/20 tick），效率每级 ×(1+3级) 加速
 * - 挖完后可再次点击开始：自动从顶部重新挖（支持改半径后重新工作）
 * - 内置无限大小物品储存，六面默认全部自动输出
 */
public class GodMinerBlockEntity extends BlockEntity implements MenuProvider, IGridConnectedBlockEntity, MachineOwner
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

    /** 矿机最大挖掘半径（格，方形半径），可经 godofthings-machines.toml 调整。
     *  <p>v5.12.1 起实时读配置（此前 static final 快照需重启；MAX_RADIUS 是热路径上的 UI/挖掘共用量，
     *  保留为方法以统一入口）。</p> */
    public static int maxRadius()
    {
        return MachinesConfig.MINER_MAX_RADIUS.get();
    }

    /** 每个 tick 最多处理的方块数，防止卡顿（实时读配置）。 */
    public static int maxBlocksPerTick()
    {
        return MachinesConfig.MINER_MAX_BLOCKS_PER_TICK.get();
    }

    private final InfiniteItemHandler itemHandler = new InfiniteItemHandler();
    private final FluidTank tank = new FluidTank(Integer.MAX_VALUE);

    /** 每个面一个模式，索引 = Direction.get3DDataValue()，取值见 FaceMode.getId()。
     *  默认全 OUTPUT：保持「六面默认全部自动输出」的旧行为，旧存档未存 FaceModes 时也走这里。 */
    private final int[] faceModes = {
            FaceMode.OUTPUT.getId(), FaceMode.OUTPUT.getId(), FaceMode.OUTPUT.getId(),
            FaceMode.OUTPUT.getId(), FaceMode.OUTPUT.getId(), FaceMode.OUTPUT.getId() };
    private final SideHandler[] sideHandlers = new SideHandler[6];
    /** OUTPUT 面暴露的只出不进液体能力（外部只能抽走，不能向矿机灌液）。 */
    private final OutputFluidHandler outputFluidHandler = new OutputFluidHandler();

    /** 神之加速槽：放入神之加速（放满 64 个）后，挖一整列基础 tick 降至 1 */
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

    /** 是否接入 AE（线缆直连并网，产物主动输出进 AE，占一个频道）。 */
    private boolean aeEnabled = true;

    /** AE 网格节点（线缆直连并网）。 */
    private final AeGridNode aeNode = new AeGridNode(this);
    private int aeTick = 0;

    private boolean running = false;
    private int radius = 16;
    private int currentY = Integer.MIN_VALUE; // 当前竖列正在挖的 Y（未初始化标记）
    private int columnIndex = Integer.MIN_VALUE; // 当前正在挖的竖列（方形区域内第几列）
    private int tickCounter = 0;
    /** 已验证挖空/全空、无需再扫描的竖列索引缓存 */
    private BitSet emptyColumns = new BitSet();
    /** 本矿机实际强制加载过的区块（chunkKey），释放时只清自己设置的，避免整区 O(半径²) 扫描 */
    private final LongOpenHashSet forcedChunks = new LongOpenHashSet();
    /** 是否已执行过加载时整区清理（每个 BE 生命周期一次） */
    private boolean areaClearedOnLoad = false;

    private int efficiencyLevel = 0;
    private int fortuneLevel = 0;
    private boolean silkTouch = false;
    /** 缓存的假镐（带时运/精准采集）：避免每挖一块都重建 + 附魔（附魔需查附魔注册表，逐块重建是性能热点） */
    private ItemStack cachedTool = ItemStack.EMPTY;

    public GodMinerBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_MINER_BE.get(), pos, state);
        itemHandler.setOnChange(this::setChanged);
        for (int i = 0; i < 6; i++)
        {
            this.sideHandlers[i] = new SideHandler(i);
        }
    }

    public InfiniteItemHandler getItemHandler()
    {
        return itemHandler;
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

    // ---- AE 网格节点（线缆直连并网，产物主动输出进 AE） ----

    @Override
    public IManagedGridNode getMainNode() { return aeNode.getMainNode(); }

    @Override
    public void saveChanges() { setChanged(); }

    /** 把内置储存产物推入 AE 网络（节流由 tick 控制）。 */
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

    /** 该面是否允许推出产物（OUTPUT / BOTH） */
    private boolean isOutputFace(Direction dir)
    {
        FaceMode mode = FaceMode.fromId(faceModes[dir.get3DDataValue()]);
        return mode == FaceMode.OUTPUT || mode == FaceMode.BOTH;
    }

    /** 该面是否允许抽入神之加速（INPUT / BOTH） */
    private boolean isInputFace(Direction dir)
    {
        FaceMode mode = FaceMode.fromId(faceModes[dir.get3DDataValue()]);
        return mode == FaceMode.INPUT || mode == FaceMode.BOTH;
    }

    // ---- capability：每个面按模式暴露受限的物品/液体能力 ----

    /** 物品能力入口：side == null 或 NONE 面返回 null（与神之熔炉/神之吸收一致）。 */
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

    /** 液体能力入口：只有 OUTPUT / BOTH 面暴露（矿机只产液体，不接受外部灌入）。 */
    @Nullable
    IFluidHandler getSideFluidCapability(@Nullable Direction side)
    {
        return side != null && isOutputFace(side) ? outputFluidHandler : null;
    }

    /**
     * 某个面的包装物品 handler（槽位布局与神之熔炉一致：输入在前、输出在后）：
     * <ul>
     *   <li>槽 0 = 神之加速槽（输入槽）：只有 INPUT / BOTH 面可插入，且只收神之加速；任何面都抽不走。</li>
     *   <li>槽 1..N = 内置无限储存（输出区）：只有 OUTPUT / BOTH 面可抽取，任何面都插不进。</li>
     * </ul>
     * 面模式可在运行时切换，故每次调用动态读取，无需失效缓存。
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
            return 1 + itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot)
        {
            return slot == 0 ? accelSlot.getStackInSlot(0) : itemHandler.getStackInSlot(slot - 1);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
        {
            if (slot != 0)
            {
                return stack; // 内置储存是产物区，不接受外部插入
            }
            return (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH)
                    ? accelSlot.insertItem(0, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            if (slot == 0)
            {
                return ItemStack.EMPTY; // 加速槽不被管道抽走
            }
            return (mode() == FaceMode.OUTPUT || mode() == FaceMode.BOTH)
                    ? itemHandler.extractItem(slot - 1, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return slot == 0 ? accelSlot.getSlotLimit(0) : Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack)
        {
            return slot == 0
                    && (mode() == FaceMode.INPUT || mode() == FaceMode.BOTH)
                    && accelSlot.isItemValid(0, stack);
        }
    }

    /** OUTPUT 面的液体能力：只允许抽走储液罐里的液体，fill 恒为 0。 */
    private class OutputFluidHandler implements IFluidHandler
    {
        @Override
        public int getTanks()
        {
            return tank.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex)
        {
            return tank.getFluidInTank(tankIndex);
        }

        @Override
        public int getTankCapacity(int tankIndex)
        {
            return tank.getTankCapacity(tankIndex);
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack)
        {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action)
        {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action)
        {
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action)
        {
            return tank.drain(maxDrain, action);
        }
    }

    public FluidTank getTank()
    {
        return tank;
    }

    /** 神之加速槽（只接受神之加速，最多 64 个） */
    public ItemStackHandler getAccelSlot()
    {
        return accelSlot;
    }

    public boolean isRunning()
    {
        return running;
    }

    public void setRunning(boolean value)
    {
        if (running && !value)
        {
            releaseForcedChunks(); // 停止时释放强制加载的区块
        }
        running = value;
        if (running)
        {
            // 已挖完（到底/所有竖列挖完）或未初始化时，从顶部重新开始
            if (currentY == Integer.MIN_VALUE || columnIndex == Integer.MIN_VALUE
                    || (level != null && (currentY < level.getMinBuildHeight() || columnIndex >= layerArea())))
            {
                columnIndex = 0;
                currentY = worldPosition.getY() - 1;
            }
        }
        setChanged();
    }

    public int getRadius()
    {
        return radius;
    }

    public void setRadius(int value)
    {
        int newRadius = Math.max(1, Math.min(maxRadius(), value));
        if (newRadius == radius)
        {
            return; // 半径未变，不重置进度
        }
        radius = newRadius;
        // 半径变化后列坐标映射改变（columnBlockPos 依赖 side），清空空列缓存并重置挖掘进度重新挖，
        // 避免增大半径时 columnIndex 沿用旧映射导致挖掘位置漂移/错乱
        emptyColumns.clear();
        if (columnIndex != Integer.MIN_VALUE)
        {
            columnIndex = 0;
            currentY = worldPosition.getY() - 1;
        }
        setChanged();
    }

    public int getCurrentY()
    {
        return currentY == Integer.MIN_VALUE ? worldPosition.getY() - 1 : currentY;
    }

    public void setCurrentY(int value)
    {
        currentY = value;
        setChanged();
    }

    /** 重置挖掘进度：从矿机正下方开始重新挖（放置时调用，保证初始状态一致） */
    public void resetDigging()
    {
        columnIndex = 0;
        currentY = worldPosition.getY() - 1;
        tickCounter = 0;
        emptyColumns.clear();
        setChanged();
    }

    public int getEfficiencyLevel()
    {
        return efficiencyLevel;
    }

    public int getFortuneLevel()
    {
        return fortuneLevel;
    }

    public boolean isSilkTouch()
    {
        return silkTouch;
    }

    public void setEnchants(int efficiency, int fortune, boolean silk)
    {
        efficiencyLevel = efficiency;
        fortuneLevel = fortune;
        silkTouch = silk;
        cachedTool = ItemStack.EMPTY; // 附魔变化后重建缓存工具
        setChanged();
    }

    /** 切换时运 3（与时运 0 互斥；与精准采集互斥——开启时运关闭精准）。 */
    public void toggleFortune()
    {
        if (fortuneLevel == 3)
        {
            fortuneLevel = 0;
        }
        else
        {
            fortuneLevel = 3;
            silkTouch = false;
        }
        cachedTool = ItemStack.EMPTY;
        setChanged();
    }

    /** 切换精准采集（与开启时运互斥——开启精准关闭时运）。 */
    public void toggleSilkTouch()
    {
        silkTouch = !silkTouch;
        if (silkTouch)
        {
            fortuneLevel = 0;
        }
        cachedTool = ItemStack.EMPTY;
        setChanged();
    }

    /** 挖 1 整列所需的 tick：默认 20，效率每级 -4，最低 1（效率 V = 1 tick/列）；
     *  神之加速槽放满 64 个后，挖一整列基础 tick 直接降为 1。 */
    public int getTicksPerColumn()
    {
        if (accelSlot.getStackInSlot(0).getCount() >= 64)
        {
            return 1;
        }
        return Math.max(1, MachinesConfig.MINER_TICKS_PER_COLUMN_BASE.get() - 4 * efficiencyLevel);
    }

    public int getFluidAmount()
    {
        return tank.getFluidAmount();
    }

    public FluidStack getFluid()
    {
        return tank.getFluid();
    }

    /** 内置储存的物品堆叠数量 */
    public int getStorageCount()
    {
        return itemHandler.getStacks().size();
    }

    // ---- 每 tick 逻辑 ----

    public static void tick(Level level, BlockPos pos, BlockState state, GodMinerBlockEntity be)
    {
        if (level.isClientSide)
        {
            return;
        }
        be.tickServer();
        // AE 产物输出节流：每 20 tick（1 秒）推一次
        be.aeTick++;
        if (be.aeTick >= 20)
        {
            be.aeTick = 0;
            be.pushOutputToAe();
        }
    }

    private void tickServer()
    {
        if (running)
        {
            if (currentY == Integer.MIN_VALUE || columnIndex == Integer.MIN_VALUE)
            {
                columnIndex = 0;
                currentY = worldPosition.getY() - 1;
            }
            int area = layerArea();
            // 快速跳过已挖空的列：emptyColumns 缓存命中时直接跳跃，否则才整列扫描
            if (columnIndex < area && emptyColumns.get(columnIndex))
            {
                columnIndex = emptyColumns.nextClearBit(columnIndex);
            }
            else if (columnIndex < area)
            {
                // 当前列所在区块未加载：强制加载并等待，绝不当作空列跳过
                if (!ensureColumnChunkLoaded())
                {
                    return; // 等待区块加载完成
                }
                if (!columnHasMinableBlocks())
                {
                    markColumnEmpty(columnIndex);
                    columnIndex++;
                }
            }
            if (columnIndex >= area)
            {
                running = false; // 范围内没有需要挖掘的方块了
                releaseForcedChunks();
                setChanged();
                autoTransfer();
                return;
            }
            tickCounter++;
            if (tickCounter >= getTicksPerColumn())
            {
                tickCounter = 0;
                // 每 N tick 挖当前列至多 MAX_BLOCKS_PER_TICK 块；只有整列挖完才跳到下一列，
                // 否则下个周期继续挖同一列（maxBlocksPerTick < 列高时避免静默漏挖剩余方块）
                if (digColumn())
                {
                    columnIndex++;
                    currentY = worldPosition.getY() - 1;
                    if (columnIndex >= area)
                    {
                        running = false; // 所有竖列挖完
                        releaseForcedChunks();
                    }
                }
                setChanged();
            }
        }
        else
        {
            tickCounter = 0;
        }
        autoTransfer();
    }

    /** 确保当前列所在区块已加载：未加载则请求强制加载并返回 false（等待） */
    private boolean ensureColumnChunkLoaded()
    {
        BlockPos colPos = columnBlockPos();
        if (level.isLoaded(colPos))
        {
            return true;
        }
        if (level instanceof ServerLevel serverLevel)
        {
            int cx = colPos.getX() >> 4;
            int cz = colPos.getZ() >> 4;
            forcedChunks.add(chunkKey(cx, cz));
            serverLevel.setChunkForced(cx, cz, true);
        }
        return false;
    }

    /** 停止或挖完时释放本矿机强制加载过的区块，避免常驻内存（只清自己设过的，O(实际块数)） */
    private void releaseForcedChunks()
    {
        if (!(level instanceof ServerLevel serverLevel) || forcedChunks.isEmpty())
        {
            return;
        }
        forcedChunks.forEach(key -> serverLevel.setChunkForced((int) (key >> 32), (int) key, false));
        forcedChunks.clear();
    }

    /** 整区释放覆盖范围内所有强制区块（仅 onLoad 清理上一会话残留时调用一次） */
    private void releaseAreaChunks(ServerLevel serverLevel)
    {
        for (int cx = (worldPosition.getX() - radius) >> 4; cx <= (worldPosition.getX() + radius) >> 4; cx++)
        {
            for (int cz = (worldPosition.getZ() - radius) >> 4; cz <= (worldPosition.getZ() + radius) >> 4; cz++)
            {
                serverLevel.setChunkForced(cx, cz, false);
            }
        }
    }

    private static long chunkKey(int x, int z)
    {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }

    @Override
    public void setRemoved()
    {
        aeNode.destroy();
        super.setRemoved();
        // 运行中拆除矿机：释放本机强制加载过的区块，避免区块常驻内存泄漏
        releaseForcedChunks();
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        aeNode.create(level, worldPosition);
        if (areaClearedOnLoad || level == null || level.isClientSide || !(level instanceof ServerLevel serverLevel))
        {
            return;
        }
        areaClearedOnLoad = true;
        if (!running)
        {
            // 世界/区块加载时清理上一会话可能残留的强制加载（运行中则按需重新加载，不打断作业）
            releaseAreaChunks(serverLevel);
            forcedChunks.clear();
        }
    }

    /** 判断当前列是否还有可挖掘的方块（非空气、非基岩），用于跳过已挖空区域 */
    private boolean columnHasMinableBlocks()
    {
        int y = currentY;
        boolean sawUnloaded = false;
        while (y >= level.getMinBuildHeight())
        {
            BlockPos p = columnBlockPos().atY(y);
            if (level.isLoaded(p))
            {
                BlockState state = level.getBlockState(p);
                if (!state.isAir() && state.getBlock() != Blocks.BEDROCK)
                {
                    return true;
                }
            }
            else
            {
                sawUnloaded = true; // 有未加载位置，保守视为有可挖方块
            }
            y--;
        }
        return sawUnloaded;
    }

    /** 标记某一列为已挖空，后续跳过时不再扫描 */
    private void markColumnEmpty(int column)
    {
        if (column >= 0)
        {
            emptyColumns.set(column);
        }
    }

    /** 挖当前列的一部分（至多 MAX_BLOCKS_PER_TICK 块），返回是否整列挖完。
     *  用 currentY 记录列内进度：当 maxBlocksPerTick < 列高时跨多个周期继续挖同一列，
     *  避免一次挖不完就跳列导致剩余方块永久漏挖。 */
    private boolean digColumn()
    {
        int y = currentY;
        int mined = 0;
        while (y >= level.getMinBuildHeight() && mined < maxBlocksPerTick())
        {
            mineBlock(columnBlockPos().atY(y));
            y--;
            mined++;
        }
        currentY = y; // 挖完时为 minBuildHeight-1，未挖完时保留列内剩余进度
        if (y < level.getMinBuildHeight())
        {
            markColumnEmpty(columnIndex); // 整列挖完，缓存为空列
            return true;
        }
        return false;
    }

    private int layerArea()
    {
        int side = 2 * radius + 1;
        return side * side;
    }

    /** 当前竖列所在的水平位置 */
    private BlockPos columnBlockPos()
    {
        int side = 2 * radius + 1;
        int dx = columnIndex % side - radius;
        int dz = columnIndex / side - radius;
        return worldPosition.offset(dx, 0, dz);
    }

    private void mineBlock(BlockPos pos)
    {
        if (!level.isLoaded(pos))
        {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir())
        {
            return;
        }
        if (state.getBlock() == Blocks.BEDROCK)
        {
            return; // 保留基岩
        }
        if (state.getBlock() instanceof LiquidBlock liquidBlock)
        {
            if (state.getFluidState().isSource())
            {
                tank.fill(new FluidStack(liquidBlock.fluid, 1000), IFluidHandler.FluidAction.EXECUTE);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); // 液体收集进储液罐，无物品掉落
            return;
        }
        List<ItemStack> drops = getDrops(state, pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        // 神之共鸣（v5.15.4）：点石成金 × 财源滚滚 + 自动熔炼（主人在线 + 穿齐全套 + 对应开关开）
        ServerLevel serverLevel = (ServerLevel) level; // 矿机只在服务端 tick（getDrops 同样强转）
        double bomb = com.godofthings.handler.ArmorSkillHandler.lootBombBoost(serverLevel, owner);
        double blockMult = com.godofthings.handler.ArmorSkillHandler.blockDropBoost(serverLevel, owner) * bomb;
        boolean autoSmelt = com.godofthings.handler.ArmorSkillHandler.autoSmeltOn(serverLevel, owner);
        for (ItemStack drop : drops)
        {
            if (drop.isEmpty())
            {
                continue;
            }
            ItemStack toInsert = drop;
            if (autoSmelt)
            {
                // 同玩家侧「神之熔炼」：产物数量 = 配方产物数 × 输入个数
                net.minecraft.world.item.ItemStack out = com.godofthings.handler.ArmorSkillHandler
                        .smeltResult(serverLevel.getServer(), drop);
                if (!out.isEmpty())
                {
                    toInsert = out.copyWithCount(Math.max(1, out.getCount() * drop.getCount()));
                }
            }
            if (blockMult > 1.0)
            {
                toInsert = toInsert.copyWithCount((int) Math.min(Integer.MAX_VALUE / 2L,
                        Math.round(toInsert.getCount() * blockMult)));
            }
            insertDrop(toInsert);
        }
    }

    /** 缓存附魔假镐（懒加载，附魔变化时由 setEnchants 重建） */
    private ItemStack getTool()
    {
        if (cachedTool.isEmpty())
        {
            cachedTool = new ItemStack(Items.DIAMOND_PICKAXE);
            if (silkTouch)
            {
                cachedTool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
            }
            if (fortuneLevel > 0)
            {
                cachedTool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.FORTUNE), fortuneLevel);
            }
        }
        return cachedTool;
    }

    /** 计算掉落物：用缓存假镐模拟，应用时运/精准采集 */
    private List<ItemStack> getDrops(BlockState state, BlockPos pos)
    {
        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, null, null, getTool());
        if (drops.isEmpty() && !state.getBlock().asItem().equals(Items.AIR))
        {
            // 工具类型不匹配等原因导致空掉落 → 直接给方块本体
            drops = List.of(new ItemStack(state.getBlock().asItem()));
        }
        return drops;
    }

    /** 存入内置无限储存（同类恒单堆、数量无上限；仅当不同物品类型数超上限时剩余物掉落到矿机旁，不吞物品） */
    private void insertDrop(ItemStack stack)
    {
        ItemStack leftover = itemHandler.insertItem(-1, stack, false);
        if (!leftover.isEmpty())
        {
            InfiniteItemHandler.dropRemainder(level, worldPosition, leftover);
        }
    }

    /** 按面配置自动传输：INPUT 面抽入神之加速，OUTPUT / BOTH 面推出产物与液体。 */
    private void autoTransfer()
    {
        for (Direction dir : Direction.values())
        {
            if (getFaceMode(dir) == FaceMode.NONE.getId())
            {
                continue;
            }
            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos))
            {
                continue;
            }
            if (isInputFace(dir))
            {
                pullAccelerator(neighborPos, dir);
            }
            if (isOutputFace(dir))
            {
                pushOutput(neighborPos, dir);
                pushFluid(neighborPos, dir);
            }
        }
    }

    /** INPUT 面：从相邻容器抽取神之加速补充加速槽（加速槽已满则跳过）。 */
    private void pullAccelerator(BlockPos neighborPos, Direction dir)
    {
        if (accelSlot.getStackInSlot(0).getCount() >= accelSlot.getSlotLimit(0))
        {
            return;
        }
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
        if (handler == null)
        {
            return;
        }
        for (int s = 0; s < handler.getSlots(); s++)
        {
            ItemStack src = handler.getStackInSlot(s);
            if (src.isEmpty() || !(src.getItem() instanceof GodAcceleratorItem))
            {
                continue;
            }
            // 先真放入加速槽，再按实际收下的数量从邻居取走；邻居拿不出那么多就把多收的还回去（兜底掉落，绝不凭空增殖）
            ItemStack leftover = accelSlot.insertItem(0, src.copy(), false);
            int moved = src.getCount() - leftover.getCount();
            if (moved <= 0)
            {
                return;
            }
            ItemStack taken = handler.extractItem(s, moved, false);
            if (taken.getCount() < moved)
            {
                ItemStack giveBack = accelSlot.extractItem(0, moved - taken.getCount(), false);
                if (!giveBack.isEmpty())
                {
                    ItemStack stillLeft = handler.insertItem(s, giveBack, false);
                    if (!stillLeft.isEmpty())
                    {
                        InfiniteItemHandler.dropRemainder(level, worldPosition, stillLeft);
                    }
                }
            }
            setChanged();
            return; // 每 tick 每面最多搬一次，避免高频搬运
        }
    }

    /** OUTPUT / BOTH 面：把内置储存产物推给该面相邻容器 */
    private void pushOutput(BlockPos neighborPos, Direction dir)
    {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
        if (handler != null)
        {
            pushTo(handler);
        }
    }

    /** OUTPUT / BOTH 面：把储液罐里的液体推给该面相邻可装液体的容器 */
    private void pushFluid(BlockPos neighborPos, Direction dir)
    {
        if (tank.getFluidAmount() <= 0)
        {
            return;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
        if (handler == null)
        {
            return;
        }
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty())
        {
            return;
        }
        int filled = handler.fill(fluid.copy(), IFluidHandler.FluidAction.SIMULATE);
        if (filled > 0)
        {
            // 真正填入邻居，再按实际填入量从矿机排掉
            int actual = handler.fill(fluid.copyWithAmount(filled), IFluidHandler.FluidAction.EXECUTE);
            if (actual > 0)
            {
                tank.drain(actual, IFluidHandler.FluidAction.EXECUTE);
                setChanged();
            }
        }
    }

    /** 把储存中的物品尽量推给相邻容器（从 0 号槽循环，推不动即停） */
    private void pushTo(IItemHandler neighbor)
    {
        while (true)
        {
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (stack.isEmpty())
            {
                break;
            }
            ItemStack toPush = stack.copy();
            boolean anyMoved = false;
            for (int s = 0; s < neighbor.getSlots(); s++)
            {
                ItemStack leftover = neighbor.insertItem(s, toPush, false);
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

    // ---- capability：按面配置暴露物品/液体 ----

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_MINER_BE.get(),
                    (be, side) -> be.getSideCapability(side));
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Godofthings.GOD_MINER_BE.get(),
                    (be, side) -> be.getSideFluidCapability(side));
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, Godofthings.GOD_MINER_BE.get(),
                    (be, side) -> be);
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
        tag.putBoolean("AeEnabled", aeEnabled);
        tag.putBoolean("Running", running);
        tag.putInt("Radius", radius);
        tag.putInt("CurrentY", currentY);
        tag.putInt("ColumnIndex", columnIndex);
        tag.putInt("TickCounter", tickCounter);
        tag.put("Inventory", itemHandler.serializeNBT(provider));
        tag.put("AccelSlot", accelSlot.serializeNBT(provider));
        tag.put("Tank", tank.writeToNBT(provider, new CompoundTag()));
        tag.putInt("Efficiency", efficiencyLevel);
        tag.putInt("Fortune", fortuneLevel);
        tag.putBoolean("SilkTouch", silkTouch);
        tag.putByteArray("EmptyColumns", emptyColumns.toByteArray());
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
        this.aeEnabled = tag.contains("AeEnabled") ? tag.getBoolean("AeEnabled") : true;
        running = tag.getBoolean("Running");
        radius = tag.getInt("Radius");
        currentY = tag.getInt("CurrentY");
        columnIndex = tag.getInt("ColumnIndex");
        tickCounter = tag.getInt("TickCounter");
        if (tag.contains("Inventory"))
        {
            itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
        }
        if (tag.contains("AccelSlot"))
        {
            accelSlot.deserializeNBT(provider, tag.getCompound("AccelSlot"));
        }
        if (tag.contains("Tank"))
        {
            tank.readFromNBT(provider, tag.getCompound("Tank"));
        }
        efficiencyLevel = tag.getInt("Efficiency");
        fortuneLevel = tag.getInt("Fortune");
        silkTouch = tag.getBoolean("SilkTouch");
        emptyColumns = BitSet.valueOf(tag.getByteArray("EmptyColumns"));
        if (tag.contains("FaceModes"))
        {
            // 旧存档没有 FaceModes：保留字段默认值（六面全 OUTPUT），行为与旧版一致
            int[] modes = tag.getIntArray("FaceModes");
            System.arraycopy(modes, 0, faceModes, 0, Math.min(6, modes.length));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider)
    {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.put("Tank", tank.writeToNBT(provider, new CompoundTag()));
        tag.putIntArray("FaceModes", faceModes);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.handleUpdateTag(tag, provider);
        if (tag.contains("Tank"))
        {
            tank.readFromNBT(provider, tag.getCompound("Tank"));
        }
        if (tag.contains("FaceModes"))
        {
            System.arraycopy(tag.getIntArray("FaceModes"), 0, faceModes, 0, 6);
        }
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.god_miner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new GodMinerMenu(containerId, inventory, this);
    }
}
