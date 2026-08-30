package com.godofthings.generator;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FE 能量发电机实体。
 * <p>
 * 负责：发电量自动增长、能量存储、上方实体（玩家/生物）全部槽位充电、
 * 上方容器内物品充电、六面输电（可逐面禁用）、充电槽物品充电、
 * 指定物品加速增长（增长量变为当前发电量的 1%）以及无线充电。
 * 无线充电包含两部分：给无线范围内生物携带的可充电物品充电、隔空向范围内机器输电；
 * 无线充电优先于六面有线输电，避免电量被有线先排空导致无线永远不生效。
 * 无线充电与输电面等参数均为每台发电机独立保存，可在 GUI 中修改。
 * 移植自 auto-resource（LGPL-3.0）。
 */
public class EnergyGeneratorEntity extends BlockEntity implements ICapabilityProvider, MenuProvider
{
    private final LazyOptional<EnergyConnection> fecOptional = LazyOptional.of(() -> new EnergyConnection(this));

    // 核心数据
    public long output;
    public long energy = 0;
    public long tickCount = 0;
    /** 下次增长的发电量（同时用于增长时实际增量） */
    public long nextIncrease = 0;

    // 无线充电开关（逐台保存）
    public boolean wirelessOn = false;
    /** 无线扫描游标：记录上次扫描到的线性位置（按全部方块展平），下次从该位置继续 */
    public long scanCursor = 0;
    /** 已记录的支持电量接收的位置及其接收面（分片扫描时更新，传输时遍历） */
    public final Map<BlockPos, Direction> wirelessTargets = new HashMap<>();

    // 无线充电参数（逐台保存，可在 GUI 修改）
    public int wirelessInterval = 5;
    public int wirelessRange = 1;
    /** 重复传电次数，对相邻输电和无线输电都生效 */
    public int transferRepeat = 1;

    // 六面输电开关（逐台保存，可在 GUI 修改，默认全启用）
    public boolean transferDown = true;
    public boolean transferUp = true;
    public boolean transferNorth = true;
    public boolean transferSouth = true;
    public boolean transferWest = true;
    public boolean transferEast = true;

