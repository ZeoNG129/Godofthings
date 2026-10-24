package com.godofthings.overloadedinterface;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.AEKeySlotFilter;
import appeng.api.storage.MEStorage;
import appeng.helpers.InterfaceLogic;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.util.ConfigInventory;
import appeng.util.ConfigMenuInventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;

/**
 * 过载接口逻辑（照抄上游 AE2LT 的 OverloadedInterfaceLogic 核心：反射替换
 * AE2 {@code InterfaceLogic} 的 config / storage 字段）。
 *
 * <p><b>为什么要替换 storage：</b>原版接口的存储槽是本地物品栏（放进什么就是什么）；
 * 上游把它换成 {@link ProxiedStorageInv}——存储槽的「种类」仍来自配置槽，
 * 「数量」直接显示 ME 网络里的实际库存，点击取放也直接操作 ME 网络
 * （ME 终端式交互）。这就是原版过载接口下方会显示网络数量的机制。</p>
 *
 * <p>裁剪（沿用既定口径）：FE 能量计费（PowerCostUtil）、合成卡缺货请求
 * （OverloadedInterfaceCrafting）、性能探针未移植；ticker 用自己的实现覆盖
 * AE2 原生的补货 ticker（上游同款 ClassToInstanceMap 后写生效技巧）。</p>
 */
public class OverloadedInterfaceLogic extends InterfaceLogic
{
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    /** 过载容量：1024 字节 × 每字节单位数（照抄上游）。 */
    private static final long OVERLOADED_BYTES = 1024L;

    // 反射句柄：替换 AE2 InterfaceLogic 的私有字段（照抄上游）
    private static final Field F_CONFIG;
    private static final Field F_STORAGE;
    private static final Method M_ON_CONFIG_CHANGED;
    private static final Method M_ON_STORAGE_CHANGED;

    static
    {
        try
        {
            F_CONFIG = InterfaceLogic.class.getDeclaredField("config");
            F_CONFIG.setAccessible(true);
            F_STORAGE = InterfaceLogic.class.getDeclaredField("storage");
            F_STORAGE.setAccessible(true);
            M_ON_CONFIG_CHANGED = InterfaceLogic.class.getDeclaredMethod("onConfigRowChanged");
            M_ON_CONFIG_CHANGED.setAccessible(true);
            M_ON_STORAGE_CHANGED = InterfaceLogic.class.getDeclaredMethod("onStorageChanged");
            M_ON_STORAGE_CHANGED.setAccessible(true);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("Failed to init reflection for OverloadedInterfaceLogic", e);
        }
    }

    private final OverloadedInterfaceBlockEntity owner;
    private final ProxiedStorageInv proxiedStorage;
    private final int slotCount;

    public OverloadedInterfaceLogic(IManagedGridNode gridNode, OverloadedInterfaceBlockEntity host,
            Item item, int slots)
    {
        super(gridNode, host, item, slots);
        this.owner = host;
        this.slotCount = slots;

        // 配置槽：解除 AE2 默认的单格数量上限（按过载容量给上限）
        var newConfig = new ConfigInventory(
                AEKeyTypes.getAll(), null, GenericStackInv.Mode.CONFIG_STACKS, slots,
                () -> invokeQuietly(M_ON_CONFIG_CHANGED, this), false)
        {
            @Override
            public long getMaxAmount(AEKey key)
            {
                return getCapacity(key.getType());
            }
        };
        setField(F_CONFIG, newConfig);
        newConfig.useRegisteredCapacities();
        for (var type : AEKeyTypes.getAll())
        {
            newConfig.setCapacity(type, overloadedCap(type));
        }

        // 存储槽：换成 ME 网络代理（显示/取放都走网络）
        this.proxiedStorage = new ProxiedStorageInv(
                this, AEKeyTypes.getAll(), slots, () -> invokeQuietly(M_ON_STORAGE_CHANGED, this));
        setField(F_STORAGE, this.proxiedStorage);
        this.proxiedStorage.useRegisteredCapacities();
        for (var type : AEKeyTypes.getAll())
        {
            this.proxiedStorage.setCapacity(type, overloadedCap(type));
        }

        // 覆盖 AE2 原生注册的 stocking ticker（ClassToInstanceMap 后写生效）
        gridNode.addService(IGridTickable.class, new ProxyTicker());
    }

    public OverloadedInterfaceBlockEntity getOwner()
    {
        return this.owner;
    }

    public ProxiedStorageInv getProxiedStorage()
    {
        return this.proxiedStorage;
    }

    private static long overloadedCap(AEKeyType type)
    {
        return OVERLOADED_BYTES * type.getAmountPerByte();
    }

    private void setField(Field field, Object value)
    {
        try
        {
            field.set(this, value);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException("Reflection set failed", e);
        }
    }

