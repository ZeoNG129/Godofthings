package com.godofthings.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.Level;

/**
 * 神之不毁：与任意有耐久的物品合成/右键使其变为无限耐久。
 */
public class GodUnbreakableItem extends Item
{
    public GodUnbreakableItem(Properties properties)
    {
        super(properties);
    }

    // 兜底（不依赖合成配方）：主手持神之不毁、副手有物品时，右键使副手物品变为无限耐久并消耗神之不毁
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
        if (hand == InteractionHand.MAIN_HAND && !off.isEmpty())
        {
            ItemStack result = off.copy();
            // 1.21.1：NBT 的 Unbreakable 标签 → UNBREAKABLE 数据组件
            result.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
            player.setItemInHand(InteractionHand.OFF_HAND, result);
            main.shrink(1);
            if (!level.isClientSide)
            {
                level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResultHolder.success(main);
        }
        return InteractionResultHolder.pass(main);
    }
}
