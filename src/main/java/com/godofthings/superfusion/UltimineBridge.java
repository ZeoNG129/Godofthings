package com.godofthings.superfusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

/**
 * FTB Ultimine 兼容（移植自 Mekanism-Card 的 {@code UltimineCompat}）。
 * <p>
 * 装了 FTB Ultimine 且玩家按住连锁键时，融合卡可以一次处理整片连锁选中的方块。
 * 全程反射访问，没装该模组时所有方法都返回空，调用方自然退化成单点操作。
 */
public final class UltimineBridge
{
    @Nullable
    private static final Class<?> ULTIMINE_CLASS = loadClass("dev.ftb.mods.ftbultimine.FTBUltimine");
    @Nullable
    private static final Class<?> PLAYER_DATA_CLASS = loadClass("dev.ftb.mods.ftbultimine.FTBUltiminePlayerData");
    @Nullable
    private static final Class<?> SERVER_CONFIG_CLASS =
            loadClass("dev.ftb.mods.ftbultimine.config.FTBUltimineServerConfig");

    @Nullable
    private static final Method GET_INSTANCE = findMethod(ULTIMINE_CLASS, "getInstance");
    @Nullable
    private static final Field LEGACY_INSTANCE_FIELD = findField(ULTIMINE_CLASS, "instance");
    @Nullable
    private static final Method GET_OR_CREATE_PLAYER_DATA =
            findMethod(ULTIMINE_CLASS, "getOrCreatePlayerData", net.minecraft.world.entity.player.Player.class);
    @Nullable
    private static final Method CLEAR_CACHE = findMethod(PLAYER_DATA_CLASS, "clearCache");
    @Nullable
    private static final Method IS_PRESSED = findMethod(PLAYER_DATA_CLASS, "isPressed");
    @Nullable
    private static final Method UPDATE_BLOCKS = findMethod(PLAYER_DATA_CLASS, "updateBlocks",
            ServerPlayer.class, BlockPos.class, Direction.class, boolean.class, int.class);
    @Nullable
    private static final Method HAS_CACHED_POSITIONS = findMethod(PLAYER_DATA_CLASS, "hasCachedPositions");
    @Nullable
    private static final Method CACHED_POSITIONS = findMethod(PLAYER_DATA_CLASS, "cachedPositions");
    @Nullable
    private static final Method GET_MAX_BLOCKS =
            findMethod(SERVER_CONFIG_CLASS, "getMaxBlocks", ServerPlayer.class);

    private UltimineBridge()
    {
    }

    /** 玩家是否正按住连锁键 */
    public static boolean isPressed(ServerPlayer player)
    {
        Object playerData = getPlayerData(player);
        if (playerData == null || IS_PRESSED == null)
        {
            return false;
        }
        try
        {
            return Boolean.TRUE.equals(IS_PRESSED.invoke(playerData));
        }
        catch (ReflectiveOperationException ignored)
        {
            return false;
        }
    }

    /** 取连锁范围内的方块坐标；没装或未按连锁键时返回空集合 */
    public static Collection<BlockPos> getCachedPositions(ServerPlayer player, BlockPos origin, Direction face)
    {
        Object playerData = getPlayerData(player);
        if (playerData == null || CLEAR_CACHE == null || UPDATE_BLOCKS == null
                || HAS_CACHED_POSITIONS == null || CACHED_POSITIONS == null || GET_MAX_BLOCKS == null)
        {
            return Collections.emptyList();
        }
        try
        {
            CLEAR_CACHE.invoke(playerData);
            Object maxBlocks = GET_MAX_BLOCKS.invoke(null, player);
            if (!(maxBlocks instanceof Integer max))
            {
                return Collections.emptyList();
            }
            UPDATE_BLOCKS.invoke(playerData, player, origin, face, false, max);
            if (!Boolean.TRUE.equals(HAS_CACHED_POSITIONS.invoke(playerData)))
            {
                return Collections.emptyList();
            }
            Object positions = CACHED_POSITIONS.invoke(playerData);
            if (positions instanceof Collection<?> collection)
            {
                return collection.stream()
                        .filter(BlockPos.class::isInstance)
                        .map(BlockPos.class::cast)
                        .toList();
            }
        }
        catch (ReflectiveOperationException ignored)
        {
        }
        return Collections.emptyList();
    }

    @Nullable
    private static Object getPlayerData(ServerPlayer player)
    {
        if (GET_OR_CREATE_PLAYER_DATA == null)
        {
            return null;
        }
        try
        {
            Object instance = GET_INSTANCE != null
                    ? GET_INSTANCE.invoke(null)
                    : LEGACY_INSTANCE_FIELD == null ? null : LEGACY_INSTANCE_FIELD.get(null);
            return instance == null ? null : GET_OR_CREATE_PLAYER_DATA.invoke(instance, player);
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }

    @Nullable
    private static Class<?> loadClass(String className)
    {
        try
        {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        }
        catch (Throwable ignored)
        {
            try
            {
                return Class.forName(className);
            }
            catch (Throwable ignoredAgain)
            {
                return null;
            }
        }
    }

    @Nullable
    private static Field findField(@Nullable Class<?> type, String name)
    {
        if (type == null)
        {
            return null;
        }
        try
        {
            return type.getField(name);
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }

    @Nullable
    private static Method findMethod(@Nullable Class<?> type, String name, Class<?>... parameterTypes)
    {
        if (type == null)
        {
            return null;
        }
        try
        {
            return type.getMethod(name, parameterTypes);
        }
        catch (ReflectiveOperationException ignored)
        {
            return null;
        }
    }
}
