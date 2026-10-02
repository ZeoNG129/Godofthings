package com.godofthings.note;

import com.godofthings.Godofthings;
import com.godofthings.network.GodNoteMessages;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/**
 * 「自动更名」的服务端驱动：开着自动更名的便签，名字固定为当日日期（MM.dd）。
 *
 * <p>两个触发点：
 * <ul>
 *   <li><b>登录时</b> —— 玩家今天才进游戏，如果名字还停在昨天，进游戏那一刻就换成今天
 *       （例：昨天是 10.02，今天进游戏 → 10.03）</li>
 *   <li><b>挂机跨零点</b> —— 每 {@link #CHECK_INTERVAL} tick 检查一次，游戏一直开着跨天也会换</li>
 * </ul>
 * 另外客户端每次上传便签时，{@link GodNoteData#put} 也会强制执行一次，三处都兜到。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class NoteAutoName
{
    /** 检查间隔（tick）：1200 = 60 秒，跨零点最多晚一分钟换名 */
    private static final int CHECK_INTERVAL = 1200;

    private static int tickCounter;

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            refresh(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        if (++tickCounter < CHECK_INTERVAL)
        {
            return;
        }
        tickCounter = 0;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers())
        {
            refresh(player);
        }
    }

    /** 名字该换就换：变了才落库 + 下发（没变就什么也不做，零开销） */
    private static void refresh(ServerPlayer player)
    {
        GodNoteData data = GodNoteData.get(player.server);
        UUID id = player.getUUID();
        NoteBook book = data.book(id);
        if (book.applyAutoName())
        {
            data.setDirty();
            GodNoteMessages.sendSync(player);
        }
    }
}
