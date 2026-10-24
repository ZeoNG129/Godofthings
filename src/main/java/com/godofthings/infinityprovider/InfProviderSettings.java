package com.godofthings.infinityprovider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * 增强供应器的设置容器（移植自 AE2LT 扩展过载样板供应器）。
 * 供 BE / Part 两个宿主共用：NBT 持久化 + 网络同步的字段都收敛在这里，
 * 宿主只负责在变更时经 {@link DirtyListener} 触发存盘与缓存失效。
 */
public final class InfProviderSettings {

    /** 运行模式：NORMAL（贴邻推送）或 WIRELESS（远程无线派发）。 */
    public enum ProviderMode {
        NORMAL, WIRELESS
    }

    /** 回收模式：OFF 不回收 / AUTO 主动抽取产物 / EJECT 虚拟输出仓（仅无线）。 */
    public enum ReturnMode {
        OFF, AUTO, EJECT
    }

    /** 无线派发策略：SINGLE_TARGET 逐台轮询 / EVEN_DISTRIBUTION 均分。 */
    public enum WirelessDispatchMode {
        SINGLE_TARGET, EVEN_DISTRIBUTION
    }

    /** 无线速度：NORMAL 标准节奏 / FAST 探针式快速重试。 */
    public enum WirelessSpeedMode {
        NORMAL, FAST
    }

    public interface DirtyListener {
        void onSettingsDirty();
    }

    private ProviderMode providerMode = ProviderMode.NORMAL;
    private ReturnMode returnMode = ReturnMode.OFF;
    private WirelessDispatchMode wirelessDispatchMode = WirelessDispatchMode.EVEN_DISTRIBUTION;
    private WirelessSpeedMode wirelessSpeedMode = WirelessSpeedMode.NORMAL;
    private boolean adaptiveBatchEnabled = false;
    /** 输入过滤：开启后被动回流只收当前样板的产物，关闭收一切。 */
    private boolean filteredImport = false;
    private int machineParallelism = 1;

    private final DirtyListener listener;

    public InfProviderSettings(DirtyListener listener) {
        this.listener = listener;
    }

    private void markDirty() {
        if (this.listener != null) {
            this.listener.onSettingsDirty();
        }
    }

    public ProviderMode getProviderMode() {
        return this.providerMode;
    }

    public void setProviderMode(ProviderMode mode) {
        if (this.providerMode == mode) {
            return;
        }
        this.providerMode = mode;
        this.markDirty();
    }

    public ReturnMode getReturnMode() {
        return this.returnMode;
    }

    public void setReturnMode(ReturnMode mode) {
        if (this.returnMode == mode) {
            return;
        }
        this.returnMode = mode;
        this.markDirty();
    }

    public WirelessDispatchMode getWirelessDispatchMode() {
        return this.wirelessDispatchMode;
    }

    public void setWirelessDispatchMode(WirelessDispatchMode mode) {
        if (this.wirelessDispatchMode == mode) {
            return;
        }
        this.wirelessDispatchMode = mode;
        this.markDirty();
    }

    public WirelessSpeedMode getWirelessSpeedMode() {
        return this.wirelessSpeedMode;
    }

    public void setWirelessSpeedMode(WirelessSpeedMode mode) {
        if (this.wirelessSpeedMode == mode) {
            return;
        }
        this.wirelessSpeedMode = mode;
        this.markDirty();
    }

    public boolean isAdaptiveBatchEnabled() {
        return this.adaptiveBatchEnabled;
    }

    public void setAdaptiveBatchEnabled(boolean enabled) {
        if (this.adaptiveBatchEnabled == enabled) {
            return;
        }
        this.adaptiveBatchEnabled = enabled;
        this.markDirty();
    }

    public boolean isFilteredImport() {
        return this.filteredImport;
    }

    public void setFilteredImport(boolean enabled) {
        if (this.filteredImport == enabled) {
            return;
        }
        this.filteredImport = enabled;
        this.markDirty();
    }

    public int getMachineParallelism() {
        return this.machineParallelism;
    }

    public void setMachineParallelism(int value) {
        int normalized = Math.max(1, value);
        if (this.machineParallelism == normalized) {
            return;
        }
        this.machineParallelism = normalized;
        this.markDirty();
    }

    // ---- NBT ----

