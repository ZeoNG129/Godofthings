package com.godofthings.overloadedinterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;

import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.blockentity.misc.InterfaceBlockEntity;
import appeng.helpers.InterfaceLogic;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.parts.automation.StackWorldBehaviors;
import appeng.util.inv.AppEngInternalInventory;

/**
 * 过载接口方块实体（移植自 AE2LT 的 OverloadedInterfaceBlockEntity）。
 *
 * <p>相对原版的增强：</p>
 * <ul>
 *   <li><b>36 槽</b>配置 / 存储（原版 9 槽）；</li>
 *   <li><b>无线模式</b>：把任意位置的 AE 设备（或普通容器）当作 IO 目标，不看贴邻；</li>
 *   <li><b>导出 OFF/AUTO</b>：把配置里的物资主动推进目标机器；</li>
 *   <li><b>导入 OFF/AUTO/EJECT</b>：AUTO 主动从目标抽回 ME；
 *       EJECT 把接口当作目标机器的输出仓（机器自己往里塞，接口自动回网）；</li>
 *   <li><b>IO 速度 普通/快速</b>：快速模式缩短冷却，提高吞吐。</li>
 * </ul>
 *
 * <p>裁剪说明（沿用 7.3.0 供应器口径）：上游的 FE 能量计费、频率绑定、
 * Thunderbolt 批处理 CPU、AdvancedAE 定向派发、内存卡导入导出、性能探针
 * 均未移植；核心 IO 语义（配置/导出/导入/弹出/速度）完整保留。</p>
 */
public class OverloadedInterfaceBlockEntity extends InterfaceBlockEntity
{
    /** 配置 / 存储槽数（原版 9 槽，过载版 36 槽）。 */
    public static final int SLOT_COUNT = 36;
    /** 无线连接上限（照抄上游）。 */
    public static final int MAX_WIRELESS_CONNECTIONS = 1024;

    private static final String TAG_INTERFACE_MODE = "InterfaceMode";
    private static final String TAG_EXPORT_MODE = "ExportMode";
    private static final String TAG_IMPORT_MODE = "ImportMode";
    private static final String TAG_IO_SPEED_MODE = "IOSpeedMode";
    private static final String TAG_CONNECTIONS = "WirelessConnections";
    private static final String TAG_UNLIMITED_SLOTS = "UnlimitedSlots";
    private static final String TAG_FILTER_INV = "FilterInv";

    /** 普通模式一次 IO 的最短间隔（tick）。 */
    private static final int NORMAL_INTERVAL = 10;
    /** 快速模式一次 IO 的最短间隔（tick）——每 tick 都跑。 */
    private static final int FAST_INTERVAL = 1;
    /** 单次向外部目标投放的上限（对齐上游：不设人工瓶颈）。 */
    private static final long TRANSFER_LIMIT = Long.MAX_VALUE;

    public enum InterfaceMode { NORMAL, WIRELESS }

    public enum IOSpeedMode { NORMAL, FAST }

    public enum ExportMode { OFF, AUTO }

    public enum ImportMode { OFF, AUTO, EJECT }

    /** 一条无线连接：目标维度 + 坐标 + 绑定面。 */
    public record WirelessConnection(ResourceKey<Level> dimension, BlockPos pos, Direction boundFace)
    {
        private static final String TAG_DIM = "Dim";
        private static final String TAG_POS = "Pos";
        private static final String TAG_FACE = "Face";

        public CompoundTag toTag()
        {
            var tag = new CompoundTag();
            tag.putString(TAG_DIM, this.dimension.location().toString());
            tag.putLong(TAG_POS, this.pos.asLong());
            tag.putInt(TAG_FACE, this.boundFace.get3DDataValue());
            return tag;
        }

