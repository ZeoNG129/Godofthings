package com.godofthings.wondrous.content.travel;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

import java.util.Optional;

/**
 * 独立传送的短距闪现实现（照抄上游 StandaloneBlink；[本地改动] 上游委托基 mod
 * BeefTeleportHandler 的原生 helper，本模组直接用这份自带实现——它本就是
 * 基 mod 处理器的逐行副本，落点计算改编自 Ender IO TravelHandler）。
 * 不依赖任何手持物品、标签或背包状态。
 */
final class StandaloneBlink
{
    private static final int BLINK_RANGE = 24;

    private StandaloneBlink()
    {
    }

    static InteractionResult tryShortTeleport(Level level, Player player)
    {
        Optional<Vec3> target = teleportPosition(level, player);
        if (target.isEmpty())
        {
            return InteractionResult.PASS;
        }

        // 服务端执行真正的传送；客户端只确认本地碰撞视野里存在合法落点，
        // 让交互包正常发出。
        if (level.isClientSide())
        {
            return InteractionResult.SUCCESS;
        }

        Optional<Vec3> eventTarget = postTeleportEvent(player, target.get());
        if (eventTarget.isEmpty())
        {
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        Vec3 destination = eventTarget.get();
        player.teleportTo(destination.x(), destination.y(), destination.z());
        if (player instanceof ServerPlayer serverPlayer)
        {
            serverPlayer.connection.resetPosition();
        }
        player.resetFallDistance();
        if (player.isInWall())
        {
            player.setPose(Pose.SWIMMING);
        }
        player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private static Optional<Vec3> teleportPosition(Level level, Player player)
    {
        BlockPos target = null;
        double floorHeight = 0.0D;

        Vec3 playerPosition = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = playerPosition.add(look.scale(BLINK_RANGE));
        ClipContext clipContext = new ClipContext(
                playerPosition,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                CollisionContext.empty());
        BlockHitResult hit = level.clip(clipContext);

        if (hit.getType() == HitResult.Type.MISS)
        {
            target = hit.getBlockPos();
        }
        else if (hit.getType() == HitResult.Type.BLOCK)
        {
            Direction direction = hit.getDirection();
            if (direction == Direction.UP)
            {
                target = hit.getBlockPos();
            }
            else if (direction == Direction.DOWN)
            {
                target = hit.getBlockPos().below((int) Math.ceil(player.getBbHeight()));
            }
            else
            {
                target = hit.getBlockPos().relative(direction);
                if (level.getBlockState(target).getCollisionShape(level, target).isEmpty())
                {
                    target = target.below();
                }
            }
        }

        // 第一段碰撞很近时，穿过它继续找下一个开放位置。
        if (playerPosition.distanceToSqr(hit.getLocation()) < 9.0D)
        {
            Vec3 traverseFrom = hit.getLocation().add(look.scale(0.01D));
            BlockPos failPosition = new BlockPos(0, Integer.MAX_VALUE, 0);
            boolean aimingUp = look.y > 0.5D;
            BlockPos traversedTarget = BlockGetter.traverseBlocks(
                    traverseFrom,
                    end,
                    clipContext,
                    (traverseContext, traversePosition) -> {
                        if (!aimingUp)
                        {
                            BlockPos below = traversalCheck(level, traversePosition.below());
                            if (below != null)
                            {
                                return below;
                            }
                        }
                        return traversalCheck(level, traversePosition);
                    },
                    failContext -> failPosition);
            if (traversedTarget != failPosition)
            {
                target = traversedTarget.immutable();
            }
        }

        if (target != null)
        {
            Optional<Double> ground = isTeleportPositionClear(level, target.below());
            if (ground.isPresent())
            {
                floorHeight = ground.get();
            }
            else
            {
                target = null;
            }
        }

        if (target == null || player.blockPosition().distManhattan(target) < 2)
        {
            return Optional.empty();
        }
        return Optional.of(Vec3.atBottomCenterOf(target).add(0.0D, floorHeight, 0.0D));
    }

    private static BlockPos traversalCheck(Level level, BlockPos position)
    {
        BlockState state = level.getBlockState(position);
        if (state.getCollisionShape(level, position).isEmpty()
                && isTeleportPositionClear(level, position.below()).isPresent())
        {
            return position;
        }
        return null;
    }

    private static Optional<Double> isTeleportPositionClear(BlockGetter level, BlockPos target)
    {
        if (level.isOutsideBuildHeight(target))
        {
            return Optional.empty();
        }

        BlockPos above = target.above();
        double height = level.getBlockState(above).getCollisionShape(level, above).max(Direction.Axis.Y);
        if (height <= 0.2D)
        {
            return Optional.of(Math.max(height, 0.0D));
        }

        above = above.above();
        if (level.getBlockState(above).getCollisionShape(level, above).isEmpty())
        {
            return Optional.of(Math.max(height, 0.0D));
        }
        return Optional.empty();
    }

    private static Optional<Vec3> postTeleportEvent(Player player, Vec3 target)
    {
        EntityTeleportEvent event = new EntityTeleportEvent(player, target.x(), target.y(), target.z());
        if (NeoForge.EVENT_BUS.post(event).isCanceled())
        {
            return Optional.empty();
        }
        return Optional.of(new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ()));
    }
}
