package com.godofthings.compass.explorers.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;

/**
 * 「这个玩家能不能传送过去」的判定（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>两种任一即可：单人存档开了作弊 / 是 OP。上游 {@code ServerLevelData#getAllowCommands()} 在 1.21.1
 * 改名为 {@link ServerLevelData#isAllowCommands()}。</p>
 */
public final class PlayerUtils
{
    private PlayerUtils()
    {
    }

    public static boolean canTeleport(MinecraftServer server, Player player)
    {
        return cheatModeEnabled(server, player) || isOp(player);
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
            ServerOpListEntry userEntry = serverPlayer.getServer().getPlayerList().getOps()
                    .get(player.getGameProfile());
            return userEntry != null;
        }
        return false;
    }
}
