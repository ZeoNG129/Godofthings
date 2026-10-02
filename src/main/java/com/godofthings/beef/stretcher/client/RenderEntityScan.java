package com.godofthings.beef.stretcher.client;

import com.godofthings.beef.stretcher.content.entity.TimeFlowEntity;
import com.godofthings.beef.stretcher.content.entity.WondrousStaffAccelerationEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 渲染路径上的实体扫描缓存。
 *
 * <p><b>【本模组对上游代码的本地改动】</b> useless_stretcher 上游有三处在<b>渲染路径</b>里直接
 * {@code level.getEntitiesOfClass(..., inflate(96/64))}：HUD 那条还是每帧无条件扫（不看手里拿着什么），
 * 另外两条只在手持手杖时扫。这里统一改成每 {@link #REFRESH_TICKS} tick 扫一次的快照、三处共用 ——
 * 最多 0.4 秒延迟（肉眼看不出来），省掉的是每帧一次 192 格范围的实体查询。</p>
 *
 * <p><b>重新移植 useless_stretcher 上游时记得带上这段改动</b>（本类 + 三处调用点，
 * 调用点都标了 {@code [本地改动]} 注释）。</p>
 */
public final class RenderEntityScan
{
    /** 快照刷新间隔（tick）：8 = 0.4 秒 */
    private static final int REFRESH_TICKS = 8;
    /** 加速实体扫描半径：上游 HUD 用 96、高亮用 64，这里统一按 96 扫，64 那处再自己过滤 */
    private static final double ACCEL_RADIUS = 96.0D;
    /** 时间流领域扫描半径（与上游一致） */
    private static final double TIME_FLOW_RADIUS = 96.0D;

    private static ClientLevel cachedLevel;
    private static long lastRefresh = Long.MIN_VALUE;
    private static List<WondrousStaffAccelerationEntity> accelCache = List.of();
    private static List<TimeFlowEntity> timeFlowCache = List.of();

    private RenderEntityScan() {}

    /** 附近的加速实体（方块机器 + 时间加速），快照最多 8 tick 旧 */
    public static List<WondrousStaffAccelerationEntity> accelEntities(ClientLevel level, Player player)
    {
        ensureFresh(level, player);
        return accelCache;
    }

    /** 附近的时间流领域实体 */
    public static List<TimeFlowEntity> timeFlowEntities(ClientLevel level, Player player)
    {
        ensureFresh(level, player);
        return timeFlowCache;
    }

    private static void ensureFresh(ClientLevel level, Player player)
    {
        long now = level.getGameTime();
        if (level == cachedLevel && lastRefresh != Long.MIN_VALUE && now - lastRefresh < REFRESH_TICKS)
        {
            return;
        }
        cachedLevel = level;
        lastRefresh = now;

        accelCache = level.getEntitiesOfClass(WondrousStaffAccelerationEntity.class,
                        new AABB(player.blockPosition()).inflate(ACCEL_RADIUS))
                .stream().filter(entity -> !entity.isRemoved()).toList();

        timeFlowCache = level.getEntitiesOfClass(TimeFlowEntity.class,
                        player.getBoundingBox().inflate(TIME_FLOW_RADIUS))
                .stream().filter(entity -> !entity.isRemoved()).toList();
    }
}