    private static void invokeQuietly(Method method, Object target)
    {
        try
        {
            method.invoke(target);
        }
        catch (Exception e)
        {
            LOG.warn("Reflection invoke failed: {}.{}",
                    method.getDeclaringClass().getSimpleName(), method.getName(), e);
        }
    }

    /**
     * 设置配置槽内容但保留无限标记（照抄上游 setConfigStackSuppressed）。
     * 切无限时把数量归 1 用。
     */
    public void setConfigStackSuppressed(int slot, @Nullable GenericStack stack)
    {
        var config = this.getConfig();
        config.setStack(slot, stack);
    }

    /** IO ticker：只驱动导出 / 导入，不做原版的本地补货。 */
    private class ProxyTicker implements IGridTickable
    {
        @Override
        public TickingRequest getTickingRequest(IGridNode node)
        {
            return new TickingRequest(1, 5, false);
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall)
        {
            if (!mainNode.isActive())
            {
                return TickRateModulation.IDLE;
            }
            boolean hasWork = owner.hasGridItemIoWork();
            if (hasWork)
            {
                owner.tickGridItemIo();
            }
            return hasWork ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
        }
    }

    // ============================================================
    // ProxiedStorageInv：存储槽代理 ME 网络（照抄上游，去掉 FE 计费）
    // ============================================================

    /**
     * 存储槽的 ME 网络代理视图。
     *
     * <ul>
     *   <li>getStack/getAmount 返回网络实际数量（受每槽上限约束）</li>
     *   <li>insert/extract 直接操作 ME 网络</li>
     *   <li>getAvailableStacks 只暴露配置过的种类（防止网络缓存膨胀）</li>
     *   <li>proxying 重入保护：网络回调再进本对象时直接拒绝</li>
     * </ul>
     */
    public static class ProxiedStorageInv extends ConfigInventory
    {
        private final OverloadedInterfaceLogic logic;
        private boolean proxying = false;
        /** 每 tick 的显示数量缓存：外部 IItemHandler 每帧轮询，不缓存会反复模拟整个网络。 */
        private final long[] displayAmountCache;
        private long displayAmountCacheTick = Long.MIN_VALUE;

        ProxiedStorageInv(OverloadedInterfaceLogic logic, Set<AEKeyType> supportedTypes,
                int size, @Nullable Runnable listener)
        {
            super(supportedTypes, null, GenericStackInv.Mode.STORAGE, size, listener, false);
            this.logic = logic;
            this.displayAmountCache = new long[size];
        }

        private IActionSource src()
        {
            return IActionSource.ofMachine(logic.owner);
        }

        private ConfigInventory cfg()
        {
            return logic.getConfig();
        }

        /** 该槽对外暴露的数量上限：无限槽不限量，普通槽 = 配置量。 */
        public long capForSlot(int slot)
        {
            if (logic.owner.isSlotUnlimited(slot))
            {
                return Long.MAX_VALUE;
            }
            long amount = cfg().getAmount(slot);
            if (amount > 0)
            {
                return amount;
            }
            var key = cfg().getKey(slot);
            return key != null ? key.getType().getAmountPerUnit() : Long.MAX_VALUE;
        }

        private long displayAmount(int slot, AEKey key)
        {
            var be = logic.host.getBlockEntity();
            var level = be != null ? be.getLevel() : null;
            long now = level != null ? level.getGameTime() : Long.MIN_VALUE;
            boolean cacheable = level != null && slot >= 0 && slot < this.displayAmountCache.length;
            if (cacheable)
            {
                if (now != this.displayAmountCacheTick)
                {
                    Arrays.fill(this.displayAmountCache, -1L);
                    this.displayAmountCacheTick = now;
                }
                long cached = this.displayAmountCache[slot];
                if (cached >= 0)
                {
                    return cached;
                }
            }
            long amount = this.visibleNetworkAmount(key, this.capForSlot(slot));
            if (cacheable)
            {
                this.displayAmountCache[slot] = amount;
            }
            return amount;
        }

        private long visibleNetworkAmount(AEKey key, long cap)
        {
            var grid = logic.mainNode.getGrid();
            if (grid == null || key == null)
            {
                return 0;
            }
            var storage = grid.getStorageService();
            long reported = storage.getCachedInventory().get(key);
            long simulated = this.simulateNetworkExtraction(storage.getInventory(), key, cap);
            // 上游 mergeReportedAndSimulatedAmount：网络报告值与模拟可取值取保守合并
            return Math.min(Math.max(reported, simulated), cap);
        }

        private long simulateNetworkExtraction(MEStorage network, AEKey key, long cap)
        {
            if (network == null || key == null || cap <= 0)
            {
                return 0;
            }
            boolean wasProxying = this.proxying;
            this.proxying = true;
            try
            {
                return network.extract(key, cap, Actionable.SIMULATE, this.src());
            }
            finally
            {
                this.proxying = wasProxying;
            }
        }

