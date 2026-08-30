package com.godofthings.toolbelt.client;

import com.godofthings.toolbelt.BeltFinder;
import com.godofthings.toolbelt.network.BeltContentsChange;
import com.godofthings.toolbelt.network.SyncBeltSlotContents;
import com.godofthings.toolbelt.slot.BeltExtensionSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ClientPacketHandlers
{
    public static void handleBeltContentsChange(final BeltContentsChange message)
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.level == null)
                return;
            Entity entity = minecraft.level.getEntity(message.player);
            if (!(entity instanceof Player))
                return;
            Player player = (Player) entity;
            BeltFinder.setBeltFromPacket(player, message.where, message.slot, message.stack);
        });
    }

    public static void handleBeltSlotContents(SyncBeltSlotContents message)
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.level == null)
                return;
            Entity entity = minecraft.level.getEntity(message.entityId);
            if (entity instanceof Player)
            {
                BeltExtensionSlot.get((LivingEntity) entity).ifPresent((slot) -> slot.setAll(message.stacks));
            }
        });
    }
}
