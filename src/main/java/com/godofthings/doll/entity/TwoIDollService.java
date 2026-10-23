package com.godofthings.doll.entity;

import com.godofthings.Godofthings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * 玩偶的服务端辅助逻辑。
 *
 * <p><b>移植说明</b>：原 mod 的 {@code TwoIDollService} 有 449 行，其中绝大部分是
 * 「星光庭院」维度（独立世界 + 星空演出 + 邀请 + 奖励掉落）的调度。按用户要求
 * <b>星光庭院不移植</b>，所以这里只留下玩偶本体真正需要的两件事：</p>
 * <ol>
 *   <li>服务器停机时清掉玩偶追踪表（否则重开存档会残留旧实体引用）；</li>
 *   <li>把玩家送到一个安全落点的工具方法 —— 玩偶收回 / 传送时用得上。</li>
 * </ol>
 *
 * <p>星光庭院相关的方法（{@code sendPlayerToStarfield}、{@code returnFromStarfield}、
 * {@code tickCompanion}、{@code dropReward}、{@code tryStartCourtyardShow} 等）已整体删除；
 * 对应的维度、ChunkGenerator、SavedData、演出网络包也不再移植。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class TwoIDollService
{
    private TwoIDollService()
    {
    }

    /** 服务器停机：清空玩偶追踪表，避免跨存档残留 */
    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event)
    {
        TwoIDollEntity.clearTracking();
    }

    /**
     * 在 {@code desired} 附近找一个能站人的落点。
     *
     * <p>先看目标点本身，不行就按半径 0..4 由近到远做一次立方搜索，再退化为
     * 9×9 网格按地表高度逐点试。全部失败返回 {@code null}，由调用方决定怎么降级。</p>
     */
    static Vec3 safeLocation(ServerLevel level, ServerPlayer player, Vec3 desired)
    {
        if (!Double.isFinite(desired.x) || !Double.isFinite(desired.y) || !Double.isFinite(desired.z)
                || !level.getWorldBorder().isWithinBounds(BlockPos.containing(desired)))
        {
            return null;
        }
        if (safeAt(level, player, desired))
        {
            return desired;
        }

        BlockPos origin = BlockPos.containing(desired);
        for (int radius = 0; radius <= 4; radius++)
        {
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius)
                    {
                        continue;
                    }
                    for (int dy = -2; dy <= 8; dy++)
                    {
                        Vec3 candidate = Vec3.atBottomCenterOf(origin.offset(dx, dy, dz));
                        if (safeAt(level, player, candidate))
                        {
                            return candidate;
                        }
                    }
                }
            }
        }

        for (int dx = -8; dx <= 8; dx += 4)
        {
            for (int dz = -8; dz <= 8; dz += 4)
            {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                Vec3 candidate = new Vec3(x + 0.5D, y, z + 0.5D);
                if (safeAt(level, player, candidate))
                {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** 该点是否可站立：脚下有碰撞、不是岩浆/仙人掌，头顶不卡 */
    private static boolean safeAt(ServerLevel level, ServerPlayer player, Vec3 pos)
    {
        BlockPos feet = BlockPos.containing(pos);
        if (feet.getY() < level.getMinBuildHeight() + 1 || feet.getY() > level.getMaxBuildHeight() - 3
                || !level.getWorldBorder().isWithinBounds(feet))
        {
            return false;
        }
        BlockPos belowPos = feet.below();
        BlockState below = level.getBlockState(belowPos);
        if (below.getCollisionShape(level, belowPos).isEmpty()
                || below.is(Blocks.MAGMA_BLOCK)
                || below.is(Blocks.CACTUS)
                || below.is(Blocks.POWDER_SNOW)
                || below.is(Blocks.CAMPFIRE)
                || below.is(Blocks.SOUL_CAMPFIRE))
        {
            return false;
        }
        BlockState atFeet = level.getBlockState(feet);
        BlockState atHead = level.getBlockState(feet.above());
        return atFeet.getCollisionShape(level, feet).isEmpty()
                && atHead.getCollisionShape(level, feet.above()).isEmpty();
    }

    /** 把玩家传送到目标点并清掉坠落速度（避免落地摔伤） */
    private static void teleport(ServerPlayer player, ServerLevel target, Vec3 pos, float yaw, float pitch)
    {
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(target, pos.x, pos.y, pos.z, yaw, pitch);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(Vec3.ZERO);
    }

    /** 给玩家发一条玩偶消息（actionBar 决定是动作栏还是聊天栏） */
    static void message(ServerPlayer player, String suffix, boolean actionBar)
    {
        player.displayClientMessage(
                Component.translatable("msg.godofthings.two_i." + suffix), actionBar);
    }
}