        // ── 显示：getStack/getAmount 直接给网络数量 ──

        @Override
        public @Nullable GenericStack getStack(int slot)
        {
            if (logic.mainNode.getGrid() == null)
            {
                return super.getStack(slot);
            }
            var key = this.cfg().getKey(slot);
            if (key == null)
            {
                return null;
            }
            long amount = this.displayAmount(slot, key);
            return amount > 0 ? new GenericStack(key, amount) : null;
        }

        @Override
        public @Nullable AEKey getKey(int slot)
        {
            if (logic.mainNode.getGrid() == null)
            {
                return super.getKey(slot);
            }
            return this.cfg().getKey(slot);
        }

        @Override
        public long getAmount(int slot)
        {
            if (logic.mainNode.getGrid() == null)
            {
                return super.getAmount(slot);
            }
            var key = this.cfg().getKey(slot);
            if (key == null)
            {
                return 0;
            }
            return this.displayAmount(slot, key);
        }

        @Override
        public boolean isAllowedIn(int slot, AEKey what)
        {
            return what != null && this.isSupportedType(what);
        }

        @Override
        public ConfigMenuInventory createMenuWrapper()
        {
            // 默认包装即可：存储槽的点击交互由菜单 clicked() 直接调 proxyInsert/proxyExtract
            return new ConfigMenuInventory(this);
        }

        // ── 取放：直接代理 ME 网络 ──

        @Override
        public long insert(int slot, AEKey what, long amount, Actionable mode)
        {
            if (what == null || amount <= 0)
            {
                return 0;
            }
            return this.proxyInsert(what, amount, mode);
        }

        @Override
        public long extract(int slot, AEKey what, long amount, Actionable mode)
        {
            if (what == null || amount <= 0)
            {
                return 0;
            }
            var key = this.cfg().getKey(slot);
            if (key == null || !key.equals(what))
            {
                return 0;
            }
            long capped = Math.min(amount, this.capForSlot(slot));
            return this.proxyExtract(what, capped, mode);
        }

        /** 公开代理：菜单 clicked() 直接调用（重入安全）。 */
        public long proxyExtract(AEKey what, long amount, Actionable mode)
        {
            if (this.proxying)
            {
                return 0;
            }
            this.proxying = true;
            try
            {
                var grid = logic.mainNode.getGrid();
                if (grid == null)
                {
                    return 0;
                }
                return grid.getStorageService().getInventory()
                        .extract(what, amount, mode, this.src());
            }
            finally
            {
                this.proxying = false;
            }
        }

        /** 公开代理：菜单 clicked() 直接调用（重入安全）。 */
        public long proxyInsert(AEKey what, long amount, Actionable mode)
        {
            if (this.proxying)
            {
                return 0;
            }
            this.proxying = true;
            try
            {
                var grid = logic.mainNode.getGrid();
                if (grid == null)
                {
                    return 0;
                }
                return grid.getStorageService().getInventory()
                        .insert(what, amount, mode, this.src());
            }
            finally
            {
                this.proxying = false;
            }
        }

        public boolean isNetworkOperationInProgress()
        {
            return this.proxying;
        }

        // ── 网络缓存视图：只暴露配置过的种类（同 key 多槽合并上限，不重复计数）──

        @Override
        public void getAvailableStacks(KeyCounter out)
        {
            if (this.proxying)
            {
                return;
            }
            var grid = logic.mainNode.getGrid();
            if (grid == null)
            {
                return;
            }
            this.proxying = true;
            try
            {
                var capByKey = new LinkedHashMap<AEKey, Long>();
                for (int slot = 0; slot < this.size(); slot++)
                {
                    var key = this.cfg().getKey(slot);
                    if (key == null)
                    {
                        continue;
                    }
                    capByKey.merge(key, this.capForSlot(slot), Long::sum);
                }
                for (var entry : capByKey.entrySet())
                {
                    var key = entry.getKey();
                    long cap = entry.getValue();
                    if (cap == Long.MAX_VALUE)
                    {
                        cap = grid.getStorageService().getCachedInventory().get(key);
                    }
                    else
                    {
                        cap = Math.min(cap,
                                grid.getStorageService().getCachedInventory().get(key));
                    }
                    if (cap > 0)
                    {
                        out.add(key, cap);
                    }
                }
            }
            finally
            {
                this.proxying = false;
            }
        }

        // ── NBT：无持久化状态（数量实时来自网络）──

        @Override
        public void writeToChildTag(CompoundTag tag, String name, HolderLookup.Provider registries)
        {
            tag.remove(name);
        }

        @Override
        public void readFromChildTag(CompoundTag tag, String name, HolderLookup.Provider registries)
        {
        }
    }
}
