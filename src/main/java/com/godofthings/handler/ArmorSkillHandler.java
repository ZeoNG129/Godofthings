package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkillEngine;
import com.godofthings.armor.skill.ArmorSkills;
import com.godofthings.network.ArmorSkillMessages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;

/**
 * 神之套装技能树的运行时：掉落类节点（v5.9.0 起技能表只剩 7 个开关式节点）。
 * <p>
 * 那 7 个节点（神之掉落 / 神之生物 / 神之方块 / 神之经验 / 神之怪蛋 / 神之头颅 / 神之熔炼）
 * 都在本类的掉落 / 经验事件里实现；原「基础属性 / 特殊增幅 / 机械共鸣 / 魔法增幅」四类的事件型效果
 * （回血 / 暴击 / 吸血 / 荆棘 / 破甲 / 奥术防护 / 死神凝视 / 不坏金身 / 附魔三件套等）已随分类一并删除。
 * <p>
 * <b>v5.12.1</b>：删除属性重挂体系（{@code refresh} / 签名巡检）—— 它唯一的作用是重挂属性修饰符，
 * 而属性修饰符引擎因全部节点无属性效果而空转（详见 {@link ArmorSkillEngine} 类注释）。
 * 现役掉落类节点每次事件实时取等级表，无需任何预重挂。
 * <p>
 * 与套装其余功能一致：<b>技能只在穿齐全套神之护甲时生效</b>。
 * <p>
 * 数值与机制移植自 Zifeng Skill Tree（子枫的百宝箱，Copyright (c) 2026 zifeng, MIT），
 * 已改为"无技能点、无前置、点击即解锁"。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class ArmorSkillHandler
{
    /** 技能是否对当前玩家生效：穿齐全套神之护甲 */
    public static boolean isActive(Player player)
    {
        return GodArmorHandler.isFullSetWorn(player);
    }

    // ---- 登录 / 重生 / 跨维度：把权威技能表推给客户端 ----

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            ArmorSkillMessages.sendSync(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            ArmorSkillMessages.sendSync(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
        {
            ArmorSkillMessages.sendSync(player);
        }
    }

    // ---- 收益侧：掉落倍率 / 战利品爆炸 / 经验倍率 ----

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
        return smeltResult(player.getServer(), in);
    }

    /** 自动熔炼（机器直查重载）：按原版熔炉配方表把输入换成产物（无配方返回空）。 */
    public static net.minecraft.world.item.ItemStack smeltResult(@org.jetbrains.annotations.Nullable net.minecraft.server.MinecraftServer server,
                                                                 net.minecraft.world.item.ItemStack in)
    {
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

    // ══════════ 机器共鸣直查（v5.15.4：不经过 FakePlayer 事件的机器用） ══════════
    // 适用：矿机（方块/熔炼）、资源三机 / 掉落机 / 怪蛋机（掉落）、吸收器（经验）——
    // 它们不产生 LivingDrops / BlockDrops / 经验事件，无法走上面的 effectAllowed 链路。

    /** 共鸣基础判定：主人在线 + 穿齐全套 + 对应共鸣开关打开；返回主人的等级表，未生效返回 null。 */
    private static Map<String, Integer> resonantLevels(ServerLevel level, UUID owner, String machineSkillId)
    {
        if (owner == null)
        {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null || !isActive(player))
        {
            return null; // 主人不在线或未穿齐全套
        }
        Map<String, Integer> levels = ArmorSkillData.get(player);
        return ArmorSkillData.isEnabled(levels, machineSkillId) ? levels : null;
    }

    /** 共鸣直查（机器直连用）：主人在线 + 穿齐全套 + 对应共鸣开关开（不经过 FakePlayer 事件的机器判定入口）。 */
    public static boolean resonanceFor(ServerLevel level, UUID owner, String machineSkillId)
    {
        return resonantLevels(level, owner, machineSkillId) != null;
    }

    /** 共鸣·掉落（战利品爆炸）倍率；未生效 = 1。资源三机 / 掉落机 / 怪蛋机 / 矿机用。 */
    public static double lootBombBoost(ServerLevel level, UUID owner)
    {
        Map<String, Integer> levels = resonantLevels(level, owner, ArmorSkills.MACHINE_LOOT_BOMB);
        return levels == null ? 1.0 : ArmorSkillEngine.lootBombMultiplier(levels);
    }

    /** 共鸣·方块（点石成金）倍率；未生效 = 1。矿机用。 */
    public static double blockDropBoost(ServerLevel level, UUID owner)
    {
        Map<String, Integer> levels = resonantLevels(level, owner, ArmorSkills.MACHINE_BLOCK_DROP);
        return levels == null ? 1.0 : ArmorSkillEngine.blockDropMultiplier(levels);
    }

    /** 共鸣·经验（经验飞涨）倍率；未生效 = 1。吸收器用。 */
    public static double xpBoost(ServerLevel level, UUID owner)
    {
        Map<String, Integer> levels = resonantLevels(level, owner, ArmorSkills.MACHINE_XP_GAIN);
        return levels == null ? 1.0 : ArmorSkillEngine.xpMultiplier(levels);
    }

    /** 共鸣·熔炼：机器继承「神之熔炼」（基础增幅与共鸣都开才生效）。矿机用。 */
    public static boolean autoSmeltOn(ServerLevel level, UUID owner)
    {
        Map<String, Integer> levels = resonantLevels(level, owner, ArmorSkills.MACHINE_AUTO_SMELT);
        return levels != null && ArmorSkillEngine.isOn(levels, ArmorSkills.AUTO_SMELT);
    }
}
