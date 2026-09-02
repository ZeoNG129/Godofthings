package com.godofthings.block.entity;

import com.godofthings.Godofthings;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
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
public class GodMinerBlockEntity extends BlockEntity implements MenuProvider
{
    /** 矿机最大挖掘半径（格，方形半径），可经 godofthings-machines.toml 调整 */
    public static final int MAX_RADIUS = MachinesConfig.MINER_MAX_RADIUS.get();
    /** 每个 tick 最多处理的方块数，防止卡顿 */
    public static final int MAX_BLOCKS_PER_TICK = MachinesConfig.MINER_MAX_BLOCKS_PER_TICK.get();

    private final InfiniteItemHandler itemHandler = new InfiniteItemHandler();
    private final FluidTank tank = new FluidTank(Integer.MAX_VALUE);

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

    public GodMinerBlockEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.GOD_MINER_BE.get(), pos, state);
        itemHandler.setOnChange(this::setChanged);
    }

    public InfiniteItemHandler getItemHandler()
    {
        return itemHandler;
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
        int newRadius = Math.max(1, Math.min(MAX_RADIUS, value));
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
                pushOutput();
                pushFluid();
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
        pushOutput();
        pushFluid();
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
        super.setRemoved();
        // 运行中拆除矿机：释放本机强制加载过的区块，避免区块常驻内存泄漏
        releaseForcedChunks();
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
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
        while (y >= level.getMinBuildHeight() && mined < MAX_BLOCKS_PER_TICK)
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
        for (ItemStack drop : drops)
        {
            if (!drop.isEmpty())
            {
                insertDrop(drop);
            }
        }
    }

    /** 计算掉落物：用假镐模拟，应用时运/精准采集 */
    private List<ItemStack> getDrops(BlockState state, BlockPos pos)
    {
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (silkTouch)
        {
            tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        }
        if (fortuneLevel > 0)
        {
            tool.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.FORTUNE), fortuneLevel);
        }
        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, null, null, tool);
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

    /** 六面全部自动输出到相邻容器 */
    private void pushOutput()
    {
        for (Direction dir : Direction.values())
        {
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
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (handler != null)
            {
                pushTo(handler);
            }
        }
    }

    /** 六面自动输出液体到相邻可装液体的容器 */
    private void pushFluid()
    {
        if (tank.getFluidAmount() <= 0)
        {
            return;
        }
        for (Direction dir : Direction.values())
        {
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
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
            if (handler == null)
            {
                continue;
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

    // ---- capability：任何面都能取走物品/液体 ----

    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityRegistration
    {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event)
        {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Godofthings.GOD_MINER_BE.get(),
                    (be, side) -> be.itemHandler);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Godofthings.GOD_MINER_BE.get(),
                    (be, side) -> be.tank);
        }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.saveAdditional(tag, provider);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider)
    {
        super.loadAdditional(tag, provider);
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
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider)
    {
        CompoundTag tag = super.getUpdateTag(provider);
        tag.put("Tank", tank.writeToNBT(provider, new CompoundTag()));
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
    }

    // ---- 液体显示名（全中文） ----

    public static String fluidName(Fluid fluid)
    {
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER)
        {
            return "水";
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA)
        {
            return "岩浆";
        }
        return "液体";
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
