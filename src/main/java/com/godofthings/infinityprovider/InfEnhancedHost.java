package com.godofthings.infinityprovider;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 增强供应器宿主（BE 与 Part 都实现）：持有设置与无线连接列表。
 * 菜单的网络动作通过这个接口读写宿主状态，与具体宿主类型解耦。
 */
public interface InfEnhancedHost {

    InfProviderSettings infSettings();

    /** 当前生效的无线连接列表（可变引用；由宿主负责持久化）。 */
    List<InfWirelessConnection> infConnections();

    int INF_MAX_CONNECTIONS = 1024;

    /** 添加或更新一条连接；同一（维度,坐标）只保留最新绑面。已达上限返回 false。 */
    default boolean infAddOrUpdateConnection(ResourceKey<Level> dimension, BlockPos pos, Direction face) {
        var connections = this.infConnections();
        int index = InfWirelessConnection.indexOf(connections, dimension, pos);
        if (index >= 0) {
            var updated = new InfWirelessConnection(dimension, pos, face);
            if (connections.get(index).equals(updated)) {
                return true;
            }
            connections.set(index, updated);
            this.infConnectionsChanged();
            return true;
        }
        if (connections.size() >= INF_MAX_CONNECTIONS) {
            return false;
        }
        connections.add(new InfWirelessConnection(dimension, pos, face));
        this.infConnectionsChanged();
        return true;
    }

    /** 移除一条连接；不存在返回 false。 */
    default boolean infRemoveConnection(ResourceKey<Level> dimension, BlockPos pos) {
        var connections = this.infConnections();
        int index = InfWirelessConnection.indexOf(connections, dimension, pos);
        if (index < 0) {
            return false;
        }
        connections.remove(index);
        this.infConnectionsChanged();
        return true;
    }

    /** 设置或连接变更后的回调：存盘 + 让逻辑层失效缓存。 */
    void infConnectionsChanged();

    /** 设置变更后的回调（由 InfProviderSettings 的 DirtyListener 触发）。 */
    void infSettingsChanged();
}
