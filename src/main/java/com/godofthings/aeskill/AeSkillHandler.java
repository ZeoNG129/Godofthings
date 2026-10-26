package com.godofthings.aeskill;

import com.godofthings.Godofthings;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.aeskill.divine.DivineGrace;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;

/**
 * 神之AE 技能的服务端接线：把玩家技能表同步到效果层，并每 tick 驱动「神明之力」。
 *
 * <p>效果层（{@link AeSkillEffects}）是 AE2 侧 mixin 唯一读取的地方，因此只要玩家技能变化
 * 就必须立刻刷新，否则会出现「界面上开了但机器没变快」。</p>
 *
 * <p>刷新时机：登录 / 重生 / 跨维度 / 每 20 tick 巡检（技能是开关式的，改动通过
 * {@code ArmorSkillMessages} 落地，巡检兜住漏网的情况），玩家退出时移除。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class AeSkillHandler
{
    /** 巡检间隔（tick）：技能改动不频繁，1 秒一次足够，也不必每 tick 扫全体玩家。 */
    private static final int SYNC_INTERVAL = 20;

    private AeSkillHandler() {}

    // ---- 玩家技能表 → 效果层 ----

    /** 把该玩家的 5 个 AE 技能等级推给效果层（未开启视为 0）。 */
    public static void syncPlayer(Player player)
    {
        if (player.level().isClientSide || !(player instanceof ServerPlayer))
        {
            return;
        }
        Map<String, Integer> levels = ArmorSkillData.get(player);
        AeSkillEffects.update(player.getUUID(),
                ArmorSkillData.effectiveLevel(levels, AeSkills.ASSEMBLER_SPEED),
                ArmorSkillData.effectiveLevel(levels, AeSkills.CPU_SPEED),
                ArmorSkillData.effectiveLevel(levels, AeSkills.ASSEMBLER_INFINITE),
                ArmorSkillData.effectiveLevel(levels, AeSkills.CPU_INFINITE),
                ArmorSkillData.effectiveLevel(levels, AeSkills.DIVINE_MACHINE));
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event)
    {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        AeSkillEffects.remove(event.getEntity().getUUID());
    }

    // ---- 每 tick 驱动 ----

    /**
     * tick 开始：重置共享时间预算。
     *
     * <p><b>必须在 {@code Pre} 且优先级 HIGHEST</b>——AE2 的合成 tick 跑在 tick 末尾
     * （{@code TickHandler.onServerTickEnd}），预算必须在那之前归零；放到 {@code Post} 会在
     * 合成已经跑完之后才重置，等于瞬时合成拿不到本 tick 的共享额度，只能吃 1ms 的默认切片，
     * 表现为「比原版慢、偶尔还要等」。</p>
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onServerTickPre(ServerTickEvent.Pre event)
    {
        TickBudget.markTickStart();
    }

    /** tick 末尾：同步技能表 + 驱动神明之力。 */
    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event)
    {
        MinecraftServer server = event.getServer();
        // 每 tick 同步（与原版一致）：技能一改，当 tick 就生效，不会出现「刚开技能机器还是旧速度」。
        for (ServerPlayer player : server.getPlayerList().getPlayers())
        {
            syncPlayer(player);
        }
        // 神明之力：给活跃产线上的机器发放额外 tick
        DivineGrace.tick(server);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        AeSkillEffects.clear();
        DivineGrace.reset();
        TickBudget.reset();
    }
}