        public static WirelessConnection fromTag(CompoundTag tag)
        {
            var dim = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(tag.getString(TAG_DIM)));
            return new WirelessConnection(dim, BlockPos.of(tag.getLong(TAG_POS)),
                    Direction.from3DDataValue(tag.getInt(TAG_FACE)));
        }
    }

    private InterfaceMode interfaceMode = InterfaceMode.NORMAL;
    private IOSpeedMode ioSpeedMode = IOSpeedMode.NORMAL;
    private ExportMode exportMode = ExportMode.OFF;
    private ImportMode importMode = ImportMode.OFF;

    private final boolean[] unlimitedSlots = new boolean[SLOT_COUNT];
    private final List<WirelessConnection> connections = new ArrayList<>();

    private final AppEngInternalInventory filterInv =
            new AppEngInternalInventory(null, 1, 1);
    private final IActionSource machineSource = IActionSource.ofMachine(this);

    private long nextIoWorkTick;

    public OverloadedInterfaceBlockEntity(BlockPos pos, BlockState state)
    {
        this(OverloadedInterfaceRegistry.OVERLOADED_INTERFACE_BE.get(), pos, state);
    }

    protected OverloadedInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Override
    protected InterfaceLogic createLogic()
    {
        return new OverloadedInterfaceLogic(this.getMainNode(), this,
                this.getItemFromBlockEntity().asItem(), SLOT_COUNT);
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator)
    {
        MenuOpener.open(OverloadedInterfaceMenu.TYPE, player, locator);
    }

    @Override
    public void returnToMainMenu(Player player, appeng.menu.ISubMenu subMenu)
    {
        MenuOpener.returnTo(OverloadedInterfaceMenu.TYPE, player, subMenu.getLocator());
    }

    @Override
    public AECableType getCableConnectionType(Direction dir)
    {
        return AECableType.DENSE_SMART;
    }

    // ==================== 状态读写 ====================

    public InterfaceMode getInterfaceMode()
    {
        return this.interfaceMode;
    }

    public void setInterfaceMode(InterfaceMode mode)
    {
        if (this.interfaceMode == mode)
        {
            return;
        }
        this.interfaceMode = mode;
        this.saveChanges();
        this.markForClientUpdate();
    }

    public IOSpeedMode getIOSpeedMode()
    {
        return this.ioSpeedMode;
    }

    public void setIOSpeedMode(IOSpeedMode mode)
    {
        if (this.ioSpeedMode == mode)
        {
            return;
        }
        this.ioSpeedMode = mode;
        this.saveChanges();
        this.markForClientUpdate();
    }

    public ExportMode getExportMode()
    {
        return this.exportMode;
    }

    public void setExportMode(ExportMode mode)
    {
        if (this.exportMode == mode)
        {
            return;
        }
        this.exportMode = mode;
        this.saveChanges();
        this.markForClientUpdate();
    }

    public ImportMode getImportMode()
    {
        return this.importMode;
    }

    public void setImportMode(ImportMode mode)
    {
        if (this.importMode == mode)
        {
            return;
        }
        this.importMode = mode;
        this.saveChanges();
        this.markForClientUpdate();
    }

    public boolean isSlotUnlimited(int slot)
    {
        return slot >= 0 && slot < SLOT_COUNT && this.unlimitedSlots[slot];
    }

    public void setSlotUnlimited(int slot, boolean unlimited)
    {
        if (slot < 0 || slot >= SLOT_COUNT || this.unlimitedSlots[slot] == unlimited)
        {
            return;
        }
        this.unlimitedSlots[slot] = unlimited;
        this.saveChanges();
    }

    /** 过滤组件槽（0 = 未装）。装了以后插入只接受过滤里的物资。 */
    public AppEngInternalInventory getFilterInv()
    {
        return this.filterInv;
    }

    public List<WirelessConnection> getConnections()
    {
        return Collections.unmodifiableList(this.connections);
    }

    public boolean addOrUpdateConnection(WirelessConnection conn)
    {
        int index = this.indexOfConnection(conn.dimension(), conn.pos());
        if (index >= 0)
        {
            this.connections.set(index, conn);
        }
        else
        {
            if (this.connections.size() >= MAX_WIRELESS_CONNECTIONS)
            {
                return false;
            }
            this.connections.add(conn);
        }
        this.saveChanges();
        this.markForClientUpdate();
        return true;
    }

    public boolean removeConnection(ResourceKey<Level> dim, BlockPos pos)
    {
        int index = this.indexOfConnection(dim, pos);
        if (index < 0)
        {
            return false;
        }
        this.connections.remove(index);
        this.saveChanges();
        this.markForClientUpdate();
        return true;
    }

    private int indexOfConnection(ResourceKey<Level> dim, BlockPos pos)
    {
        for (int i = 0; i < this.connections.size(); i++)
        {
            var c = this.connections.get(i);
            if (c.dimension().equals(dim) && c.pos().equals(pos))
            {
                return i;
            }
        }
        return -1;
    }

    /** 配置变化后让 IO 立刻重新跑一轮。 */
    public void onGridIoConfigChanged()
    {
        this.nextIoWorkTick = 0L;
    }

    // ==================== IO 主循环 ====================

    /** 由逻辑层的 ticker 调用：是否还有活可干。 */
    public boolean hasGridItemIoWork()
    {
        if (this.exportMode == ExportMode.AUTO)
        {
            return true;
        }
        if (this.importMode == ImportMode.AUTO)
        {
            return true;
        }
        // EJECT 是被动接收（机器往里塞），不需要主动 tick
        return false;
    }

    /** 由逻辑层的 ticker 调用：跑一轮导出 / 导入。 */
    public void tickGridItemIo()
    {
        if (!(this.level instanceof ServerLevel serverLevel))
        {
            return;
        }
        var grid = this.getMainNode().getGrid();
        if (grid == null || !this.getMainNode().isActive())
        {
            return;
        }
        long now = serverLevel.getGameTime();
        if (now < this.nextIoWorkTick)
        {
            return;
        }
        int interval = this.ioSpeedMode == IOSpeedMode.FAST ? FAST_INTERVAL : NORMAL_INTERVAL;
        this.nextIoWorkTick = now + interval;

        var meStorage = grid.getStorageService().getInventory();
        for (var target : this.resolveTargets(serverLevel))
        {
            if (this.importMode == ImportMode.AUTO)
            {
                this.doImport(target, meStorage);
            }
            if (this.exportMode == ExportMode.AUTO)
            {
                this.doExport(target, meStorage);
            }
        }
    }

    /** 一次 IO 目标：位置 + 面向 + 已解析的外部存储。 */
    private record IoTarget(ServerLevel level, BlockPos pos, Direction side, MEStorage storage)
    {
    }

    /**
     * 解析当前所有 IO 目标。
     *
     * <p>普通模式：贴邻六个面（受 PUSH_DIRECTION 限制）；
     * 无线模式：连接列表里的远程机器，用 boundFace 直接作为 capability 面
     * （与 AE2 PatternProviderTarget 的语义一致）。</p>
     */
    private List<IoTarget> resolveTargets(ServerLevel hostLevel)
    {
        var result = new ArrayList<IoTarget>();
        if (this.interfaceMode == InterfaceMode.WIRELESS)
        {
            for (var conn : this.connections)
            {
                var targetLevel = hostLevel.getServer().getLevel(conn.dimension());
                if (targetLevel == null || !targetLevel.isLoaded(conn.pos()))
                {
                    continue;
                }
                var storage = this.wrapStorage(targetLevel, conn.pos(), conn.boundFace());
                if (storage != null)
                {
                    result.add(new IoTarget(targetLevel, conn.pos(), conn.boundFace(), storage));
                }
            }
            return result;
        }

        // 普通模式：贴邻六个面，方向受 PUSH_DIRECTION 约束
        var pushDir = this.getPushDirection();
        for (var dir : Direction.values())
        {
            if (pushDir != null && dir != pushDir)
            {
                continue;
            }
            var targetPos = this.worldPosition.relative(dir);
            if (!hostLevel.isLoaded(targetPos))
            {
                continue;
            }
            var storage = this.wrapStorage(hostLevel, targetPos, dir.getOpposite());
            if (storage != null)
            {
                result.add(new IoTarget(hostLevel, targetPos, dir.getOpposite(), storage));
            }
        }
        return result;
    }

    /**
     * 当前推拉方向约束（来自方块状态的 PUSH_DIRECTION）。
     * 返回 null 表示 ALL（六个面都允许）；否则只允许该方向。
     */
    @org.jetbrains.annotations.Nullable
    private Direction getPushDirection()
    {
        var state = this.getBlockState();
        if (!state.hasProperty(appeng.block.crafting.PatternProviderBlock.PUSH_DIRECTION))
        {
            return null;
        }
        var push = state.getValue(appeng.block.crafting.PatternProviderBlock.PUSH_DIRECTION);
        if (push == appeng.block.crafting.PushDirection.ALL)
        {
            return null;
        }
        return push.getDirection();
    }

    /** 把目标机器解析成 ME 存储（物品 / 流体能力桥）。 */
    private MEStorage wrapStorage(ServerLevel level, BlockPos pos, Direction side)
    {
        var strategies = StackWorldBehaviors.createExternalStorageStrategies(level, pos, side);
        var wrappers = new java.util.IdentityHashMap<AEKeyType, MEStorage>(2);
        for (var entry : strategies.entrySet())
        {
            var wrapper = entry.getValue().createWrapper(false, () -> {
            });
            if (wrapper != null)
            {
                wrappers.put(entry.getKey(), wrapper);
            }
        }
        return wrappers.isEmpty() ? null : new appeng.me.storage.CompositeStorage(wrappers);
    }

    /**
     * 导出：把配置里指定的物资从 ME 网络推进目标，直到达到每槽配置量。
     * 未标记「无限」的槽只补到配置数量为止；无限的槽有多少推多少（受 TRANSFER_LIMIT）。
     */
    private void doExport(IoTarget target, MEStorage meStorage)
    {
        var config = this.getInterfaceLogic().getConfig();
        long budget = TRANSFER_LIMIT;
        for (int slot = 0; slot < SLOT_COUNT && budget > 0; slot++)
        {
            var key = config.getKey(slot);
            if (key == null)
            {
                continue;
            }
            long configured = config.getAmount(slot);
            if (configured <= 0)
            {
                continue;
            }
            // 照抄上游：无限槽目标量 = Long.MAX_VALUE（持续补货），
            // 普通槽 = 配置量 - 目标已有量
            long maxAmount = this.unlimitedSlots[slot] ? Long.MAX_VALUE : configured;
            var existing = new KeyCounter();
            target.storage().getAvailableStacks(existing);
            long present = existing.get(key);
            long wanted = Math.max(0L, maxAmount - present);
            if (wanted <= 0)
            {
                continue;
            }
            long extracted = meStorage.extract(key, Math.min(wanted, budget),
                    Actionable.SIMULATE, this.machineSource);
            if (extracted <= 0)
            {
                continue;
            }
            long accepted = target.storage().insert(key, extracted, Actionable.SIMULATE,
                    this.machineSource);
            if (accepted <= 0)
            {
                continue;
            }
            long taken = meStorage.extract(key, accepted, Actionable.MODULATE, this.machineSource);
            if (taken <= 0)
            {
                continue;
            }
            long inserted = target.storage().insert(key, taken, Actionable.MODULATE,
                    this.machineSource);
            if (inserted < taken)
            {
                // 目标只收了一部分：把剩下的塞回网络，绝不凭空丢失
                meStorage.insert(key, taken - inserted, Actionable.MODULATE, this.machineSource);
            }
            budget -= inserted;
        }
    }

    /** 导入：把目标里的物资抽回 ME 网络。装了过滤组件时只收过滤里的种类。 */
    private void doImport(IoTarget target, MEStorage meStorage)
    {
        var available = new KeyCounter();
        target.storage().getAvailableStacks(available);
        long budget = TRANSFER_LIMIT;
        for (var entry : available)
        {
            if (budget <= 0)
            {
                break;
            }
            var key = entry.getKey();
            long amount = entry.getLongValue();
            if (amount <= 0 || !this.isInsertAllowedByFilter(key))
            {
                continue;
            }
            long accepted = meStorage.insert(key, Math.min(amount, budget), Actionable.SIMULATE,
                    this.machineSource);
            if (accepted <= 0)
            {
                continue;
            }
            long taken = target.storage().extract(key, accepted, Actionable.MODULATE,
                    this.machineSource);
            if (taken <= 0)
            {
                continue;
            }
            long inserted = meStorage.insert(key, taken, Actionable.MODULATE, this.machineSource);
            if (inserted < taken)
            {
                // 网络只吃了一部分：把剩下的退回目标
                target.storage().insert(key, taken - inserted, Actionable.MODULATE,
                        this.machineSource);
            }
            budget -= inserted;
        }
    }

    /** 过滤组件判定：没装过滤组件时全放行。 */
    public boolean isInsertAllowedByFilter(AEKey key)
    {
        var filter = this.filterInv.getStackInSlot(0);
        if (filter.isEmpty())
        {
            return true;
        }
        // 过滤组件用 ConfigInventory 记录允许的种类；这里退化为「样本只挡同类以外」
        var sample = GenericStack.fromItemStack(filter);
        return sample == null || sample.what().equals(key);
    }

    // ==================== 弹出（EJECT）====================

    /**
     * EJECT 模式的输出仓视图：目标机器把产物塞进这里，接口自动灌进 ME 网络。
     *
     * <p>这是对上游「幽灵输出方块」方案的等价简化：直接把本接口暴露成一个
     * insert-only 的 {@link MEStorage}，挂到相邻面 / 无线连接的对应面上，
     * 机器只要支持在那一面输出就会主动塞进来。</p>
     */
    public MEStorage getEjectStorage()
    {
        if (this.importMode != ImportMode.EJECT)
        {
            return null;
        }
        return new EjectStorage(this);
    }

    /** 只进不出、直接灌进 ME 网络的存储视图。 */
    private record EjectStorage(OverloadedInterfaceBlockEntity host) implements MEStorage
    {
        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source)
        {
            if (what == null || amount <= 0 || !host.isInsertAllowedByFilter(what))
            {
                return 0;
            }
            var grid = host.getMainNode().getGrid();
            if (grid == null)
            {
                return 0;
            }
            var me = grid.getStorageService().getInventory();
            if (mode == Actionable.SIMULATE)
            {
                return me.insert(what, amount, Actionable.SIMULATE, host.machineSource);
            }
            long inserted = me.insert(what, amount, Actionable.MODULATE, host.machineSource);
            if (inserted > 0)
            {
                host.saveChanges();
            }
            return inserted;
        }

        @Override
        public net.minecraft.network.chat.Component getDescription()
        {
            return net.minecraft.network.chat.Component.translatable(
                    "gui.godofthings.overloaded_interface.eject_storage");
        }
    }

    // ==================== NBT / 同步 ====================

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.putString(TAG_INTERFACE_MODE, this.interfaceMode.name());
        tag.putString(TAG_EXPORT_MODE, this.exportMode.name());
        tag.putString(TAG_IMPORT_MODE, this.importMode.name());
        tag.putString(TAG_IO_SPEED_MODE, this.ioSpeedMode.name());

        var unlimited = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++)
        {
            if (this.unlimitedSlots[i])
            {
                var entry = new CompoundTag();
                entry.putInt("slot", i);
                unlimited.add(entry);
            }
        }
        tag.put(TAG_UNLIMITED_SLOTS, unlimited);

        var conns = new ListTag();
        for (var conn : this.connections)
        {
            conns.add(conn.toTag());
        }
        tag.put(TAG_CONNECTIONS, conns);

        this.filterInv.writeToNBT(tag, TAG_FILTER_INV, registries);
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadTag(tag, registries);
        this.interfaceMode = readEnum(tag.getString(TAG_INTERFACE_MODE), InterfaceMode.class,
                InterfaceMode.NORMAL);
        this.exportMode = readEnum(tag.getString(TAG_EXPORT_MODE), ExportMode.class, ExportMode.OFF);
        this.importMode = readEnum(tag.getString(TAG_IMPORT_MODE), ImportMode.class, ImportMode.OFF);
        this.ioSpeedMode = readEnum(tag.getString(TAG_IO_SPEED_MODE), IOSpeedMode.class,
                IOSpeedMode.NORMAL);

        java.util.Arrays.fill(this.unlimitedSlots, false);
        var unlimited = tag.getList(TAG_UNLIMITED_SLOTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < unlimited.size(); i++)
        {
            int slot = unlimited.getCompound(i).getInt("slot");
            if (slot >= 0 && slot < SLOT_COUNT)
            {
                this.unlimitedSlots[slot] = true;
            }
        }

        this.connections.clear();
        var conns = tag.getList(TAG_CONNECTIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < conns.size() && this.connections.size() < MAX_WIRELESS_CONNECTIONS; i++)
        {
            this.connections.add(WirelessConnection.fromTag(conns.getCompound(i)));
        }

        this.filterInv.readFromNBT(tag, TAG_FILTER_INV, registries);
    }

    private static <E extends Enum<E>> E readEnum(String name, Class<E> type, E fallback)
    {
        try
        {
            return Enum.valueOf(type, name);
        }
        catch (IllegalArgumentException e)
        {
            return fallback;
        }
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data)
    {
        super.writeToStream(data);
        data.writeByte(this.interfaceMode.ordinal());
        data.writeByte(this.exportMode.ordinal());
        data.writeByte(this.importMode.ordinal());
        data.writeByte(this.ioSpeedMode.ordinal());
        // 连接明细要下发：客户端屏幕显示连接数、世界渲染也要用
        data.writeVarInt(this.connections.size());
        for (var conn : this.connections)
        {
            data.writeResourceLocation(conn.dimension().location());
            data.writeBlockPos(conn.pos());
            data.writeByte(conn.boundFace().get3DDataValue());
        }
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data)
    {
        boolean changed = super.readFromStream(data);
        this.interfaceMode = InterfaceMode.values()[data.readByte()];
        this.exportMode = ExportMode.values()[data.readByte()];
        this.importMode = ImportMode.values()[data.readByte()];
        this.ioSpeedMode = IOSpeedMode.values()[data.readByte()];
        int count = data.readVarInt();
        this.connections.clear();
        for (int i = 0; i < count; i++)
        {
            var dim = ResourceKey.create(Registries.DIMENSION, data.readResourceLocation());
            var pos = data.readBlockPos();
            var face = Direction.from3DDataValue(data.readByte());
            this.connections.add(new WirelessConnection(dim, pos, face));
        }
        return true;
    }

    /** 逻辑层访问：本 BE 的接口逻辑。 */
    public OverloadedInterfaceLogic getOverloadedLogic()
    {
        return this.getInterfaceLogic() instanceof OverloadedInterfaceLogic logic ? logic : null;
    }
}
