package com.godofthings.infinityprovider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import appeng.api.AECapabilities;
import appeng.api.config.Actionable;
import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.me.helpers.MachineSource;

import com.godofthings.mixin.PatternProviderLogicAccessor;

/**
 * 增强供应器逻辑（移植自 AE2LT 的 OverloadedPatternProviderLogic，裁剪掉
 * Thunderbolt 批处理 / FE 能量计费 / AdvancedAE 兼容 / 频率绑定后的自包含版）。
 *
 * <p>核心三件事：</p>
 * <ul>
 *   <li><b>无线派发</b>（{@link #pushPattern} 覆写）：WIRELESS 模式下把样板输入
 *       推给连接列表里的远程机器（逐台轮询，含自适应倍增）；</li>
 *   <li><b>回收扫描</b>（{@link #tickReturnSweep}）：AUTO 模式下周期性从机器
 *       抽取与当前样板产物匹配的输出；EJECT 模式把返回栏包装成 ME 存储暴露给
 *       连接机器的输出面（见 {@link #getEjectStorage}）；</li>
 *   <li><b>自适应倍增</b>：每台机器记住上一个完整接收的批次大小，下次从该值
 *       起步尝试，被拒则折半回退。</li>
 * </ul>
 */
public class InfProviderLogic extends PatternProviderLogic {

    private static final int TARGET_CACHE_TTL = 20;
    /** 每次回收扫描最多轮询的机器数。 */
    private static final int MAX_RETURN_POLLS_PER_TICK = 64;
    /** 回收扫描间隔（tick）。 */
    private static final int RETURN_INTERVAL = 20;

    private final InfEnhancedHost enhancedHost;
    private final IActionSource actionSource;

    /** 连接 →（派发目标 + 底层存储）缓存，TTL 由游戏刻驱动。 */
    private final Map<InfWirelessConnection, CachedTarget> targetCaches = new IdentityHashMap<>();

    private record CachedTarget(@Nullable PatternProviderTarget target, @Nullable MEStorage storage, long tick) {
    }

    /** 每台机器 × 每个样板的上次成功批次（自适应倍增的历史）。 */
    private final Map<InfWirelessConnection, Map<IPatternDetails, Integer>> batchHistory =
            new IdentityHashMap<>();

    /** 派发 / 回收共用的轮询游标。 */
    private int cursor;
    private long nextSweepTick;

    public InfProviderLogic(IManagedGridNode mainNode, PatternProviderLogicHost host,
            int slotCount, InfEnhancedHost enhancedHost) {
        super(mainNode, host, slotCount);
        this.enhancedHost = enhancedHost;
        this.actionSource = new MachineSource(mainNode::getNode);
    }

    private InfProviderSettings settings() {
        return this.enhancedHost.infSettings();
    }

    private Level hostLevel() {
        return this.hostBlockEntity().getLevel();
    }

    private net.minecraft.world.level.block.entity.BlockEntity hostBlockEntity() {
        return ((PatternProviderLogicAccessor) this).getHost().getBlockEntity();
    }

