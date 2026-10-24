package com.godofthings.infinityprovider;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.crafting.PatternProviderBlockEntity;

/**
 * 增强供应器 BE 的公共基类：在原版供应器之上叠加
 * 无线模式 / 回收模式 / 自适应倍增 / 高级设置（移植自 AE2LT 扩展过载样板供应器，
 * 裁剪掉 Thunderbolt / FE 计费 / 频率绑定后的自包含实现）。
 */
public abstract class InfEnhancedBlockEntity extends PatternProviderBlockEntity
        implements InfEnhancedHost, ServerTickingBlockEntity {

    private static final String TAG_CONNECTIONS = "InfConnections";

    private final InfProviderSettings settings = new InfProviderSettings(this::onSettingsDirty);
    private final List<InfWirelessConnection> connections = new ArrayList<>();

    private int clientConnectionCount;

    protected InfEnhancedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public InfProviderSettings infSettings() {
        return this.settings;
    }

    @Override
    public List<InfWirelessConnection> infConnections() {
        return this.connections;
    }

    @Override
    public void infConnectionsChanged() {
        if (this.getLogic() instanceof InfProviderLogic logic) {
            logic.infInvalidateCaches();
        }
        this.saveChanges();
    }

    @Override
    public void infSettingsChanged() {
        this.infConnectionsChanged();
    }

    private void onSettingsDirty() {
        this.infSettingsChanged();
    }

    /** 无线模式下不参与贴邻派发（原版 getTargets 路径）。 */
    @Override
    public java.util.EnumSet<Direction> getTargets() {
        if (this.settings.getProviderMode() == InfProviderSettings.ProviderMode.WIRELESS) {
            return java.util.EnumSet.noneOf(Direction.class);
        }
        return super.getTargets();
    }

    /** 服务端节拍：驱动回收扫描（AUTO/EJECT 共用）。 */
    @Override
    public void serverTick() {
        if (this.getLevel() != null && !this.getLevel().isClientSide
                && this.getLogic() instanceof InfProviderLogic logic) {
            logic.tickReturnSweep();
        }
    }

    // ---- NBT ----

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.settings.writeToTag(tag);
        InfWirelessConnection.writeTagList(tag, TAG_CONNECTIONS, this.connections);
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        this.settings.readFromTag(tag);
        this.connections.clear();
        this.connections.addAll(InfWirelessConnection.readTagList(tag, TAG_CONNECTIONS,
                InfEnhancedHost.INF_MAX_CONNECTIONS));
        if (this.getLogic() instanceof InfProviderLogic logic) {
            logic.infInvalidateCaches();
        }
    }

    // ---- 客户端同步（设置 + 连接数摘要；连接明细不下发）----

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        this.settings.writeToStream(data);
        data.writeVarInt(this.connections.size());
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        changed |= this.settings.readFromStream(data);
        this.clientConnectionCount = data.readVarInt();
        return changed;
    }

    public int getClientConnectionCount() {
        return this.clientConnectionCount;
    }

    public int getConnectionCount() {
        return this.connections.size();
    }

    public boolean isConnectionsFull() {
        return this.connections.size() >= InfEnhancedHost.INF_MAX_CONNECTIONS;
    }

    /** 连接器工具入口：本供应器是否接受这个目标（默认只接受本维度）。 */
    public boolean acceptsWirelessTarget(ResourceKey<net.minecraft.world.level.Level> dimension,
            BlockPos pos, Direction face) {
        var level = this.getLevel();
        return level != null && level.dimension().equals(dimension);
    }
}
