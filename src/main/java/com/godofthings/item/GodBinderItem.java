package com.godofthings.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * 神之绑定器：蹲下右键绑定自己（记录玩家 UUID），放入神之传输后为其充能。
 * 再次蹲下右键可重新绑定当前玩家。
 */
public class GodBinderItem extends Item
{
    /** 绑定玩家 UUID 的 CUSTOM_DATA 键。 */
    public static final String KEY_OWNER = "BinderOwner";

    public GodBinderItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown())
        {
            if (!level.isClientSide)
            {
                CustomData.update(DataComponents.CUSTOM_DATA, stack,
                        tag -> tag.putUUID(KEY_OWNER, player.getUUID()));
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.bound", player.getName()), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }
}
