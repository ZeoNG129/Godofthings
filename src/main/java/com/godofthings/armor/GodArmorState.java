package com.godofthings.armor;

import com.godofthings.Godofthings;
import com.godofthings.handler.GodArmorHandler;
import net.minecraft.world.entity.player.Player;

/**
 * 神之套装功能开关的读写入口。
 * <p>
 * 服务端：权威值存在玩家的 {@code godofthings:armor_features} 数据附件里（随玩家存档持久化、
 * 死亡复制），只由服务端写入。
 * <p>
 * 客户端：不写附件，用一个静态镜像承接服务端推来的值（{@code ArmorSyncPayload}）。
 * 这样界面绘制与客户端侧效果（熔岩可视等）读到的都是同一份数据，且不依赖附件同步时序。
 */
public final class GodArmorState
{
    /** 客户端镜像（服务端在登录、每次改动、客户端主动请求时会推送） */
    private static volatile int clientMask = GodArmorFeatures.ALL;

    private GodArmorState() {}

    /** 取该玩家当前的开关位图（自动区分物理端）。 */
    public static int get(Player player)
    {
        if (player.level().isClientSide)
        {
            return clientMask;
        }
        return GodArmorFeatures.sanitize(player.getData(Godofthings.ARMOR_FEATURES));
    }

    /** 服务端写入（只允许服务端调用；客户端调用不会生效也不该调用）。 */
    public static void set(Player player, int mask)
    {
        player.setData(Godofthings.ARMOR_FEATURES, GodArmorFeatures.sanitize(mask));
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
