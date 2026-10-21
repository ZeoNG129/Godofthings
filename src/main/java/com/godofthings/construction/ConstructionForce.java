package com.godofthings.construction;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 「Ctrl 强制施工」的跨端标记表（只在服务端用）。
 *
 * <p>服务端<b>读不到 Ctrl</b>（原版只同步了 Shift 的 {@code isShiftKeyDown}），所以链路是：
 * 客户端判定 Ctrl → 发 {@code ConstructionForcePayload} → 服务端在这里记一个
 * {@link #WINDOW_TICKS} tick 的标记<b>并当场发起作业</b> → 紧随其后的那一发原版右键
 * （{@code ServerboundUseItemOnPacket}）在 {@link GodConstructionItem#onItemUseFirst} 里
 * 只把标记<b>消费掉</b>并返回 SUCCESS —— 这样方块就没机会开界面了。</p>
 *
 * <p>顺序为什么成立：客户端 {@code MultiPlayerGameMode#performUseItemOn} 先跑
 * {@code Item#onItemUseFirst}（本包里发强制包的地方），包体由外层
 * {@code startPrediction} 在它<b>返回之后</b>才构造并发出，所以强制包一定排在那发
 * {@code ServerboundUseItemOnPacket} 之前；两者又都落到服务端主线程队列里，
 * 按到达顺序执行，标记必然先就位（兜底方案见 {@code GodConstructionItem} 的注释）。</p>
 *
 * <p>标记是「这一下右键已经被强制施工处理过」的一次性令牌：</p>
 * <ul>
 *   <li>{@link #consume} 取出来就删，不会连续挡两次；</li>
 *   <li>超过 {@link #WINDOW_TICKS} 自动作废，并由 {@link #cleanup} 每 tick 清掉
 *       —— 客户端万一没把右键发上来也不会留下永久标记；</li>
 *   <li>玩家下线时 {@link #forget} 清掉，UUID 复用时不会串味。</li>
 * </ul>
 */
public final class ConstructionForce
{
    /** 标记存活多少 tick（客户端同一个 tick 内到达的右键包足够用，留出 1 秒余量） */
    public static final int WINDOW_TICKS = 20;

    /** 玩家 UUID → 标记失效的 tick（口径 = {@link MinecraftServer#getTickCount()}） */
    private static final Map<UUID, Long> FORCE_UNTIL = new HashMap<>();

    private ConstructionForce()
    {
    }

    /** 记一个标记：服务端收到「强制施工」包时调（作业由调用方当场发起） */
    public static void mark(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return;
        }
        FORCE_UNTIL.put(player.getUUID(), server.getTickCount() + (long) WINDOW_TICKS);
    }

    /**
     * 消费标记：只有<b>还没过期</b>的标记才算数（取出来就删，一次 Ctrl 点击只挡一次右键）。
     *
     * @return true = 刚才那次 Ctrl 点击的原版右键包到了，调用方应当返回 SUCCESS 挡住方块交互
     */
    public static boolean consume(Player player)
    {
        UUID playerId = player.getUUID();
        Long until = FORCE_UNTIL.remove(playerId);
        if (until == null)
        {
            return false;
        }
        MinecraftServer server = player.getServer();
        return server != null && until >= server.getTickCount();
    }

    /** 每 tick 清掉过期标记（在 {@link ConstructionJob} 的 ServerTickEvent.Post 里顺手调） */
    static void cleanup(MinecraftServer server)
    {
        if (FORCE_UNTIL.isEmpty())
        {
            return;
        }
        long now = server.getTickCount();
        FORCE_UNTIL.values().removeIf(until -> until < now);
    }

    /** 玩家下线：标记跟着作废 */
    static void forget(Player player)
    {
        FORCE_UNTIL.remove(player.getUUID());
    }
}
