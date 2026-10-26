package com.godofthings.elevator.network;

import com.godofthings.elevator.block.ElevatorContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 电梯的网络载荷注册与公共校验。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.neoforge.network.NetworkHandler}（MIT），逐行照抄。</p>
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = com.godofthings.Godofthings.MODID)
public final class ElevatorNetwork
{
    private ElevatorNetwork() {}

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1.0.0");

        registrar.playToServer(ElevatorTeleportPayload.TYPE, ElevatorTeleportPayload.STREAM_CODEC,
                ElevatorTeleportHandler::handle);
        registrar.playToServer(SetDirectionalPayload.TYPE, SetDirectionalPayload.STREAM_CODEC,
                SetDirectionalPayload::handle);
        registrar.playToServer(SetArrowPayload.TYPE, SetArrowPayload.STREAM_CODEC,
                SetArrowPayload::handle);
        registrar.playToServer(RemoveCamoPayload.TYPE, RemoveCamoPayload.STREAM_CODEC,
                RemoveCamoPayload::handle);
        registrar.playToServer(SetFacingPayload.TYPE, SetFacingPayload.STREAM_CODEC,
                SetFacingPayload::handle);
    }

    /**
     * 设置类数据包的基本校验：玩家有效、位置已加载、且玩家当前打开的正是那个电梯的界面。
     * 防止客户端伪造坐标修改任意方块。
     */
    public static boolean isBadClientPacket(Player player, BlockPos pos)
    {
        if (player == null || player.isDeadOrDying() || player.isRemoved())
        {
            return true;
        }
        Level world = player.level();
        if (!world.isLoaded(pos))
        {
            return true;
        }
        if (!(player.containerMenu instanceof ElevatorContainer container))
        {
            return true;
        }
        return !container.getPos().equals(pos);
    }
}