    // 加速增长槽位（放入配置指定物品后增长量变为当前发电量的 1%），只能放 1 个
    public final ItemStackHandler starSlot = new ItemStackHandler(1)
    {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack)
        {
            return stack.is(EnergyGenConfig.STAR_ITEM);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    // 充电槽位（可放入可充电物品为其充电），每次充电 1 个
    public final ItemStackHandler chargeSlot = new ItemStackHandler(1)
    {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack)
        {
            return stack.getCapability(ForgeCapabilities.ENERGY).map(IEnergyStorage::canReceive).orElse(false);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    // 六面输电轮询索引
    private int findIndex = 0;

    public EnergyGeneratorEntity(BlockPos pos, BlockState state)
    {
        super(Godofthings.ENERGY_GENERATOR_BE.get(), pos, state);
        this.output = EnergyGenConfig.MIN;
    }

    /**
     * 服务端每 tick 调用（由方块的 ticker 触发）
     */
    public void serverTick()
    {
        Level level = getLevel();
        if (level == null || level.isClientSide)
        {
            return;
        }
        // 增长逻辑：先刷新下次增长量，到达间隔后应用
        tickCount = EnergyGenTool.suit(tickCount + 1);
        updateNextIncrease();
        if (tickCount / 20 >= EnergyGenConfig.SECOND)
        {
            tickCount = 0;
            output = Math.min(EnergyGenConfig.MAX, EnergyGenTool.suit(output + nextIncrease));
        }
        // 发电
        energy = EnergyGenTool.suit(energy + output);

        // 充电槽充电
        chargeChargeSlot();
        // 上方实体充电（玩家物品栏/存储栏/装备栏全部覆盖）
        chargeEntitiesAbove();
        // 上方容器充电
        chargeContainersAbove();
        // 无线充电（优先于有线输电，避免电量被六面输电先排空）
        if (wirelessOn)
        {
            wirelessChargeEntities();
            wirelessTick();
        }
        // 六面输电
        outputToSides();
        setChanged();
    }

    /**
     * 计算下一次增长的发电量并保存到 nextIncrease（原信标功能已由加速槽代替）
     */
    private void updateNextIncrease()
    {
        long increase = EnergyGenConfig.STEP;
        if (!starSlot.getStackInSlot(0).isEmpty())
        {
            // 放入指定物品后，增长的发电量变为当前发电量的 1%（至少 1，避免低产量时停止增长）
            increase = Math.max(1, output / 100);
        }
        nextIncrease = increase;
    }

    /**
     * 给充电槽中的物品充电
     */
    private void chargeChargeSlot()
    {
        if (energy <= 0)
        {
            return;
        }
        ItemStack stack = chargeSlot.getStackInSlot(0);
        if (stack.isEmpty())
        {
            return;
        }
        stack.getCapability(ForgeCapabilities.ENERGY).resolve().filter(IEnergyStorage::canReceive).ifPresent(storage -> {
            int maxOutput = EnergyGenTool.suitInt(energy);
            int result = storage.receiveEnergy(maxOutput, false);
            if (result < 0)
            {
                result = 0;
            }
            if (result > maxOutput)
            {
                result = maxOutput;
            }
            if (result > 0)
            {
                energy -= result;
                chargeSlot.setStackInSlot(0, stack);
            }
        });
    }

    /**
     * 给站在机器上方实体的所有槽位中可充电物品充电（玩家物品栏/存储栏/装备栏均覆盖）
     */
    private void chargeEntitiesAbove()
    {
        Level level = getLevel();
        if (level == null)
        {
            return;
        }
        List<LivingEntity> entityList = level.getEntitiesOfClass(LivingEntity.class, new AABB(getBlockPos().relative(Direction.UP)));
        for (LivingEntity livingEntity : entityList)
        {
            Iterable<ItemStack> slots = livingEntity.getAllSlots();
            for (ItemStack stack : slots)
            {
                if (energy <= 0)
                {
                    return;
                }
                stack.getCapability(ForgeCapabilities.ENERGY).resolve().filter(IEnergyStorage::canReceive).ifPresent(storage -> {
                    int maxOutput = EnergyGenTool.suitInt(energy);
                    int result = storage.receiveEnergy(maxOutput, false);
                    if (result < 0)
                    {
                        result = 0;
                    }
                    if (result > maxOutput)
                    {
                        result = maxOutput;
                    }
                    if (result > 0)
                    {
                        energy -= result;
                    }
                });
            }
        }
    }

    /**
     * 给机器上方容器中的可充电物品充电（箱子、漏斗等带物品栏的方块实体）
     */
    private void chargeContainersAbove()
    {
        Level level = getLevel();
        if (level == null || energy <= 0)
        {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(getBlockPos().relative(Direction.UP));
        if (blockEntity == null)
        {
            return;
        }
        blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++)
            {
                if (energy <= 0)
                {
                    return;
                }
                ItemStack stack = handler.getStackInSlot(i);
                if (stack.isEmpty())
                {
                    continue;
                }
                stack.getCapability(ForgeCapabilities.ENERGY).resolve().filter(IEnergyStorage::canReceive).ifPresent(storage -> {
                    int maxOutput = EnergyGenTool.suitInt(energy);
                    int result = storage.receiveEnergy(maxOutput, false);
                    if (result < 0)
                    {
                        result = 0;
                    }
                    if (result > maxOutput)
                    {
                        result = maxOutput;
                    }
                    if (result > 0)
                    {
                        energy -= result;
                        // 修改了容器内物品的能量数据，标记容器已改变以便落盘/同步
                        blockEntity.setChanged();
                    }
                });
            }
        });
    }

