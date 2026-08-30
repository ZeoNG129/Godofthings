package com.godofthings.torcherino.network;

import com.godofthings.torcherino.Torcherino;
import com.godofthings.torcherino.config.TorcherinoConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashSet;
import java.util.Set;

/**
 * 加速火把网络通道：滑块值上行、开屏下行、等级同步下行。
 * 移植自 Torcherino（MIT License）。
 */
public final class TorcherinoNetwork
{
    private static final String VERSION = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryBuild(Torcherino.MOD_ID, "torcherino"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    private static final Set<String> ALLOWED_UUIDS = new HashSet<>();

    private TorcherinoNetwork() {}

    public static void register()
    {
        CHANNEL.registerMessage(0, ValueUpdateMessage.class,
                ValueUpdateMessage::encode, ValueUpdateMessage::decode, ValueUpdateMessage::handle);
        CHANNEL.registerMessage(1, OpenScreenMessage.class,
                OpenScreenMessage::encode, OpenScreenMessage::decode, OpenScreenMessage::handle);
        CHANNEL.registerMessage(2, S2CTierSyncMessage.class,
                S2CTierSyncMessage::encode, S2CTierSyncMessage::decode, S2CTierSyncMessage::handle);
        MinecraftForge.EVENT_BUS.addListener(TorcherinoNetwork::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(TorcherinoNetwork::onPlayerLoggedOut);
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            ALLOWED_UUIDS.add(player.getStringUUID());
            S2CTierSyncMessage message = new S2CTierSyncMessage(Torcherino.getTiers());
            CHANNEL.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            if (TorcherinoConfig.INSTANCE.online_mode.equals("ONLINE"))
            {
                ALLOWED_UUIDS.remove(player.getStringUUID());
            }
        }
    }

    /** 客户端 → 服务端：更新滑块值。 */
    public static void sendUpdate(BlockPos pos, int xRange, int zRange, int yRange, int speed, int redstoneMode)
    {
        CHANNEL.sendToServer(new ValueUpdateMessage(pos, xRange, zRange, yRange, speed, redstoneMode));
    }

    /** 服务端 → 客户端：打开调节界面。 */
    public static void openScreen(ServerPlayer player, BlockPos pos, Component name, int xRange, int zRange, int yRange,
                                  int speed, int redstoneMode)
    {
        CHANNEL.sendTo(new OpenScreenMessage(pos, name, xRange, zRange, yRange, speed, redstoneMode),
                player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static boolean isPlayerOnline(String uuid)
    {
        return ALLOWED_UUIDS.contains(uuid);
    }
}
