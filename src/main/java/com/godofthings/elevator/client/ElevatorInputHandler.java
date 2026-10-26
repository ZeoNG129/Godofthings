package com.godofthings.elevator.client;

import com.godofthings.Godofthings;
import com.godofthings.elevator.ElevatorBlockBase;
import com.godofthings.elevator.ElevatorConfig;
import com.godofthings.elevator.network.ElevatorNetwork;
import com.godofthings.elevator.network.ElevatorTeleportPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端输入检测：按跳跃上行、按潜行下行。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.ElevatorHandler}（MIT），逐行对照编写。</p>
 *
 * <p><b>注意</b>：本工程没有 {@code runClient} 任务，客户端侧问题只能靠静态检查 + 实机验证；
 * 本类只在 {@link Dist#CLIENT} 注册，且唯一的跨端调用是发网络包。</p>
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = Godofthings.MODID)
public final class ElevatorInputHandler
{
    private static boolean lastSneaking;
    private static boolean lastJumping;

    private ElevatorInputHandler() {}

    @SubscribeEvent
    public static void onKey(InputEvent.Key event)
    {
        handleInput();
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Post event)
    {
        handleInput();
    }

    private static void handleInput()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || !player.isAlive() || player.input == null)
        {
            return;
        }

        boolean sneaking = player.input.shiftKeyDown;
        if (lastSneaking != sneaking)
        {
            lastSneaking = sneaking;
            if (sneaking)
            {
                tryTeleport(player, Direction.DOWN);
            }
        }

        boolean jumping = player.input.jumping;
        if (lastJumping != jumping)
        {
            lastJumping = jumping;
            if (jumping)
            {
                tryTeleport(player, Direction.UP);
            }
        }
    }

    private static void tryTeleport(LocalPlayer player, Direction facing)
    {
        Level world = player.level();

        BlockPos fromPos = getOriginElevator(player);
        if (fromPos == null)
        {
            return;
        }

        BlockPos.MutableBlockPos toPos = fromPos.mutable();

        ElevatorBlockBase fromElevator = (ElevatorBlockBase) world.getBlockState(fromPos).getBlock();

        while (true)
        {
            toPos.setY(toPos.getY() + facing.getStepY());
            if (world.isOutsideBuildHeight(toPos)
                    || Math.abs(toPos.getY() - fromPos.getY()) > ElevatorConfig.range())
            {
                break;
            }

            ElevatorBlockBase toElevator = com.godofthings.elevator.network.ElevatorTeleportHandler
                    .getElevator(world.getBlockState(toPos));
            if (toElevator != null
                    && com.godofthings.elevator.network.ElevatorTeleportHandler.isValidPos(world, toPos))
            {
                if (!ElevatorConfig.sameColor() || fromElevator.getColor() == toElevator.getColor())
                {
                    PacketDistributor.sendToServer(new ElevatorTeleportPayload(fromPos, toPos.immutable()));
                    break;
                }
            }
        }
    }

    /** 玩家脚下（含向下 2 格）的第一台电梯。 */
    private static BlockPos getOriginElevator(LocalPlayer player)
    {
        Level world = player.level();
        BlockPos pos = player.blockPosition();

        for (int i = 0; i < 3; i++)
        {
            if (com.godofthings.elevator.network.ElevatorTeleportHandler.getElevator(world.getBlockState(pos)) != null)
            {
                return pos;
            }
            pos = pos.below();
        }
        return null;
    }
}
