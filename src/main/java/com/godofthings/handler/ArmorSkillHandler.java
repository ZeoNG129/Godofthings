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
        if (tickCounter % 20 == 0 && isActive(player)
                && ArmorSkillEngine.isOn(ArmorSkillData.get(player), ArmorSkills.UNBREAKABLE))
        {
            repairInventory(player);
        }
        // 不坏金身：常驻 抗性提升 X / 伤害吸收 C / 抗火 V（无限时长，HUD 不倒数）
        if (tickCounter % 20 == 0 && isActive(player)
                && ArmorSkillEngine.isOn(ArmorSkillData.get(player), ArmorSkills.ULT_GOLDEN))
        {
            applyGolden(player);
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

        // ② 我方被攻击：奥术神体（魔法伤害 -35%）
        if (event.getEntity() instanceof ServerPlayer victim && isActive(victim))
        {
            Map<String, Integer> victimLevels = ArmorSkillData.get(victim);
            if (ArmorSkillEngine.isOn(victimLevels, ArmorSkills.ULT_ARCANE_BODY)
                    && isMagicDamage(event.getSource()))
            {
                event.setAmount((float) (event.getAmount() * (1.0 - ArmorSkillEngine.ARCANE_REDUCTION)));
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
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !isActive(player))
        {
            return;
        }
        Map<String, Integer> levels = ArmorSkillData.get(player);
        double mult = ArmorSkillEngine.mobDropMultiplier(levels) * ArmorSkillEngine.lootBombMultiplier(levels);
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
            double eggChance = ArmorSkillEngine.spawnEggChance(levels);
            if (eggChance > 0 && player.getRandom().nextDouble() < eggChance)
            {
                net.minecraft.world.item.Item egg = ArmorSkillEngine.spawnEggFor(type);
                if (egg != null)
                {
                    event.getDrops().add(newDrop(event.getEntity(), new net.minecraft.world.item.ItemStack(egg)));
                }
            }
            double headChance = ArmorSkillEngine.headDropChance(levels);
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
        if (!(event.getBreaker() instanceof ServerPlayer player) || !isActive(player))
        {
            return;
        }
        Map<String, Integer> levels = ArmorSkillData.get(player);
        double mult = ArmorSkillEngine.blockDropMultiplier(levels) * ArmorSkillEngine.lootBombMultiplier(levels);
        if (mult <= 1.0)
        {
            return;
        }
        for (net.minecraft.world.entity.item.ItemEntity drop : event.getDrops())
        {
            growStack(drop.getItem(), mult);
        }
        // 自动熔炼：把可熔炼的掉落物换成熔炼产物
        if (ArmorSkillEngine.isOn(levels, ArmorSkills.AUTO_SMELT))
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
        if (!(event.getAttackingPlayer() instanceof ServerPlayer player) || !isActive(player))
        {
            return;
        }
        double mult = ArmorSkillEngine.xpMultiplier(ArmorSkillData.get(player));
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
}
