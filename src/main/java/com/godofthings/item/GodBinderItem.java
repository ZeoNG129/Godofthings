package com.godofthings.item;

import com.godofthings.block.entity.GodTransmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 神之绑定器：右键 FE 机器绑定到最近的神之传输充能；再次右键取消绑定。
 * <p>
 * 使用 {@code onItemUseFirst} 在方块交互（打开机器 UI）之前拦截：右键 FE 机器只执行
 * 绑定 / 取消绑定，不打开机器界面。
 */
public class GodBinderItem extends Item
{
    public GodBinderItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx)
    {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (player == null || level.isClientSide)
        {
            return InteractionResult.PASS;
        }
        // 目标方块是 FE 机器 → 绑定 / 取消绑定到最近的神之传输（拦截方块交互）
        if (GodTransmitterBlockEntity.hasEnergyStorage(level, pos))
        {
            GodTransmitterBlockEntity nearest = GodTransmitterBlockEntity.findNearest(level, pos);
            if (nearest == null)
            {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.no_transmitter"), true);
                return InteractionResult.SUCCESS;
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
                nearest.bindMachine(level, pos);
                player.displayClientMessage(
                        Component.translatable("message.godofthings.binder.machine_bound"), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
