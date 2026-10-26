package com.godofthings.elevator.network;

import com.godofthings.elevator.ElevatorBlockBase;
import com.godofthings.elevator.ElevatorConfig;
import com.godofthings.elevator.ElevatorRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.EnumSet;

/**
 * 服务端传送处理：校验请求 + 执行传送。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.network.TeleportHandler}（MIT），逐行对照编写。</p>
 */
public final class ElevatorTeleportHandler
{
    private ElevatorTeleportHandler() {}

    static void handle(final ElevatorTeleportPayload message, IPayloadContext ctx)
    {
        ctx.enqueueWork(() ->
        {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (isBadTeleportPacket(message, player))
            {
                return;
            }

            // 经验消耗（先扣，扣不动就中止）
            if (ElevatorConfig.useXP() && !player.isCreative())
            {
                int xpCost = ElevatorConfig.xpPointsAmount();
                if (getPlayerExperienceProgress(player) - xpCost >= 0 || player.experienceLevel > 0)
                {
                    player.giveExperiencePoints(-xpCost);
                }
                else
                {
                    player.displayClientMessage(
                            Component.translatable("block.godofthings.elevator.missing_xp")
                                    .withStyle(ChatFormatting.RED),
                            true);
                    return;
                }
            }

            if (!(player.level() instanceof ServerLevel world))
            {
                return;
            }

            BlockPos toPos = message.to();
            BlockState toState = world.getBlockState(message.to());

            // 朝向：有方向的电梯使用其朝向，否则保留玩家自身
            final float yaw = toState.getValue(ElevatorBlockBase.DIRECTIONAL)
                    ? toState.getValue(ElevatorBlockBase.FACING).toYRot()
                    : player.getYRot();

            final float pitch = (toState.getValue(ElevatorBlockBase.DIRECTIONAL) && ElevatorConfig.resetPitchDirectional())
                    || (!toState.getValue(ElevatorBlockBase.DIRECTIONAL) && ElevatorConfig.resetPitchNormal())
                    ? 0F
                    : player.getXRot();

            // 水平位置：可选对齐到方块中心
            final double toX, toZ;
            if (ElevatorConfig.precisionTarget())
            {
                toX = toPos.getX() + .5D;
                toZ = toPos.getZ() + .5D;
            }
            else
            {
                toX = player.getX();
                toZ = player.getZ();
            }

            double blockYOffset = toState.getBlockSupportShape(world, toPos).max(Direction.Axis.Y);
            player.teleportTo(world, toX, Math.max(toPos.getY(), toPos.getY() + blockYOffset), toZ,
                    EnumSet.noneOf(RelativeMovement.class), yaw, pitch);
            player.setDeltaMovement(player.getDeltaMovement().multiply(new Vec3(1D, 0D, 1D)));

            world.playSound(null, toPos, ElevatorRegistry.TELEPORT_SOUND.get(), SoundSource.BLOCKS, 1F, 1F);
        });
    }

    /** 请求合法性校验：站的位置、同 XZ、不同 Y、两端都是电梯、目标不窒息、可选同色。 */
    private static boolean isBadTeleportPacket(ElevatorTeleportPayload msg, Player player)
    {
        if (player == null || !player.isAlive())
        {
            return true;
        }

        Level world = player.level();
        BlockPos fromPos = msg.from();
        BlockPos toPos = msg.to();

        if (!world.isLoaded(fromPos) || !world.isLoaded(toPos))
        {
            return true;
        }

        // 玩家必须仍然站在 from 那台电梯上
        final double distanceSq = player.distanceToSqr(Vec3.atCenterOf(fromPos));
        if (distanceSq > 6D)
        {
            return true;
        }

        if (fromPos.getX() != toPos.getX() || fromPos.getZ() != toPos.getZ())
        {
            return true;
        }

        if (fromPos.getY() == toPos.getY())
        {
            return true;
        }

        ElevatorBlockBase fromElevator = getElevator(world.getBlockState(fromPos));
        ElevatorBlockBase toElevator = getElevator(world.getBlockState(toPos));

        if (fromElevator == null || toElevator == null)
        {
            return true;
        }

        if (!isValidPos(world, toPos))
        {
            return true;
        }

        return ElevatorConfig.sameColor() && fromElevator.getColor() != toElevator.getColor();
    }

    private static int getPlayerExperienceProgress(Player player)
    {
        return Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /** 目标格上方不能是窒息方块（否则传送进去会卡死）。 */
    public static boolean isValidPos(BlockGetter world, BlockPos pos)
    {
        return !world.getBlockState(pos.above()).isSuffocating(world, pos);
    }

    public static ElevatorBlockBase getElevator(BlockState blockState)
    {
        return blockState.getBlock() instanceof ElevatorBlockBase elevator ? elevator : null;
    }
}
