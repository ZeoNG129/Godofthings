package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodTransmitterBlockEntity;
import com.godofthings.item.GodBinderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 副手持有绑定器时放置 FE 机器，自动绑定到最近的神之传输进行充能。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class BinderPlaceHandler
{
    @SubscribeEvent
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof GodBinderItem))
        {
            return;
        }
        BlockPos pos = event.getPos();
        if (!GodTransmitterBlockEntity.hasEnergyStorage(level, pos))
        {
            return;
        }
        GodTransmitterBlockEntity nearest = GodTransmitterBlockEntity.findNearest(level, pos);
        if (nearest != null)
        {
            nearest.bindMachine(level, pos);
            player.displayClientMessage(
                    Component.translatable("message.godofthings.binder.machine_bound"), true);
        }
    }
}
