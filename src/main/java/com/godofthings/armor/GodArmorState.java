package com.godofthings.armor;

import com.godofthings.Godofthings;
import com.godofthings.handler.GodArmorHandler;
import net.minecraft.world.entity.player.Player;

/**
 * 神之套装功能开关的读写入口。
 * <p>
 * 服务端：权威值存在玩家的 {@code godofthings:armor_features_v2} 数据附件里（随玩家存档持久化、
 * 死亡复制），只由服务端写入。
 * <p>
 * 客户端：不写附件，用一个静态镜像承接服务端推来的值（{@code ArmorSyncPayload}）。
 * 这样界面绘制与客户端侧效果（熔岩可视等）读到的都是同一份数据，且不依赖附件同步时序。
 * <p>
 * <b>老存档迁移（v5.9.0）</b>：16 个开关合并成 8 个之后，旧存档里只有 {@code armor_features}
 * （16 位掩码）。新附件默认 {@code -1} 表示「还没写过」，读到 -1 时把旧掩码按合并规则迁移过来
 * （{@link GodArmorFeatures#migrateLegacy(int)}）并落库；之后一律只读写新附件。
 */
public final class GodArmorState
{
    /** 客户端镜像（服务端在登录、每次改动、客户端主动请求时会推送） */
    private static volatile int clientMask = GodArmorFeatures.ALL;

    private GodArmorState() {}

    /** 取该玩家当前的开关位图（自动区分物理端；老存档在这里做一次迁移） */
    public static int get(Player player)
    {
        if (player.level().isClientSide)
        {
            return clientMask;
        }
        int v2 = player.getData(Godofthings.ARMOR_FEATURES_V2);
        if (v2 >= 0)
        {
            return GodArmorFeatures.sanitize(v2);
        }
        // v5.8.0 及以前：只有 16 位掩码 → 迁移成 8 位并落库，之后不再走这条路
        int migrated = GodArmorFeatures.migrateLegacy(player.getData(Godofthings.ARMOR_FEATURES));
        player.setData(Godofthings.ARMOR_FEATURES_V2, migrated);
        return migrated;
    }

    /** 服务端写入（只允许服务端调用；客户端调用不会生效也不该调用）。 */
    public static void set(Player player, int mask)
    {
        player.setData(Godofthings.ARMOR_FEATURES_V2, GodArmorFeatures.sanitize(mask));
    }

    public static boolean isOn(Player player, int feature)
    {
        return GodArmorFeatures.isOn(get(player), feature);
    }

    /** 「穿齐全套 **且** 该功能开关打开」——绝大多数功能判断都用这个。 */
    public static boolean active(Player player, int feature)
    {
        return GodArmorHandler.isFullSetWorn(player) && isOn(player, feature);
    }

    /** 常驻效果（不占开关位）：「穿齐套装即生效」，例如万民敬仰（村庄英雄）。 */
    public static boolean alwaysOn(Player player)
    {
        return GodArmorHandler.isFullSetWorn(player);
    }

    // ---- 客户端镜像 ----

    public static int getClientMask()
    {
        return clientMask;
    }

    public static void setClientMask(int mask)
    {
        clientMask = GodArmorFeatures.sanitize(mask);
    }
}