    private static final String TAG_PROVIDER_MODE = "InfMode";
    private static final String TAG_RETURN_MODE = "InfReturn";
    private static final String TAG_WIRELESS_DISPATCH = "InfDispatch";
    private static final String TAG_WIRELESS_SPEED = "InfSpeed";
    private static final String TAG_ADAPTIVE_BATCH = "InfAdaptive";
    private static final String TAG_FILTERED_IMPORT = "InfFiltered";
    private static final String TAG_MACHINE_PARALLELISM = "InfParallel";

    public void writeToTag(CompoundTag tag) {
        tag.putString(TAG_PROVIDER_MODE, this.providerMode.name());
        tag.putString(TAG_RETURN_MODE, this.returnMode.name());
        tag.putString(TAG_WIRELESS_DISPATCH, this.wirelessDispatchMode.name());
        tag.putString(TAG_WIRELESS_SPEED, this.wirelessSpeedMode.name());
        tag.putBoolean(TAG_ADAPTIVE_BATCH, this.adaptiveBatchEnabled);
        tag.putBoolean(TAG_FILTERED_IMPORT, this.filteredImport);
        tag.putInt(TAG_MACHINE_PARALLELISM, this.machineParallelism);
    }

    public void readFromTag(CompoundTag tag) {
        this.providerMode = readEnum(tag, TAG_PROVIDER_MODE, ProviderMode.class, ProviderMode.NORMAL);
        this.returnMode = readEnum(tag, TAG_RETURN_MODE, ReturnMode.class, ReturnMode.OFF);
        this.wirelessDispatchMode = readEnum(tag, TAG_WIRELESS_DISPATCH, WirelessDispatchMode.class,
                WirelessDispatchMode.EVEN_DISTRIBUTION);
        this.wirelessSpeedMode = readEnum(tag, TAG_WIRELESS_SPEED, WirelessSpeedMode.class,
                WirelessSpeedMode.NORMAL);
        this.adaptiveBatchEnabled = tag.getBoolean(TAG_ADAPTIVE_BATCH);
        this.filteredImport = tag.getBoolean(TAG_FILTERED_IMPORT);
        this.machineParallelism = Math.max(1, tag.getInt(TAG_MACHINE_PARALLELISM));
    }

    private static <E extends Enum<E>> E readEnum(CompoundTag tag, String key,
            Class<E> type, E fallback) {
        if (!tag.contains(key)) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, tag.getString(key));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    // ---- 网络流（BE 客户端同步用，字段顺序与 writeToStream/readFromStream 一致）----

    public void writeToStream(RegistryFriendlyByteBuf data) {
        data.writeByte(this.providerMode.ordinal());
        data.writeByte(this.returnMode.ordinal());
        data.writeByte(this.wirelessDispatchMode.ordinal());
        data.writeByte(this.wirelessSpeedMode.ordinal());
        data.writeBoolean(this.adaptiveBatchEnabled);
        data.writeBoolean(this.filteredImport);
        data.writeVarInt(this.machineParallelism);
    }

    /** @return 设置是否有变化（供 readFromStream 汇报渲染刷新）。 */
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        var newMode = safeEnum(ProviderMode.values(), data.readByte(), ProviderMode.NORMAL);
        var newReturn = safeEnum(ReturnMode.values(), data.readByte(), ReturnMode.OFF);
        var newDispatch = safeEnum(WirelessDispatchMode.values(), data.readByte(),
                WirelessDispatchMode.EVEN_DISTRIBUTION);
        var newSpeed = safeEnum(WirelessSpeedMode.values(), data.readByte(), WirelessSpeedMode.NORMAL);
        boolean newAdaptive = data.readBoolean();
        boolean newFiltered = data.readBoolean();
        int newParallelism = Math.max(1, data.readVarInt());
        boolean changed = newMode != this.providerMode
                || newReturn != this.returnMode
                || newDispatch != this.wirelessDispatchMode
                || newSpeed != this.wirelessSpeedMode
                || newAdaptive != this.adaptiveBatchEnabled
                || newFiltered != this.filteredImport
                || newParallelism != this.machineParallelism;
        this.providerMode = newMode;
        this.returnMode = newReturn;
        this.wirelessDispatchMode = newDispatch;
        this.wirelessSpeedMode = newSpeed;
        this.adaptiveBatchEnabled = newAdaptive;
        this.filteredImport = newFiltered;
        this.machineParallelism = newParallelism;
        return changed;
    }

    private static <E extends Enum<E>> E safeEnum(E[] values, byte ordinal, E fallback) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }
}
