package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkillEngine;
import com.godofthings.armor.skill.ArmorSkills;
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
    /** 常驻药水效果刷新间隔（tick） */
    private static final int EFFECT_INTERVAL = 40;
    /** 常驻效果持续时间（tick）：比刷新间隔长，避免闪烁 */
    private static final int EFFECT_DURATION = 100;

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
        // 属性重挂前先记录生命状况：装备后生命上限会从 20 涨到几万，
        // 而当前血量不会自动跟着涨（原版只在上限下降时 clamp），
        // 会导致血条压缩后只剩不到 1 点（半颗心）。故按"生命比例"同步。
        float beforeHealth = player.getHealth();
        float beforeMax = player.getMaxHealth();

        if (isActive(player))
        {
            ArmorSkillEngine.applyAll(player, levels);
        }
        else
        {
            ArmorSkillEngine.removeAll(player);
        }

        float afterMax = player.getMaxHealth();
        if (afterMax > beforeMax && beforeMax > 0.0f)
        {
            // 上限变高：保持原来的生命百分比（满血→满血，半血→半血）
            float ratio = Math.max(0.0f, Math.min(1.0f, beforeHealth / beforeMax));
            player.setHealth(Math.max(1.0f, ratio * afterMax));
        }
        // 上限变低时不缩放（交回原版 clamp 行为，脱下套装即为满血）

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

        // 常驻药水效果 + 创造飞行（阶段 2）；每 40 tick 刷一次，持续 100 tick 不会闪烁
        if (tickCounter % EFFECT_INTERVAL == 0 && isActive(player))
        {
            Map<String, Integer> levels = ArmorSkillData.get(player);
            for (ArmorSkillEngine.EffectSpec spec : ArmorSkillEngine.passiveEffects(levels))
            {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        spec.effect(), EFFECT_DURATION, spec.amplifier(), true, false, false));
            }
            if (ArmorSkillEngine.removesDarkness(levels) && player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS))
            {
                player.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
            }
            if (ArmorSkillEngine.hasFlight(levels) && !player.getAbilities().mayfly)
            {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        }
        // 万载不磨：定期把物品耐久修满（工具/护甲永不损耗）
        if (tickCounter % 20 == 0 && unbreakableAllowed(player))
        {
            repairInventory(player);
        }
        // 不坏金身：常驻 抗性提升 X / 伤害吸收 C / 抗火 V（无限时长，HUD 不倒数）
        if (tickCounter % 20 == 0 && isActive(player)
                && ArmorSkillEngine.isOn(ArmorSkillData.get(player), ArmorSkills.ULT_GOLDEN))
        {
            applyGolden(player);
        }
        // 机械共鸣：选区操作分批执行（每 tick 最多 256 格，不卡顿）
        processZoneQueue(player);
        // 机械共鸣：选区攻击（半径内敌对生物每 20 tick 受一次你的攻击伤害）
        if (tickCounter % 20 == 0 && isActive(player)
                && ArmorSkillEngine.isOn(ArmorSkillData.get(player), ArmorSkills.MACHINE_ZONE_ATTACK))
        {
            attackHostilesInZone(player);
        }
        // 常驻效果（阶段 2 第三批）：烈焰不侵 / 发光 / 驱法破咒
        if (tickCounter % EFFECT_INTERVAL == 0 && isActive(player))
        {
            Map<String, Integer> lv = ArmorSkillData.get(player);
            if (ArmorSkillEngine.isOn(lv, ArmorSkills.FIRE_PROTECT))
            {
                ensureEffect(player, net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 0);
            }
            if (ArmorSkillEngine.isOn(lv, ArmorSkills.GLOW))
            {
                applyGlow(player);
            }
            if (ArmorSkillEngine.isOn(lv, ArmorSkills.SPELL_PURGE)
                    && player.level().getGameTime() % ArmorSkillEngine.SPELL_PURGE_INTERVAL == 0)
            {
                purgeDebuffs(player);
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
            // 金身真解：自定义「物理减伤」属性，独立乘算层（不改原版护甲公式）
            double reduction = attacker.getAttributeValue(com.godofthings.armor.skill.ModAttributes.DAMAGE_REDUCTION);
            double multiplier = (1.0 - Math.max(0.0, Math.min(1.0, reduction)))
                    * (1.0 + ArmorSkillEngine.armorPenPercent(levels));
            double critChance = ArmorSkillEngine.critChance(levels);
            if (critChance > 0 && attacker.getRandom().nextDouble() < critChance)
            {
                multiplier *= ArmorSkillEngine.critMultiplier(levels);
            }
            if (multiplier != 1.0)
            {
                event.setAmount((float) (event.getAmount() * multiplier));
            }
            // 破法之刃：目标身上每个增益 → 对其伤害 +15%（最多 +60%）
            if (ArmorSkillEngine.isOn(levels, ArmorSkills.SPELLBREAK)
                    && event.getEntity() instanceof LivingEntity buffTarget)
            {
                int beneficial = 0;
                for (net.minecraft.world.effect.MobEffectInstance inst : buffTarget.getActiveEffects())
                {
                    if (inst.getEffect().value().isBeneficial())
                    {
                        beneficial++;
                    }
                }
                multiplier *= 1.0 + ArmorSkillEngine.spellbreakBonus(beneficial);
            }
            // 死神凝视：非玩家目标血量低于 15% 时 30% 概率直接处决
            if (ArmorSkillEngine.isOn(levels, ArmorSkills.ULT_REAPER)
                    && !(event.getEntity() instanceof ServerPlayer)
                    && event.getEntity() instanceof LivingEntity target
                    && target.isAlive()
                    && target.getHealth() / Math.max(1.0f, target.getMaxHealth()) < ArmorSkillEngine.REAPER_THRESHOLD
                    && attacker.getRandom().nextFloat() < ArmorSkillEngine.REAPER_CHANCE)
            {
                event.setAmount(ArmorSkillEngine.REAPER_DAMAGE);
            }
        }

        // ①.5 防护选区：我方攻击半径内的友好生物时取消伤害（机械共鸣）
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && isActive(attacker)
                && ArmorSkillEngine.isOn(ArmorSkillData.get(attacker), ArmorSkills.MACHINE_ZONE_PROTECT)
                && event.getEntity() instanceof LivingEntity target
                && isProtectedFriendly(attacker, target))
        {
            event.setCanceled(true);
            return;
        }

        // ② 我方被攻击：奥术防护（壁垒/真解公式减伤 + 法术抑制 + 适应叠层 + 法术反射）
        if (event.getEntity() instanceof ServerPlayer victim && isActive(victim)
                && isMagicDamage(event.getSource()))
        {
            Map<String, Integer> victimLevels = ArmorSkillData.get(victim);
            // 法术反射：概率把伤害原样反弹给施法者，自身免伤
            if (ArmorSkillEngine.isOn(victimLevels, ArmorSkills.SPELL_REFLECT)
                    && event.getSource().getEntity() instanceof LivingEntity caster
                    && caster != victim
                    && victim.getRandom().nextFloat() < ArmorSkillEngine.SPELL_REFLECT_CHANCE)
            {
                caster.hurt(victim.damageSources().magic(), event.getAmount());
                event.setAmount(0.0f);
            }
            else
            {
                double red = ArmorSkillEngine.magicReduction(victimLevels);
                if (isIndirectDamage(event.getSource()))
                {
                    red = 1.0 - (1.0 - red) * (1.0 - ArmorSkillEngine.dampenReduction(victimLevels));
                }
                // 适应之躯：受击层数（0~60%）再乘算一层
                red = 1.0 - (1.0 - red) * (1.0 - adaptBonus(victim));
                if (red > 0.0)
                {
                    event.setAmount((float) (event.getAmount() * (1.0 - Math.min(0.9999, red))));
                }
            }
            // 受击后叠一层适应
            if (ArmorSkillEngine.isOn(victimLevels, ArmorSkills.ARCANE_ADAPT))
            {
                addAdaptStack(victim);
            }
        }

        // ③ 我方被攻击：荆棘反伤
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

    // ---- 收益侧：掉落倍率 / 战利品爆炸 / 经验倍率（阶段 2） ----

    /** 生物掉落：猎魂丰收 × 财源滚滚（把每个掉落物的数量按倍率放大） */
    @SubscribeEvent
    public static void onLivingDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event)
    {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || !effectAllowed(player, ArmorSkills.MACHINE_MOB_DROP))
        {
            return;
        }
        Map<String, Integer> levels = levelsFor(player);
        double bomb = effectAllowed(player, ArmorSkills.MACHINE_LOOT_BOMB)
                ? ArmorSkillEngine.lootBombMultiplier(levels) : 1.0;
        double mult = ArmorSkillEngine.mobDropMultiplier(levels) * bomb;
        if (mult <= 1.0)
        {
            return;
        }
        for (net.minecraft.world.entity.item.ItemEntity drop : event.getDrops())
        {
            growStack(drop.getItem(), mult);
        }
        // 妖魂凝卵 / 斩首夺颅：按概率额外掉落刷怪蛋与头颅
        if (!(event.getEntity() instanceof ServerPlayer))
        {
            net.minecraft.world.entity.EntityType<?> type = event.getEntity().getType();
            double eggChance = effectAllowed(player, ArmorSkills.MACHINE_SPAWN_EGG)
                    ? ArmorSkillEngine.spawnEggChance(levels) : 0.0;
            if (eggChance > 0 && player.getRandom().nextDouble() < eggChance)
            {
                net.minecraft.world.item.Item egg = ArmorSkillEngine.spawnEggFor(type);
                if (egg != null)
                {
                    event.getDrops().add(newDrop(event.getEntity(), new net.minecraft.world.item.ItemStack(egg)));
                }
            }
            double headChance = effectAllowed(player, ArmorSkills.MACHINE_MOB_HEAD)
                    ? ArmorSkillEngine.headDropChance(levels) : 0.0;
            if (headChance > 0 && player.getRandom().nextDouble() < headChance)
            {
                net.minecraft.world.item.Item head = ArmorSkillEngine.headItemFor(type);
                if (head != null)
                {
                    event.getDrops().add(newDrop(event.getEntity(), new net.minecraft.world.item.ItemStack(head)));
                }
            }
        }
    }

    /** 在实体位置生成一个掉落物实体（用于额外掉落） */
    private static net.minecraft.world.entity.item.ItemEntity newDrop(net.minecraft.world.entity.LivingEntity at,
                                                                     net.minecraft.world.item.ItemStack stack)
    {
        return new net.minecraft.world.entity.item.ItemEntity(at.level(), at.getX(), at.getY() + 0.5, at.getZ(), stack);
    }

    /** 方块掉落：点石成金 × 财源滚滚 + 自动熔炼 */
    @SubscribeEvent
    public static void onBlockDrops(net.neoforged.neoforge.event.level.BlockDropsEvent event)
    {
        if (!(event.getBreaker() instanceof ServerPlayer player)
                || !effectAllowed(player, ArmorSkills.MACHINE_BLOCK_DROP))
        {
            return;
        }
        Map<String, Integer> levels = levelsFor(player);
        double bomb = effectAllowed(player, ArmorSkills.MACHINE_LOOT_BOMB)
                ? ArmorSkillEngine.lootBombMultiplier(levels) : 1.0;
        double mult = ArmorSkillEngine.blockDropMultiplier(levels) * bomb;
        if (mult <= 1.0)
        {
            return;
        }
        for (net.minecraft.world.entity.item.ItemEntity drop : event.getDrops())
        {
            growStack(drop.getItem(), mult);
        }
        // 自动熔炼：把可熔炼的掉落物换成熔炼产物
        if (ArmorSkillEngine.isOn(levels, ArmorSkills.AUTO_SMELT)
                && effectAllowed(player, ArmorSkills.MACHINE_AUTO_SMELT))
        {
            for (net.minecraft.world.entity.item.ItemEntity drop : event.getDrops())
            {
                net.minecraft.world.item.ItemStack in = drop.getItem();
                if (in.isEmpty())
                {
                    continue;
                }
                net.minecraft.world.item.ItemStack out = smeltResult(player, in);
                if (!out.isEmpty())
                {
                    out.setCount(Math.max(1, out.getCount() * in.getCount()));
                    drop.setItem(out);
                }
            }
        }
    }

    /** 经验掉落：经验飞涨 */
    @SubscribeEvent
    public static void onExperienceDrop(net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent event)
    {
        if (!(event.getAttackingPlayer() instanceof ServerPlayer player)
                || !effectAllowed(player, ArmorSkills.MACHINE_XP_GAIN))
        {
            return;
        }
        double mult = ArmorSkillEngine.xpMultiplier(levelsFor(player));
        if (mult > 1.0)
        {
            event.setDroppedExperience((int) Math.min(Integer.MAX_VALUE, Math.round(event.getDroppedExperience() * mult)));
        }
    }

    /** 按倍率放大一个掉落堆叠（数量上限 6400，防止爆栈） */
    private static void growStack(net.minecraft.world.item.ItemStack stack, double mult)
    {
        if (stack.isEmpty())
        {
            return;
        }
        int target = (int) Math.min(6400L, Math.round(stack.getCount() * mult));
        if (target > stack.getCount())
        {
            stack.setCount(target);
        }
    }

    // ---- 阶段 2 第二批：战斗大招 / 掉落生产 / 铁砧 ----

    /** 免死（凤凰涅槃 / 虚空神体）冷却到期时间，按玩家 UUID */
    private static final Map<UUID, Long> UNDYING_UNTIL = new HashMap<>();

    /** 是否魔法伤害（奥术神体用） */
    private static boolean isMagicDamage(net.minecraft.world.damagesource.DamageSource source)
    {
        return source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
    }

    /** 万载不磨：把背包里所有损耗过的物品耐久修满（含护甲与副手） */
    private static void repairInventory(ServerPlayer player)
    {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            net.minecraft.world.item.ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && st.isDamageableItem() && st.getDamageValue() > 0)
            {
                st.setDamageValue(0);
            }
        }
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values())
        {
            net.minecraft.world.item.ItemStack st = player.getItemBySlot(slot);
            if (!st.isEmpty() && st.isDamageableItem() && st.getDamageValue() > 0)
            {
                st.setDamageValue(0);
            }
        }
    }

    /** 不坏金身：常驻 抗性提升 X / 伤害吸收 C / 抗火 V（无限时长；等级不足时补齐） */
    private static void applyGolden(ServerPlayer player)
    {
        ensureEffect(player, net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 9);
        ensureEffect(player, net.minecraft.world.effect.MobEffects.ABSORPTION, 99);
        ensureEffect(player, net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 4);
    }

    private static void ensureEffect(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier)
    {
        net.minecraft.world.effect.MobEffectInstance cur = player.getEffect(effect);
        if (cur == null || cur.getAmplifier() < amplifier)
        {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    effect, Integer.MAX_VALUE, amplifier, false, false, false));
        }
    }

    /** 虚空神体：免疫击退 */
    @SubscribeEvent
    public static void onKnockBack(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player && isActive(player)
                && ArmorSkillEngine.isOn(ArmorSkillData.get(player), ArmorSkills.ULT_VOID_BODY))
        {
            event.setCanceled(true);
        }
    }

    /**
     * 免死：凤凰涅槃（优先）与虚空神体（兜底）。
     * <p>凤凰涅槃：回 50% 血、清空状态、5 秒伤害吸收盾、播放不死图腾动画，冷却 60 秒。
     * <p>虚空神体：同样阻止死亡并回 50% 血，冷却 60 秒。
     */
    @SubscribeEvent
    public static void onLivingDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isActive(player))
        {
            return;
        }
        Map<String, Integer> levels = ArmorSkillData.get(player);
        boolean revive = ArmorSkillEngine.isOn(levels, ArmorSkills.ULT_REVIVE);
        boolean voidBody = ArmorSkillEngine.isOn(levels, ArmorSkills.ULT_VOID_BODY);
        if (!revive && !voidBody)
        {
            return;
        }
        long now = player.level().getGameTime();
        if (now < UNDYING_UNTIL.getOrDefault(player.getUUID(), 0L))
        {
            return; // 冷却中
        }
        event.setCanceled(true);
        player.setHealth(Math.max(1.0f, player.getMaxHealth() * ArmorSkillEngine.REVIVE_HEALTH_RATIO));
        if (revive)
        {
            player.removeAllEffects();
        }
        // 吸收盾代替无敌帧
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.ABSORPTION, 100, 4, false, false, false));
        // 原版不死图腾同款复活动画
        player.level().broadcastEntityEvent(player, (byte) 35);
        UNDYING_UNTIL.put(player.getUUID(), now + ArmorSkillEngine.UNDYING_COOLDOWN);
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "chat.godofthings.armor.undying",
                ArmorSkillEngine.UNDYING_COOLDOWN / 20));
    }

    /** 不朽铭文：铁砧中放入两个相同物品 → 合成带「无法破坏」词条的工具 */
    @SubscribeEvent
    public static void onAnvilUpdate(net.neoforged.neoforge.event.AnvilUpdateEvent event)
    {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !isActive(player)
                || !ArmorSkillEngine.isOn(ArmorSkillData.get(player), ArmorSkills.ULT_UNBREAK_TAG))
        {
            return;
        }
        net.minecraft.world.item.ItemStack left = event.getLeft();
        net.minecraft.world.item.ItemStack right = event.getRight();
        if (left.isEmpty() || right.isEmpty() || !net.minecraft.world.item.ItemStack.isSameItem(left, right))
        {
            return;
        }
        if (left.has(net.minecraft.core.component.DataComponents.UNBREAKABLE))
        {
            return; // 已经是无法破坏
        }
        net.minecraft.world.item.ItemStack out = left.copyWithCount(1);
        out.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,
                new net.minecraft.world.item.component.Unbreakable(true));
        event.setOutput(out);
        event.setCost(1);
    }

    /**
     * 附魔三件套（特殊被动，均在铁砧上操作）：
     * <ul>
     *   <li>随机附魔：右槽 4 青金石 + 1 级经验 → 给左槽物品加一条随机正面附魔</li>
     *   <li>附魔突破：右槽 2 青金石块 + 4 级经验 → 已有附魔各 +1 级（上限 20）</li>
     *   <li>超限附魔：右槽 2 下界之星 + 10 级经验 → 已有附魔各 +1 级（上限 100）</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onAnvilEnchant(net.neoforged.neoforge.event.AnvilUpdateEvent event)
    {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !isActive(player))
        {
            return;
        }
        net.minecraft.world.item.ItemStack left = event.getLeft();
        net.minecraft.world.item.ItemStack right = event.getRight();
        if (left.isEmpty() || right.isEmpty() || !net.minecraft.world.item.enchantment.EnchantmentHelper.canStoreEnchantments(left))
        {
            return;
        }
        Map<String, Integer> levels = ArmorSkillData.get(player);
        boolean over = ArmorSkillEngine.isOn(levels, ArmorSkills.ENCHANT_OVER)
                && right.is(net.minecraft.world.item.Items.NETHER_STAR) && right.getCount() >= 2;
        boolean brk = !over && ArmorSkillEngine.isOn(levels, ArmorSkills.ENCHANT_BREAK)
                && right.is(net.minecraft.world.item.Items.LAPIS_BLOCK) && right.getCount() >= 2;
        boolean rnd = !over && !brk && ArmorSkillEngine.isOn(levels, ArmorSkills.ENCHANT_RANDOM)
                && right.is(net.minecraft.world.item.Items.LAPIS_LAZULI) && right.getCount() >= 4;
        if (!over && !brk && !rnd)
        {
            return;
        }
        net.minecraft.world.item.ItemStack out = left.copyWithCount(1);
        if (rnd)
        {
            var reg = player.server.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            java.util.List<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> pool =
                    reg.holders()
                            .filter(h -> !h.is(net.minecraft.tags.EnchantmentTags.CURSE)
                                    && h.value().canEnchant(left))
                            .toList();
            if (pool.isEmpty())
            {
                return;
            }
            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> pick =
                    pool.get(player.getRandom().nextInt(pool.size()));
            net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(out,
                    m -> m.set(pick, Math.max(1, m.getLevel(pick) + 1)));
        }
        else
        {
            int cap = over ? 100 : 20;
            net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(out, m ->
            {
                for (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> h
                        : new java.util.ArrayList<>(m.keySet()))
                {
                    int cur = m.getLevel(h);
                    if (cur < cap)
                    {
                        m.set(h, cur + 1);
                    }
                }
            });
        }
        event.setOutput(out);
        event.setCost(over ? 10 : (brk ? 4 : 1));
        event.setMaterialCost(over ? 2 : (brk ? 2 : 4));
    }

    /** 无限交易：每次交易后重置该报价的已用次数 */
    @SubscribeEvent
    public static void onTradeWithVillager(net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isActive(player)
                || !ArmorSkillEngine.isOn(ArmorSkillData.get(player), ArmorSkills.UNLIMITED_TRADES))
        {
            return;
        }
        event.getMerchantOffer().resetUses();
    }

    /** 村民大师：右键村民把它提升为大师级（5 级） */
    @SubscribeEvent
    public static void onEntityInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isActive(player)
                || !ArmorSkillEngine.isOn(ArmorSkillData.get(player), ArmorSkills.VILLAGER_MASTER))
        {
            return;
        }
        if (event.getTarget() instanceof net.minecraft.world.entity.npc.Villager villager
                && villager.getVillagerData().getLevel() < 5)
        {
            villager.setVillagerData(villager.getVillagerData().setLevel(5));
            villager.setVillagerXp(0);
        }
    }

    /** 暴食：进食瞬间完成（把使用时长压到 1 tick） */
    @SubscribeEvent
    public static void onUseItemStart(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isActive(player))
        {
            return;
        }
        if (event.getItem().has(net.minecraft.core.component.DataComponents.FOOD)
                && ArmorSkillEngine.isOn(ArmorSkillData.get(player), ArmorSkills.GLUTTONY))
        {
            event.setDuration(1);
        }
    }

    /** 发光：给附近生物挂发光效果（每 40 tick 刷一次，持续 100 tick） */
    private static void applyGlow(ServerPlayer player)
    {
        net.minecraft.world.phys.AABB box = player.getBoundingBox()
                .inflate(ArmorSkillEngine.GLOW_RADIUS);
        for (net.minecraft.world.entity.LivingEntity e : player.level().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class, box))
        {
            if (e != player)
            {
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, EFFECT_DURATION, 0, true, false, false));
            }
        }
    }

    /** 驱法破咒：清除自己与附近友方各一个负面效果 */
    private static void purgeDebuffs(ServerPlayer player)
    {
        purgeOne(player);
        net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(8.0);
        for (net.minecraft.world.entity.player.Player other : player.level().getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class, box))
        {
            if (other != player)
            {
                purgeOne(other);
            }
        }
    }

    private static void purgeOne(net.minecraft.world.entity.LivingEntity entity)
    {
        for (net.minecraft.world.effect.MobEffectInstance inst : entity.getActiveEffects())
        {
            if (!inst.getEffect().value().isBeneficial())
            {
                entity.removeEffect(inst.getEffect());
                return; // 每次只清一个
            }
        }
    }

    /** 是否间接伤害（弹射物 / 法术）——法术抑制只对这类生效 */
    private static boolean isIndirectDamage(net.minecraft.world.damagesource.DamageSource source)
    {
        return source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile
                || source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC);
    }

    // ---- 适应之躯：受击叠层（0~60%），8 秒无受击衰减到 0 ----

    private static final Map<UUID, Integer> ADAPT_STACKS = new HashMap<>();
    private static final Map<UUID, Long> ADAPT_EXPIRE = new HashMap<>();

    private static void addAdaptStack(ServerPlayer player)
    {
        long now = player.level().getGameTime();
        int stacks = ADAPT_STACKS.getOrDefault(player.getUUID(), 0);
        if (now > ADAPT_EXPIRE.getOrDefault(player.getUUID(), 0L))
        {
            stacks = 0; // 过期重新计
        }
        ADAPT_STACKS.put(player.getUUID(), Math.min(30, stacks + 1));
        ADAPT_EXPIRE.put(player.getUUID(), now + 160);
    }

    private static double adaptBonus(ServerPlayer player)
    {
        long now = player.level().getGameTime();
        if (now > ADAPT_EXPIRE.getOrDefault(player.getUUID(), 0L))
        {
            return 0;
        }
        int stacks = ADAPT_STACKS.getOrDefault(player.getUUID(), 0);
        return Math.min(ArmorSkillEngine.ADAPT_MAX, stacks * ArmorSkillEngine.ADAPT_STEP);
    }

    /** 自动熔炼：把方块掉落里可熔炼的物品换成熔炼产物（查原版熔炉配方表） */
    private static net.minecraft.world.item.ItemStack smeltResult(ServerPlayer player, net.minecraft.world.item.ItemStack in)
    {
        var server = player.getServer();
        if (server == null)
        {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        for (var holder : server.getRecipeManager()
                .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMELTING))
        {
            var recipe = holder.value();
            var ings = recipe.getIngredients();
            if (!ings.isEmpty() && ings.get(0).test(in))
            {
                return recipe.getResultItem(server.registryAccess()).copy();
            }
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    // ══════════ 机械共鸣（阶段 3） ══════════

    /** 选区半径（格，方形半径） */
    public static final int ZONE_RADIUS = 8;
    /** 选区操作每 tick 最多处理的方块数（防卡顿） */
    private static final int ZONE_BATCH = 256;

    /** 待执行的选区操作队列（按玩家） */
    private static final Map<UUID, java.util.ArrayDeque<net.minecraft.core.BlockPos>> ZONE_QUEUE = new HashMap<>();
    private static final Map<UUID, Boolean> ZONE_MODE_PLACE = new HashMap<>();

    /**
     * 效果归属的玩家：真玩家 = 自己；假玩家（模拟玩家机器，如数字型采矿机）= 其主人（需在线）。
     * <p>机器以主人 UUID 触发事件，故用 UUID 反查在线主人。
     */
    private static ServerPlayer ownerOf(ServerPlayer player)
    {
        if (!(player instanceof net.neoforged.neoforge.common.util.FakePlayer))
        {
            return player;
        }
        return player.getServer() == null ? null
                : player.getServer().getPlayerList().getPlayer(player.getUUID());
    }

    /**
     * 效果是否允许对本次触发生效（机械共鸣判定，移植自参考模组 {@code isEffectAllowedFor}）。
     * <ul>
     *   <li><b>真玩家</b>：只要穿齐全套即生效，<b>不需要</b>共鸣技能</li>
     *   <li><b>假玩家（机器）</b>：主人需在线且穿齐全套，并<b>开启对应的共鸣技能</b>才允许继承</li>
     * </ul>
     * 关闭共鸣技能立即回收（每次事件实时判定，无持久状态）。
     */
    public static boolean effectAllowed(ServerPlayer player, String machineSkillId)
    {
        if (!(player instanceof net.neoforged.neoforge.common.util.FakePlayer))
        {
            return isActive(player);
        }
        ServerPlayer owner = ownerOf(player);
        if (owner == null || !isActive(owner))
        {
            return false;
        }
        return ArmorSkillEngine.isOn(ArmorSkillData.get(owner), machineSkillId);
    }

    /** 取"生效用的等级表"：真玩家 = 自己；机器 = 主人 */
    private static Map<String, Integer> levelsFor(ServerPlayer player)
    {
        ServerPlayer owner = ownerOf(player);
        return owner == null ? Map.of() : ArmorSkillData.get(owner);
    }

    /** 万载不磨是否生效（真玩家看自己的技能；机器看共鸣开关 + 主人的技能） */
    private static boolean unbreakableAllowed(ServerPlayer player)
    {
        if (player instanceof net.neoforged.neoforge.common.util.FakePlayer)
        {
            return effectAllowed(player, ArmorSkills.MACHINE_UNBREAKABLE)
                    && ArmorSkillEngine.isOn(levelsFor(player), ArmorSkills.UNBREAKABLE);
        }
        return isActive(player) && ArmorSkillEngine.isOn(ArmorSkillData.get(player), ArmorSkills.UNBREAKABLE);
    }

    /** 是否受"防护选区"保护的友好生物（非怪物、非玩家、在自己半径内） */
    private static boolean isProtectedFriendly(ServerPlayer attacker, LivingEntity target)
    {
        if (target == attacker || target instanceof net.minecraft.world.entity.player.Player
                || target instanceof net.minecraft.world.entity.monster.Monster)
        {
            return false;
        }
        return target.distanceToSqr(attacker) <= (double) ZONE_RADIUS * ZONE_RADIUS;
    }

    /** 选区攻击：半径内的敌对生物持续受击（伤害 = 你的攻击力） */
    private static void attackHostilesInZone(ServerPlayer player)
    {
        net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(ZONE_RADIUS);
        float damage = (float) player.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        for (net.minecraft.world.entity.monster.Monster mob
                : player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, box))
        {
            if (mob.isAlive())
            {
                mob.hurt(player.damageSources().playerAttack(player), damage);
            }
        }
    }

    /** 客户端触发选区操作：mode = true 放置 / false 挖掘 */
    public static void startZoneOperation(ServerPlayer player, boolean place)
    {
        String skill = place ? ArmorSkills.MACHINE_ZONE_PLACE : ArmorSkills.MACHINE_ZONE_EXCAVATE;
        if (!isActive(player) || !ArmorSkillEngine.isOn(ArmorSkillData.get(player), skill))
        {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "chat.godofthings.armor.zone.not_learned"), true);
            return;
        }
        java.util.ArrayDeque<net.minecraft.core.BlockPos> queue = new java.util.ArrayDeque<>();
        net.minecraft.core.BlockPos center = player.blockPosition();
        for (int dx = -ZONE_RADIUS; dx <= ZONE_RADIUS; dx++)
        {
            for (int dy = -ZONE_RADIUS; dy <= ZONE_RADIUS; dy++)
            {
                for (int dz = -ZONE_RADIUS; dz <= ZONE_RADIUS; dz++)
                {
                    queue.add(center.offset(dx, dy, dz));
                }
            }
        }
        ZONE_QUEUE.put(player.getUUID(), queue);
        ZONE_MODE_PLACE.put(player.getUUID(), place);
    }

    /** 每 tick 从队列里处理一批方块（放置或挖掘） */
    private static void processZoneQueue(ServerPlayer player)
    {
        java.util.ArrayDeque<net.minecraft.core.BlockPos> queue = ZONE_QUEUE.get(player.getUUID());
        if (queue == null || queue.isEmpty())
        {
            return;
        }
        boolean place = ZONE_MODE_PLACE.getOrDefault(player.getUUID(), false);
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        net.minecraft.world.item.ItemStack held = player.getMainHandItem();
        int done = 0;
        while (done < ZONE_BATCH && !queue.isEmpty())
        {
            net.minecraft.core.BlockPos pos = queue.poll();
            done++;
            if (!level.isLoaded(pos) || pos.equals(player.blockPosition()))
            {
                continue;
            }
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
            if (place)
            {
                if (held.isEmpty() || !state.canBeReplaced())
                {
                    continue; // 没有方块可放
                }
                net.minecraft.world.level.block.state.BlockState toPlace =
                        net.minecraft.world.level.block.Block.byItem(held.getItem()).defaultBlockState();
                if (toPlace.isAir() || !toPlace.canSurvive(level, pos))
                {
                    continue;
                }
                level.setBlock(pos, toPlace, 3);
                if (!player.isCreative())
                {
                    held.shrink(1);
                    if (held.isEmpty())
                    {
                        break; // 方块用完了
                    }
                }
            }
            else
            {
                if (state.isAir() || state.getDestroySpeed(level, pos) < 0)
                {
                    continue; // 空气 / 不可破坏方块（基岩等）跳过
                }
                level.destroyBlock(pos, true, player);
            }
        }
        if (queue.isEmpty())
        {
            ZONE_QUEUE.remove(player.getUUID());
            ZONE_MODE_PLACE.remove(player.getUUID());
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    place ? "chat.godofthings.armor.zone.placed" : "chat.godofthings.armor.zone.dug"), true);
        }
    }
}
