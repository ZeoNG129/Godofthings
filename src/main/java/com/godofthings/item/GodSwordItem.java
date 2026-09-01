package com.godofthings.item;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import javax.annotation.Nullable;

/**
 * 神之剑：代码层面级秒杀，无论目标多少血量都是一击必杀。物品不可破坏（无耐久损耗）。
 * <p>
 * 持有神之剑按 J 键打开功能面板，可独立或同时启用三个功能：
 * <ul>
 *   <li>斩首：击杀带头颅的生物必掉对应头颅（僵尸/骷髅/苦力怕/凋灵骷髅/猪灵/末影龙）。</li>
 *   <li>捕捉：击杀任何生物掉落对应刷怪蛋。</li>
 *   <li>抢劫：顶级抢夺 255（直接管理神之剑的 LOOTING 附魔）。</li>
 * </ul>
 */
public class GodSwordItem extends SwordItem
{
    public GodSwordItem(Properties properties)
    {
        super(Tiers.NETHERITE, properties.component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
    {
        if (attacker.level().isClientSide)
        {
            return true;
        }
        ServerLevel level = (ServerLevel) attacker.level();

        // 击杀：用带玩家攻击者的伤害源（而非 kill() 的 genericKill，后者无 entity）。
        // 掉落表 dropFromLootTable 的 ATTACKING_ENTITY 取自 damageSource.getEntity()，
        // looting 条件（LootItemRandomChanceWithEnchantedBonusCondition）读的正是该攻击者的抢夺附魔，
        // 因此必须用 playerAttack 才能让「抢劫」生效。
        // Float.MAX_VALUE 即使经过无敌帧差值计算也足以秒杀任何目标。
        if (target.isAlive())
        {
            DamageSource source = attacker instanceof Player player
                    ? level.damageSources().playerAttack(player)
                    : level.damageSources().generic();
            target.hurt(source, Float.MAX_VALUE);
        }

        // 目标死亡后执行斩首 / 捕捉（开关存在神之剑 CUSTOM_DATA，见 SwordModes）
        if (target.isDeadOrDying())
        {
            if (SwordModes.isBeheadEnabled(stack))
            {
                dropHead(target);
            }
            if (SwordModes.isCaptureEnabled(stack))
            {
                dropEgg(target);
            }
        }

        return true;
    }

    /** 斩首：击杀带头颅的生物时必掉对应头颅。 */
    private static void dropHead(LivingEntity target)
    {
        Item head = headForEntity(target.getType());
        if (head != null)
        {
            target.spawnAtLocation(new ItemStack(head));
        }
    }

    @Nullable
    private static Item headForEntity(EntityType<?> type)
    {
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED)
        {
            return Items.ZOMBIE_HEAD;
        }
        if (type == EntityType.SKELETON || type == EntityType.STRAY)
        {
            return Items.SKELETON_SKULL;
        }
        if (type == EntityType.WITHER_SKELETON)
        {
            return Items.WITHER_SKELETON_SKULL;
        }
        if (type == EntityType.CREEPER)
        {
            return Items.CREEPER_HEAD;
        }
        if (type == EntityType.PIGLIN || type == EntityType.PIGLIN_BRUTE || type == EntityType.ZOMBIFIED_PIGLIN)
        {
            return Items.PIGLIN_HEAD;
        }
        if (type == EntityType.ENDER_DRAGON)
        {
            return Items.DRAGON_HEAD;
        }
        return null;
    }

    /** 捕捉：击杀生物掉落对应刷怪蛋。 */
    private static void dropEgg(LivingEntity target)
    {
        SpawnEggItem egg = SpawnEggItem.byId(target.getType());
        if (egg != null)
        {
            target.spawnAtLocation(new ItemStack(egg));
        }
    }

    /** 抢劫开关：开启时给神之剑附加 255 级抢夺，关闭时移除。由切换面板经网络在服务端调用。 */
    public static void applyLooting(ItemStack stack, ServerLevel level, boolean enabled)
    {
        Holder<Enchantment> looting = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.LOOTING);
        if (enabled)
        {
            stack.enchant(looting, 255);
        }
        else
        {
            EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.removeIf(h -> h.is(Enchantments.LOOTING)));
        }
    }
}