    // ==================== 无线派发 ====================

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (this.settings().getProviderMode() == InfProviderSettings.ProviderMode.WIRELESS) {
            return this.wirelessPushPattern(patternDetails, inputHolder);
        }
        return super.pushPattern(patternDetails, inputHolder);
    }

    private boolean wirelessPushPattern(IPatternDetails pattern, KeyCounter[] inputs) {
        var accessor = (PatternProviderLogicAccessor) this;
        if (!accessor.getSendList().isEmpty()
                || !accessor.getMainNode().isActive()
                || this.getCraftingLockedReason() != LockCraftingMode.NONE) {
            return false;
        }
        if (!(this.hostLevel() instanceof ServerLevel level)) {
            return false;
        }
        var connections = this.validConnections(level);
        if (connections.isEmpty()) {
            return false;
        }
        long gameTick = level.getGameTime();

        boolean any = false;
        for (int i = 0; i < connections.size() && !any; i++) {
            var conn = connections.get((this.cursor + i) % connections.size());
            any = this.tryPushToConnection(level, conn, pattern, inputs, gameTick);
        }
        if (any) {
            this.cursor++;
            accessor.invokeOnPushPatternSuccess(pattern);
        }
        return any;
    }

    private boolean tryPushToConnection(ServerLevel level, InfWirelessConnection conn,
            IPatternDetails pattern, KeyCounter[] inputs, long gameTick) {
        var target = this.resolveTarget(level, conn, gameTick);
        if (target == null || target.target() == null) {
            return false;
        }
        var dispatch = target.target();
        if (!pattern.supportsPushInputsToExternalInventory()) {
            return false;
        }
        // 阻挡模式：机器里还留着样板输入时不派新任务
        if (this.isBlocking()
                && dispatch.containsPatternInput(
                        ((PatternProviderLogicAccessor) this).getPatternInputs())) {
            return false;
        }

        int copies = 1;
        if (this.settings().isAdaptiveBatchEnabled()) {
            copies = this.adaptiveCopies(conn, pattern, this.settings().getMachineParallelism());
        }

        var planned = planInputs(pattern, inputs, copies);
        if (planned == null || planned.isEmpty()) {
            return false;
        }
        // 先模拟全部可入，再实际投放；一次失败整单让出，不产生半截批次
        for (var stack : planned) {
            if (dispatch.insert(stack.what(), stack.amount(), Actionable.SIMULATE) < stack.amount()) {
                this.rememberBatch(conn, pattern, Math.max(1, copies / 2));
                return false;
            }
        }
        for (var stack : planned) {
            long inserted = dispatch.insert(stack.what(), stack.amount(), Actionable.MODULATE);
            if (inserted < stack.amount()) {
                ((PatternProviderLogicAccessor) this)
                        .invokeAddToSendList(stack.what(), stack.amount() - inserted);
            }
        }
        this.rememberBatch(conn, pattern, copies);
        return true;
    }

    /** 自适应倍增：从记忆值起步（受机器并行数上限约束）。 */
    private int adaptiveCopies(InfWirelessConnection conn, IPatternDetails pattern, int cap) {
        int remembered = this.batchHistory
                .getOrDefault(conn, Map.of())
                .getOrDefault(pattern, 1);
        return Math.max(1, Math.min(remembered, Math.max(1, cap)));
    }

    /** 记住本台机器对样板的最新批次；成功翻倍由下次起步值体现，被拒折半。 */
    private void rememberBatch(InfWirelessConnection conn, IPatternDetails pattern, int copies) {
        this.batchHistory.computeIfAbsent(conn, k -> new IdentityHashMap<>())
                .put(pattern, Math.max(1, copies));
    }

    @Nullable
    private static List<GenericStack> planInputs(IPatternDetails pattern, KeyCounter[] inputs, int copies) {
        if (copies <= 0) {
            return null;
        }
        var planned = new ArrayList<GenericStack>();
        try {
            pattern.pushInputsToExternalInventory(inputs, (what, amount) -> {
                long scaled = Math.multiplyExact(amount, (long) copies);
                if (scaled > 0) {
                    planned.add(new GenericStack(what, scaled));
                }
            });
        } catch (ArithmeticException overflow) {
            return null;
        }
        return planned;
    }

    @Nullable
    private CachedTarget resolveTarget(ServerLevel level, InfWirelessConnection conn, long gameTick) {
        var cached = this.targetCaches.get(conn);
        if (cached != null && gameTick - cached.tick() < TARGET_CACHE_TTL) {
            return cached;
        }
        var targetLevel = level.getServer().getLevel(conn.dimension());
        if (targetLevel == null || !targetLevel.isLoaded(conn.pos())) {
            cached = new CachedTarget(null, null, gameTick);
            this.targetCaches.put(conn, cached);
            return cached;
        }
        var be = targetLevel.getBlockEntity(conn.pos());
        PatternProviderTarget target = null;
        MEStorage storage = null;
        if (be != null && !be.isRemoved()) {
            // ME 存储能力优先；没有时回退外部存储策略（物品/流体 Forge 能力桥，
            // 与原版 PatternProviderTarget.get 的两级解析一致——普通熔炉等靠这条通道）
            storage = targetLevel.getCapability(
                    AECapabilities.ME_STORAGE, conn.pos(), be.getBlockState(), be,
                    conn.machineSide());
            if (storage == null) {
                storage = wrapExternalStorage(targetLevel, conn);
            }
            target = PatternProviderTarget.get(targetLevel, conn.pos(), be, conn.machineSide(),
                    this.actionSource);
        }
        cached = new CachedTarget(target, storage, gameTick);
        this.targetCaches.put(conn, cached);
        return cached;
    }

    /** 用 AE2 的外部存储策略把目标机器的物品 / 流体能力桥接成 ME 存储。 */
    @Nullable
    private static MEStorage wrapExternalStorage(ServerLevel level, InfWirelessConnection conn) {
        var strategies = appeng.parts.automation.StackWorldBehaviors
                .createExternalStorageStrategies(level, conn.pos(), conn.machineSide());
        var externalStorages = new java.util.IdentityHashMap<appeng.api.stacks.AEKeyType, MEStorage>(2);
        for (var entry : strategies.entrySet()) {
            var wrapper = entry.getValue().createWrapper(false, () -> {
            });
            if (wrapper != null) {
                externalStorages.put(entry.getKey(), wrapper);
            }
        }
        if (externalStorages.isEmpty()) {
            return null;
        }
        return new appeng.me.storage.CompositeStorage(externalStorages);
    }

    private List<InfWirelessConnection> validConnections(ServerLevel level) {
        var all = this.enhancedHost.infConnections();
        var result = new ArrayList<InfWirelessConnection>(all.size());
        for (var conn : all) {
            if (conn.isAlive(level)) {
                result.add(conn);
            }
        }
        return result;
    }

    /**
     * 清理失效连接（目标方块实体已消失且区块已加载才判定失效；未加载的保留）。
     * 每 100 tick 跑一轮，从回收扫描顺带驱动。
     */
    private long nextPruneTick;

    private void pruneInvalidConnections(ServerLevel level) {
        long gameTick = level.getGameTime();
        if (gameTick < this.nextPruneTick) {
            return;
        }
        this.nextPruneTick = gameTick + 100;
        var connections = this.enhancedHost.infConnections();
        if (connections.isEmpty()) {
            return;
        }
        boolean removed = connections.removeIf(conn -> {
            var targetLevel = level.getServer().getLevel(conn.dimension());
            // 维度不在或区块未加载：无法判定，保留
            if (targetLevel == null || !targetLevel.isLoaded(conn.pos())) {
                return false;
            }
            return targetLevel.getBlockEntity(conn.pos()) == null;
        });
        if (removed) {
            this.enhancedHost.infConnectionsChanged();
        }
    }

    /** 宿主设置 / 连接变更后调用：清空缓存，下一拍重建。 */
    public void infInvalidateCaches() {
        this.targetCaches.clear();
        this.batchHistory.clear();
    }

    // ==================== 回收（AUTO）====================

    /** 周期性清理失效连接 + AUTO 模式下从连接机器抽取匹配产物回网。 */
    public void tickReturnSweep() {
        if (!(this.hostLevel() instanceof ServerLevel level)
                || !((PatternProviderLogicAccessor) this).getMainNode().isActive()) {
            return;
        }
        // 连接清理与回收模式无关，始终驱动
        this.pruneInvalidConnections(level);
        if (this.settings().getReturnMode() != InfProviderSettings.ReturnMode.AUTO
                || this.settings().getProviderMode() != InfProviderSettings.ProviderMode.WIRELESS) {
            return;
        }
        long gameTick = level.getGameTime();
        if (gameTick < this.nextSweepTick) {
            return;
        }
        this.nextSweepTick = gameTick + RETURN_INTERVAL;

        var connections = this.validConnections(level);
        if (connections.isEmpty()) {
            return;
        }
        // 输入过滤关 = 不限制回收种类（空集合语义为「全收」）；开 = 只收样板产物
        var allowedOutputs = this.settings().isFilteredImport()
                ? this.currentPatternOutputs()
                : null;
        if (allowedOutputs != null && allowedOutputs.isEmpty()) {
            return;
        }
        int polled = Math.min(connections.size(), MAX_RETURN_POLLS_PER_TICK);
        for (int i = 0; i < polled; i++) {
            var conn = connections.get((this.cursor + i) % connections.size());
            var target = this.resolveTarget(level, conn, gameTick);
            if (target != null && target.storage() != null) {
                this.extractOutputs(target.storage(), allowedOutputs, this.getReturnInv());
            }
        }
        this.cursor += polled;
    }

    /** 当前全部样板的产物键集合（回收匹配用）。 */
    private Set<AEKey> currentPatternOutputs() {
        var outputs = new HashSet<AEKey>();
        var patternInv = this.getPatternInv();
        for (int i = 0; i < patternInv.size(); i++) {
            ItemStack stack = patternInv.getStackInSlot(i);
            var details = PatternDetailsHelper.decodePattern(stack, this.hostLevel());
            if (details == null) {
                continue;
            }
            for (var output : details.getOutputs()) {
                outputs.add(output.what());
            }
        }
        return outputs;
    }

    /** 从机器存储抽取匹配产物入返回栏；塞不下的原路留在机器里，绝不凭空消失。null = 不过滤全收。 */
    private void extractOutputs(MEStorage storage, @Nullable Set<AEKey> allowedOutputs,
            PatternProviderReturnInventory returnInv) {
        var counter = new KeyCounter();
        storage.getAvailableStacks(counter);
        for (var entry : counter) {
            var key = entry.getKey();
            long amount = entry.getLongValue();
            if (amount <= 0 || (allowedOutputs != null && !allowedOutputs.contains(key))) {
                continue;
            }
            long accepted = returnInv.insert(key, amount, Actionable.SIMULATE, this.actionSource);
            if (accepted <= 0) {
                continue;
            }
            long taken = storage.extract(key, accepted, Actionable.MODULATE, this.actionSource);
            if (taken > 0) {
                returnInv.insert(key, taken, Actionable.MODULATE, this.actionSource);
            }
        }
    }

    // ==================== 回收（EJECT）====================

    /**
     * EJECT 模式：把供应器返回栏包装成 ME 存储暴露给连接机器的输出面。
     * 机器把它当普通输出仓塞产物，返回栏自动回网。非 EJECT+无线组合返回 null。
     */
    @Nullable
    public MEStorage getEjectStorage() {
        if (this.settings().getReturnMode() != InfProviderSettings.ReturnMode.EJECT
                || this.settings().getProviderMode() != InfProviderSettings.ProviderMode.WIRELESS) {
            return null;
        }
        return new EjectStorage(this.getReturnInv());
    }

    /** 只进不出的输出仓视图。 */
    private record EjectStorage(PatternProviderReturnInventory returnInv) implements MEStorage {
        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            return this.returnInv.insert(what, amount, mode, source);
        }

        @Override
        public net.minecraft.network.chat.Component getDescription() {
            return net.minecraft.network.chat.Component.translatable(
                    "gui.godofthings.infprovider.eject_storage");
        }
    }
}
