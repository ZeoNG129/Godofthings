package com.godofthings.compass.natures.util;

import com.godofthings.compass.natures.NaturesCompassData;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.neoforge.server.permission.PermissionAPI;

/**
 * 「这个玩家能不能传送过去」的判定（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>三种任一即可：单人存档开了作弊 / 是 OP / 有权限节点 {@code godofthings.natures.teleport}。</p>
 */
public final class PlayerUtils
{
    private PlayerUtils()
    {
    }

    public static boolean canTeleport(MinecraftServer server, Player player)
    {
        return cheatModeEnabled(server, player) || isOp(player) || hasPermission(player);
    }

    public static boolean cheatModeEnabled(MinecraftServer server, Player player)
    {
        if (server != null && server.isSingleplayer())
        {
            LevelData levelData = server.getLevel(player.level().dimension()).getLevelData();
            if (levelData instanceof ServerLevelData serverLevelData)
            {
                return serverLevelData.isAllowCommands();
            }
        }
        return false;
    }

    public static boolean isOp(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            ServerOpListEntry userEntry = serverPlayer.getServer().getPlayerList().getOps().get(player.getGameProfile());
            return userEntry != null;
        }
        return false;
    }

    public static boolean hasPermission(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return PermissionAPI.getPermission(serverPlayer, NaturesCompassData.TELEPORT_PERMISSION);
        }
        return false;
    }
}
