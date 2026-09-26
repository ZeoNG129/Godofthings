package com.godofthings.armor.skill;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 机械共鸣「选区」数据（每个玩家 × 每个模式各一个方形选区）。
 * <p>
 * <b>机制移植自 Zifeng Skill Tree（子枫的百宝箱）的木棍选区系统</b>
 * （Copyright (c) 2026 zifeng, MIT License，见 README「第三方代码与许可」）：
 * 手持<b>木棍</b>时，<b>左键点两次</b>取两个角点成区（潜行 + 左键清除），
 * 每个模式（放置 / 挖掘 / 攻击 / 防护）<b>各自记一个选区</b>，互不覆盖。
 *
 * <h3>存储</h3>
 * 服务端权威、按玩家 UUID + 模式索引保存在内存里（不做持久化：重登/重启后需重新框选，
 * 与参考模组的行为一致——它的选区也是"当前会话内有效"的操作区）。
 * 客户端另有一份镜像，用于线框渲染。
 */
public final class ArmorZoneData
{
    /** 模式索引（选区的 key）：放置 / 挖掘 / 攻击 / 防护 */
    public static final int MODE_PLACE = 0;
    public static final int MODE_EXCAVATE = 1;
    public static final int MODE_ATTACK = 2;
    public static final int MODE_PROTECT = 3;
    public static final int MODE_COUNT = 4;

    /** 单次操作允许的最大体积（超过只提示不执行，防止误框选整个区块卡死） */
    public static final long MAX_VOLUME = 131072L;

    /** 选区 = [min, max] 两个角（含端点） */
    public record Zone(BlockPos min, BlockPos max)
    {
        public long volume()
        {
            return (long) (max.getX() - min.getX() + 1)
                    * (max.getY() - min.getY() + 1)
                    * (max.getZ() - min.getZ() + 1);
        }

        public boolean contains(BlockPos pos)
        {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }
    }

    /** 服务端：UUID → (模式 → 选区) */
    private static final Map<UUID, Map<Integer, Zone>> SERVER = new HashMap<>();
    /** 客户端镜像（线框渲染用） */
    private static final Map<Integer, Zone> CLIENT = new HashMap<>();

    private ArmorZoneData() {}

    /** 由两个任意角点构成规范化的选区（min/max 排序） */
    public static Zone of(BlockPos a, BlockPos b)
    {
        return new Zone(
                new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ())),
                new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
    }

    // ---- 服务端 ----

    public static void setServer(ServerPlayer player, int mode, Zone zone)
    {
        SERVER.computeIfAbsent(player.getUUID(), k -> new HashMap<>()).put(mode, zone);
    }

    public static void clearServer(ServerPlayer player, int mode)
    {
        Map<Integer, Zone> map = SERVER.get(player.getUUID());
        if (map != null)
        {
            map.remove(mode);
        }
    }

    public static Zone getServer(ServerPlayer player, int mode)
    {
        Map<Integer, Zone> map = SERVER.get(player.getUUID());
        return map == null ? null : map.get(mode);
    }

    /** 该玩家是否有任意选区（用于选区攻击/防护的判定） */
    public static boolean hasAnyZone(ServerPlayer player)
    {
        Map<Integer, Zone> map = SERVER.get(player.getUUID());
        return map != null && !map.isEmpty();
    }

    // ---- 客户端镜像 ----

    public static void setClient(int mode, Zone zone)
    {
        if (zone == null)
        {
            CLIENT.remove(mode);
        }
        else
        {
            CLIENT.put(mode, zone);
        }
    }

    public static Zone getClient(int mode)
    {
        return CLIENT.get(mode);
    }

    public static void clearClient()
    {
        CLIENT.clear();
    }

    /** 模式的中文/英文名 lang 键后缀 */
    public static String modeSuffix(int mode)
    {
        return switch (mode)
        {
            case MODE_PLACE -> "place";
            case MODE_EXCAVATE -> "excavate";
            case MODE_ATTACK -> "attack";
            default -> "protect";
        };
    }

    /** 模式的显示色（沿用参考模组：放置=金黄 / 挖掘=青 / 攻击=红 / 防护=绿） */
    public static int modeColor(int mode)
    {
        return switch (mode)
        {
            case MODE_PLACE -> 0xFFFFD24A;
            case MODE_EXCAVATE -> 0xFF55FFFF;
            case MODE_ATTACK -> 0xFFFF5555;
            default -> 0xFF55FF55;
        };
    }
}