    /**
     * 六面输电（跳过被禁用的面），轮询索引实现负载均衡；重复传电次数生效
     */
    private void outputToSides()
    {
        Level level = getLevel();
        if (level == null)
        {
            return;
        }
        Direction[] directions = Direction.values();
        for (int rep = 0; rep < transferRepeat && energy > 0; rep++)
        {
            for (int i = 0; i < directions.length; i++)
            {
                if (energy <= 0)
                {
                    return;
                }
                findIndex = (findIndex + 1) % directions.length;
                Direction direction = directions[findIndex];
                if (!isTransferEnabled(direction))
                {
                    continue;
                }
                BlockPos pos = getBlockPos().relative(direction);
                BlockEntity entity = level.getBlockEntity(pos);
                if (entity == null)
                {
                    continue;
                }
                entity.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).resolve().filter(IEnergyStorage::canReceive).ifPresent(storage -> {
                    int maxOutput = EnergyGenTool.suitInt(energy);
                    int result = storage.receiveEnergy(maxOutput, false);
                    if (result < 0)
                    {
                        result = 0;
                    }
                    if (result > maxOutput)
                    {
                        result = maxOutput;
                    }
                    if (result > 0)
                    {
                        energy -= result;
                    }
                });
            }
        }
    }

    /**
     * 无线充电：给无线范围内生物（玩家/怪物）携带的所有可充电物品充电
     */
    private void wirelessChargeEntities()
    {
        Level level = getLevel();
        if (level == null || energy <= 0)
        {
            return;
        }
        EnergyGenTool.chargeEntitiesInWirelessRange(level, getBlockPos(), wirelessRange,
                () -> energy, consumed -> energy -= consumed);
    }

    /**
     * 无线充电每 tick 处理：
     * 每 tick 扫描一片（整个区域按全部方块线性均分为 wirelessInterval*20 片，游标记录上次位置下次继续），
     * 然后按重复传电次数遍历已记录位置尝试输电。
     */
    private void wirelessTick()
    {
        scanWirelessSlice();
        wirelessTransfer();
    }

    /**
     * 扫描当前分片：把整个扫描区域按全部方块线性均分为 wirelessInterval*20 片，每 tick 扫一片。
     */
    private void scanWirelessSlice()
    {
        Level level = getLevel();
        if (level == null)
        {
            return;
        }
        int range = EnergyGenTool.normalizeWirelessRange(wirelessRange);
        int half = range >> 1;
        BlockPos pos = getBlockPos();
        int originX = pos.getX() >> 4 << 4;
        int originZ = pos.getZ() >> 4 << 4;
        int minX = originX - half * 16;
        int minZ = originZ - half * 16;
        int width = range * 16;
        int minY = level.getMinBuildHeight();
        long layerSize = (long) width * width;
        long volume = layerSize * level.getHeight();

        // 缩小范围后清理超出当前区域的目标（无线目标按区块列判断）
        wirelessTargets.keySet().removeIf(bp -> {
            int bx = bp.getX() >> 4;
            int bz = bp.getZ() >> 4;
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            return Math.abs(bx - cx) > half || Math.abs(bz - cz) > half;
        });

        long slices = Math.max(1L, (long) Math.max(1, wirelessInterval) * 20);
        long sliceSize = Math.max(1L, (volume + slices - 1) / slices);
        long start = scanCursor;
        long end = Math.min(volume, start + sliceSize);
        scanLinearRange(level, minX, minZ, width, minY, layerSize, start, end);
        scanCursor = end >= volume ? 0 : end;
    }

    /**
     * 扫描线性索引落在 [from, to) 内的方块实体并刷新无线目标
     */
    private void scanLinearRange(Level level, int minX, int minZ, int width, int minY, long layerSize, long from, long to)
    {
        if (from >= to)
        {
            return;
        }
        BlockPos pos = getBlockPos();
        int cMinX = minX >> 4;
        int cMaxX = (minX + width - 1) >> 4;
        int cMinZ = minZ >> 4;
        int cMaxZ = (minZ + width - 1) >> 4;
        for (int cx = cMinX; cx <= cMaxX; cx++)
        {
            for (int cz = cMinZ; cz <= cMaxZ; cz++)
            {
                // 只处理已加载的区块，避免强制生成区块
                if (!level.isLoaded(new BlockPos(cx << 4, pos.getY(), cz << 4)))
                {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                if (chunk == null)
                {
                    continue;
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet())
                {
                    BlockPos bp = entry.getKey();
                    if (bp.equals(worldPosition))
                    {
                        continue;
                    }
                    long idx = (bp.getX() - minX) + (long) (bp.getZ() - minZ) * width + (long) (bp.getY() - minY) * layerSize;
                    if (idx < from || idx >= to)
                    {
                        continue;
                    }
                    refreshWirelessTarget(bp, entry.getValue());
                }
            }
        }
    }

    /**
     * 扫描目标的所有面，找到第一个可输入能量的面截止并缓存该面；没有可接收面则移除旧记录
     */
    private void refreshWirelessTarget(BlockPos bp, BlockEntity target)
    {
        for (Direction dir : Direction.values())
        {
            if (target.getCapability(ForgeCapabilities.ENERGY, dir).resolve().map(IEnergyStorage::canReceive).orElse(false))
            {
                wirelessTargets.put(bp.immutable(), dir);
                return;
            }
        }
        wirelessTargets.remove(bp);
    }

    /**
     * 每 tick 遍历已记录目标，按重复传电次数循环向其中输入电量（使用扫描时缓存的面）
     */
    private void wirelessTransfer()
    {
        Level level = getLevel();
        if (level == null || energy <= 0)
        {
            return;
        }
        int repeat = Math.max(1, transferRepeat);
        for (int rep = 0; rep < repeat && energy > 0; rep++)
        {
            for (Map.Entry<BlockPos, Direction> entry : wirelessTargets.entrySet())
            {
                if (energy <= 0)
                {
                    return;
                }
                BlockPos targetPos = entry.getKey();
                if (!level.isLoaded(targetPos))
                {
                    continue;
                }
                BlockEntity target = level.getBlockEntity(targetPos);
                if (target == null || target == this)
                {
                    continue;
                }
                target.getCapability(ForgeCapabilities.ENERGY, entry.getValue()).resolve().filter(IEnergyStorage::canReceive).ifPresent(storage -> {
                    int maxOutput = EnergyGenTool.suitInt(energy);
                    int result = storage.receiveEnergy(maxOutput, false);
                    if (result < 0)
                    {
                        result = 0;
                    }
                    if (result > maxOutput)
                    {
                        result = maxOutput;
                    }
                    if (result > 0)
                    {
                        energy -= result;
                    }
                });
            }
        }
    }

    /**
     * 指定面是否允许输电
     */
    public boolean isTransferEnabled(Direction direction)
    {
        return switch (direction)
        {
            case DOWN -> transferDown;
            case UP -> transferUp;
            case NORTH -> transferNorth;
            case SOUTH -> transferSouth;
            case WEST -> transferWest;
            case EAST -> transferEast;
        };
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction)
    {
        return capability == ForgeCapabilities.ENERGY ? fecOptional.cast() : super.getCapability(capability, direction);
    }

    @Override
    @Nonnull
    public Component getDisplayName()
    {
        return Component.translatable("block.godofthings.energy_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inv, @Nonnull Player player)
    {
        return new EnergyGeneratorMenu(id, inv, worldPosition);
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt)
    {
        super.saveAdditional(nbt);
        nbt.putLong("output", output);
        nbt.putLong("energy", energy);
        nbt.putLong("tickCount", tickCount);
        nbt.putLong("nextIncrease", nextIncrease);
        nbt.putBoolean("wirelessOn", wirelessOn);
        nbt.putInt("wirelessInterval", wirelessInterval);
        nbt.putInt("wirelessRange", wirelessRange);
        nbt.putInt("transferRepeat", transferRepeat);
        nbt.putBoolean("transferDown", transferDown);
        nbt.putBoolean("transferUp", transferUp);
        nbt.putBoolean("transferNorth", transferNorth);
        nbt.putBoolean("transferSouth", transferSouth);
        nbt.putBoolean("transferWest", transferWest);
        nbt.putBoolean("transferEast", transferEast);
        nbt.put("starSlot", starSlot.serializeNBT());
        nbt.put("chargeSlot", chargeSlot.serializeNBT());
    }

    @Override
    public void load(@Nonnull CompoundTag nbt)
    {
        super.load(nbt);
        if (nbt.contains("output", Tag.TAG_LONG))
        {
            output = EnergyGenTool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("energy", Tag.TAG_LONG))
        {
            energy = EnergyGenTool.suit(nbt.getLong("energy"));
        }
        if (nbt.contains("tickCount", Tag.TAG_LONG))
        {
            tickCount = EnergyGenTool.suit(nbt.getLong("tickCount"));
        }
        if (nbt.contains("nextIncrease", Tag.TAG_LONG))
        {
            nextIncrease = EnergyGenTool.suit(nbt.getLong("nextIncrease"));
        }
        else if (nbt.contains("beaconIncrease", Tag.TAG_LONG))
        {
            nextIncrease = EnergyGenTool.suit(nbt.getLong("beaconIncrease"));
        }
        if (nbt.contains("wirelessOn", Tag.TAG_BYTE))
        {
            wirelessOn = nbt.getBoolean("wirelessOn");
        }
        if (nbt.contains("wirelessInterval", Tag.TAG_INT))
        {
            wirelessInterval = Math.max(1, nbt.getInt("wirelessInterval"));
        }
        if (nbt.contains("wirelessRange", Tag.TAG_INT))
        {
            wirelessRange = Math.max(1, nbt.getInt("wirelessRange"));
        }
        if (nbt.contains("transferRepeat", Tag.TAG_INT))
        {
            transferRepeat = Math.max(1, nbt.getInt("transferRepeat"));
        }
        if (nbt.contains("transferDown", Tag.TAG_BYTE))
        {
            transferDown = nbt.getBoolean("transferDown");
        }
        if (nbt.contains("transferUp", Tag.TAG_BYTE))
        {
            transferUp = nbt.getBoolean("transferUp");
        }
        if (nbt.contains("transferNorth", Tag.TAG_BYTE))
        {
            transferNorth = nbt.getBoolean("transferNorth");
        }
        if (nbt.contains("transferSouth", Tag.TAG_BYTE))
        {
            transferSouth = nbt.getBoolean("transferSouth");
        }
        if (nbt.contains("transferWest", Tag.TAG_BYTE))
        {
            transferWest = nbt.getBoolean("transferWest");
        }
        if (nbt.contains("transferEast", Tag.TAG_BYTE))
        {
            transferEast = nbt.getBoolean("transferEast");
        }
        if (nbt.contains("starSlot", Tag.TAG_COMPOUND))
        {
            starSlot.deserializeNBT(nbt.getCompound("starSlot"));
        }
        if (nbt.contains("chargeSlot", Tag.TAG_COMPOUND))
        {
            chargeSlot.deserializeNBT(nbt.getCompound("chargeSlot"));
        }
    }
}
