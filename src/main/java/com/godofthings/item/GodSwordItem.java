package com.godofthings.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Unbreakable;

/**
 * 神之剑：代码层面级秒杀，无论目标多少血量都是一击必杀。
 * <p>
 * 攻击命中时直接调用 {@link LivingEntity#kill()} 击杀目标，
 * 绕过护甲、伤害上限与血量判定。物品不可破坏（无耐久损耗）。
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
        if (!attacker.level().isClientSide && target.isAlive())
        {
            target.kill(); // 代码层面秒杀，无视血量
        }
        return true;
    }
}
