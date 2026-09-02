package com.godofthings.item;

import com.godofthings.block.entity.GodTransmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * 神之绑定器：
 * <ul>
 *   <li>蹲下右键（面向空气）绑定自己，记录玩家 UUID，放入神之传输后为其物品栏充能。</li>
 *   <li>右键 FE 机器：绑定到最近的神之传输充能；再次右键取消绑定。</li>
 * </ul>
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

    @Override
    public InteractionResult useOn(UseOnContext ctx)
    {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (player == null || level.isClientSide)
        {
            return InteractionResult.PASS;
        }
        // 目标方块是 FE 机器 → 绑定 / 取消绑定到最近的神之传输
        if (GodTransmitterBlockEntity.hasEnergyStorage(level, pos))
        {
            GodTransmitterBlockEntity nearest = GodTransmitterBlockEntity.findNearest(level, pos);
            if (nearest == null)
            {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.no_transmitter"), true);
                return InteractionResult.FAIL;
            }
            ResourceKey<Level> dim = level.dimension();
            if (nearest.isBound(dim, pos))
            {
                nearest.unbindMachine(dim, pos);
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.machine_unbound"), true);
            }
            else
            {
                nearest.bindMachine(dim, pos);
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.machine_bound"), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
