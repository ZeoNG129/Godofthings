package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkillEngine;
import com.godofthings.network.ArmorSkillMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 神之套装技能树的运行时：属性重挂 + 事件型技能（回血 / 暴击 / 吸血 / 荆棘 / 破甲）。
 * <p>
 * 与套装其余功能一致：<b>技能只在穿齐全套神之护甲时生效</b>；脱下立即回收全部属性加成。
 * <p>
 * 数值与机制移植自 Zifeng Skill Tree（子枫的百宝箱，Copyright (c) 2026 zifeng, MIT），
 * 已改为"无技能点、无前置、点击即解锁"。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class ArmorSkillHandler
{
    /** 每 10 tick 巡检一次"技能表 / 是否穿齐全套"是否变化（变了才重挂，避免每 tick 动属性） */
    private static final int CHECK_INTERVAL = 10;
    /** 回血每 20 tick（1 秒）一次 */
    private static final int REGEN_INTERVAL = 20;

    /** 上次重挂时的状态指纹（避免每 tick 重复挂修饰符） */
    private static final Map<UUID, Integer> LAST_SIGNATURE = new HashMap<>();
    private static int tickCounter = 0;

    /** 技能是否对当前玩家生效：穿齐全套神之护甲 */
    public static boolean isActive(Player player)
    {
        return GodArmorHandler.isFullSetWorn(player);
    }

    /** 立即按当前状态重挂属性（技能变更 / 穿脱套装 / 登录重生时调用）。 */
    public static void refresh(ServerPlayer player)
    {
        Map<String, Integer> levels = ArmorSkillData.get(player);
        if (isActive(player))
        {
            ArmorSkillEngine.applyAll(player, levels);
        }
        else
        {
            ArmorSkillEngine.removeAll(player);
        }
        LAST_SIGNATURE.put(player.getUUID(), signatureOf(player, levels));
    }

    private static int signatureOf(Player player, Map<String, Integer> levels)
    {
        return levels.hashCode() * 31 + (isActive(player) ? 1 : 0);
    }

    // ---- 登录 / 重生 / 跨维度 ----

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            refresh(player);
            ArmorSkillMessages.sendSync(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            refresh(player);
            ArmorSkillMessages.sendSync(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            refresh(player);
            ArmorSkillMessages.sendSync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        // 巡检：技能表或"是否穿齐全套"变化时重挂（穿脱套装 / 其它途径改技能都能收敛）
        if (++tickCounter % CHECK_INTERVAL == 0)
        {
            Map<String, Integer> levels = ArmorSkillData.get(player);
            int sig = signatureOf(player, levels);
            Integer last = LAST_SIGNATURE.get(player.getUUID());
            if (last == null || last != sig)
            {
                refresh(player);
            }
        }
        // 回血（生生不息 + 生生真解）
        if (tickCounter % REGEN_INTERVAL == 0 && isActive(player))
        {
            double regen = ArmorSkillEngine.regenPerSecond(ArmorSkillData.get(player));
            if (regen > 0 && player.getHealth() < player.getMaxHealth())
            {
                player.heal((float) regen);
            }
        }
    }

    // ---- 攻击侧：暴击 / 破甲 ----

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event)
    {
        // ① 我方攻击目标：暴击 + 破甲增伤
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && attacker != event.getEntity()
                && isActive(attacker))
        {
            Map<String, Integer> levels = ArmorSkillData.get(attacker);
            double multiplier = 1.0 + ArmorSkillEngine.armorPenPercent(levels);
            double critChance = ArmorSkillEngine.critChance(levels);
            if (critChance > 0 && attacker.getRandom().nextDouble() < critChance)
            {
                multiplier *= ArmorSkillEngine.critMultiplier(levels);
            }
            if (multiplier != 1.0)
            {
                event.setAmount((float) (event.getAmount() * multiplier));
            }
        }

        // ② 我方被攻击：荆棘反伤
        if (event.getEntity() instanceof ServerPlayer victim
                && isActive(victim)
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker != victim)
        {
            double thorns = ArmorSkillEngine.thornsDamage(ArmorSkillData.get(victim));
            if (thorns > 0)
            {
                attacker.hurt(victim.damageSources().thorns(victim), (float) thorns);
            }
        }
    }

    // ---- 吸血（伤害结算后） ----

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event)
    {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || attacker == event.getEntity())
        {
            return;
        }
        if (!isActive(attacker))
        {
            return;
        }
        double rate = ArmorSkillEngine.lifestealRate(ArmorSkillData.get(attacker));
        if (rate <= 0)
        {
            return;
        }
        float heal = (float) (event.getNewDamage() * rate);
        if (heal > 0 && attacker.getHealth() < attacker.getMaxHealth())
        {
            attacker.heal(heal);
        }
    }
}
