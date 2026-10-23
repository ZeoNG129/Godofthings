package com.godofthings.doll.entity;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * 玩偶的服务端辅助逻辑。
 *
 * <p><b>裁剪说明</b>：上游的 {@code TwoIDollService} 大部分是「星光庭院」维度
 * （独立世界 + 星空演出 + 邀请 + 奖励掉落 + 同行玩偶跨维度搬运）的调度。
 * 本模组按需求<b>移除了星光庭院</b>，对应方法（sendPlayerToStarfield / returnFromStarfield /
 * tickCompanion / dropReward / tryStartCourtyardShow 等）连同维度与 ChunkGenerator 一并删除；
 * 这里只保留两件事：</p>
 * <ol>
 *   <li>服务器停机时清掉玩偶追踪表；</li>
 *   <li>安全落点工具（safeLocation / safeAt），供未来需要传送玩偶时复用。</li>
 * </ol>
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class TwoIDollService {
    private TwoIDollService() {
    }

    /** 服务器停机：清空玩偶追踪表，避免跨存档残留 */
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        TwoIDollEntity.clearTracking();
    }

    /** 在 {@code desired} 附近找一个能站人的落点；全部失败返回 {@code null}。 */
    static Vec3 safeLocation(ServerLevel level, ServerPlayer player, Vec3 desired) {
        if (!Double.isFinite(desired.x) || !Double.isFinite(desired.y) || !Double.isFinite(desired.z)
                || !level.getWorldBorder().isWithinBounds(BlockPos.containing(desired))) return null;
        if (safeAt(level, player, desired)) return desired;

        BlockPos origin = BlockPos.containing(desired);
        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy = -2; dy <= 8; dy++) {
                        Vec3 candidate = Vec3.atBottomCenterOf(origin.offset(dx, dy, dz));
                        if (safeAt(level, player, candidate)) return candidate;
                    }
                }
            }
        }

        for (int dx = -8; dx <= 8; dx += 4) {
            for (int dz = -8; dz <= 8; dz += 4) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                Vec3 candidate = new Vec3(x + 0.5, y, z + 0.5);
                if (safeAt(level, player, candidate)) return candidate;
            }
        }
        return null;
    }

    /** 该点是否可站立：脚下有碰撞、不是岩浆/仙人掌等，头顶两格不卡 */
    private static boolean safeAt(ServerLevel level, ServerPlayer player, Vec3 pos) {
        BlockPos feet = BlockPos.containing(pos);
        if (feet.getY() < level.getMinBuildHeight() + 1 || feet.getY() > level.getMaxBuildHeight() - 3
                || !level.getWorldBorder().isWithinBounds(feet)) return false;
        BlockPos belowPos = feet.below();
        BlockState below = level.getBlockState(belowPos);
        if (below.getCollisionShape(level, belowPos).isEmpty()
                || below.is(Blocks.MAGMA_BLOCK)
                || below.is(Blocks.CACTUS)
                || below.is(Blocks.CAMPFIRE)
                || below.is(Blocks.SOUL_CAMPFIRE)) return false;
        for (int dy = 0; dy <= 1; dy++) {
            BlockState state = level.getBlockState(feet.above(dy));
            if (!state.getFluidState().isEmpty()
                    || state.is(Blocks.FIRE)
                    || state.is(Blocks.SOUL_FIRE)
                    || state.is(Blocks.POWDER_SNOW)
                    || state.is(Blocks.SWEET_BERRY_BUSH)) return false;
        }
        return level.noCollision(player, player.getDimensions(Pose.STANDING).makeBoundingBox(pos));
    }

    /** 给玩家发一条玩偶消息（actionBar 决定动作栏还是聊天栏） */
    private static void message(ServerPlayer player, String suffix, boolean actionBar) {
        player.displayClientMessage(Component.translatable("msg.godofthings.two_i." + suffix), actionBar);
    }
}