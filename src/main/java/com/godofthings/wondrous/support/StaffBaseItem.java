package com.godofthings.wondrous.support;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 上游 {@code EndlessBeafItem} 的本地替身（[本地改动]）。
 * <p>
 * 上游的荒辰移晷之杖继承基 mod（useless_mod）的造化杖 {@code EndlessBeafItem}，
 * 从而获得模式轮盘 / G 菜单开关（时间加速开关组件等）。本模组没有造化杖基类，
 * 这里提供一个等价的最小基类：
 * <ul>
 *   <li>TieredItem（耐久/工具分类与上游一致，NEITHER 档）；</li>
 *   <li>时间加速开关组件由 {@link StaffSupportHooks} 提供的本地组件承载，
 *       右键轮盘 / G 菜单不再移植（本模组用自己的配置界面与按键切换加速档）。</li>
 * </ul>
 * 上游的 canPerformAction / 剪刀 / 打火石能力面随基 mod 的模式轮盘而来，
 * 本模组的杖不承担这些工具形态（本项目没有造化杖），故一律不声明；
 * 挖掘能力交给原版 TieredItem 默认行为。
 */
public class StaffBaseItem extends TieredItem
{
    public StaffBaseItem(Properties properties)
    {
        super(new Tier()
        {
            @Override
            public int getUses()
            {
                return 0;
            }

            @Override
            public float getSpeed()
            {
                return 1.0F;
            }

            @Override
            public float getAttackDamageBonus()
            {
                return 0.0F;
            }

            @Override
            public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops()
            {
                return net.minecraft.tags.BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            }

            @Override
            public int getEnchantmentValue()
            {
                return 0;
            }

            @Override
            public net.minecraft.world.item.crafting.Ingredient getRepairIngredient()
            {
                return net.minecraft.world.item.crafting.Ingredient.EMPTY;
            }
        }, properties);
    }

    // 上游 EndlessBeafItem.use()：非潜行右键 = 传送开关等基 mod 功能。本模组没有这些
    // 基 mod 功能，默认 PASS 交回原版；子类（WondrousStaffItem）的 use 只在天体加速路径生效。
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand)
    {
        return InteractionResult.PASS;
    }

    /** 本模组没有造化杖的模式轮盘；无扳手标签概念，恒 false（供上游移植代码调用）。 */
    public static boolean isWrenchTagEnabled(ItemStack stack)
    {
        return false;
    }

    /** 上游基 mod 的剪刀开关；本模组杖无该模式，恒 false（不暴露剪刀能力面）。 */
    public static boolean isShearsEnabled(ItemStack stack)
    {
        return false;
    }

    /** 上游基 mod 的打火石开关；本模组杖无该模式，恒 false。 */
    public static boolean isFlintAndSteelEnabled(ItemStack stack)
    {
        return false;
    }
}
