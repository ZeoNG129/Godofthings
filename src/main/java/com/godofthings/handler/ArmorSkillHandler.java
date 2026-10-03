package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkillEngine;
import com.godofthings.armor.skill.ArmorSkills;
import com.godofthings.network.ArmorSkillMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 神之套装技能树的运行时：属性重挂 + 掉落类节点（v5.9.0 起技能表只剩 7 个开关式节点）。
 * <p>
 * 那 7 个节点（神之掉落 / 神之生物 / 神之方块 / 神之经验 / 神之怪蛋 / 神之头颅 / 神之熔炼）
 * 都在本类的掉落事件里实现；原「基础属性 / 特殊增幅 / 机械共鸣 / 魔法增幅」四类的事件型效果
 * （回血 / 暴击 / 吸血 / 荆棘 / 破甲 / 奥术防护 / 死神凝视 / 不坏金身 / 附魔三件套等）已随分类一并删除。
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
        if (effectAllowed(player, ArmorSkills.MACHINE_AUTO_SMELT)
                && ArmorSkillEngine.isOn(levels, ArmorSkills.AUTO_SMELT))
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


    // ══════════ 机器归属判定（原「机械共鸣」的通用部分） ══════════

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
     * 效果是否允许对本次触发生效。
     * <ul>
     *   <li><b>真玩家</b>：只要穿齐全套即生效（不看共鸣开关）</li>
     *   <li><b>假玩家（机器）</b>：主人需在线 + 穿齐全套 + <b>该节点对应的「神之共鸣」开关已打开</b></li>
     * </ul>
     * <p><b>v5.11.0</b>：原「机械共鸣」按用户要求恢复并改名「神之共鸣」—— 7 个开关、<b>默认关</b>，
     * 所以机器默认<b>不</b>继承增幅，要让某台机器吃到某个增幅就在对应节点上把共鸣打开。
     * 每次事件实时判定，无持久状态。</p>
     *
     * @param machineSkillId 对应节点的共鸣开关 id（{@link ArmorSkills#MACHINE_LOOT_BOMB} 等）
     */
    public static boolean effectAllowed(ServerPlayer player, String machineSkillId)
    {
        ServerPlayer owner = ownerOf(player);
        if (owner == null || !isActive(owner))
        {
            return false;
        }
        if (!(player instanceof net.neoforged.neoforge.common.util.FakePlayer))
        {
            return true; // 真玩家：穿齐即生效
        }
        return ArmorSkillData.isEnabled(ArmorSkillData.get(owner), machineSkillId);
    }

    /** 取"生效用的等级表"：真玩家 = 自己；机器 = 主人 */
    private static Map<String, Integer> levelsFor(ServerPlayer player)
    {
        ServerPlayer owner = ownerOf(player);
        return owner == null ? Map.of() : ArmorSkillData.get(owner);
    }
}
