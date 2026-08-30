package com.godofthings.toolbelt.common;

import com.godofthings.toolbelt.BeltFinder;
import com.godofthings.toolbelt.ToolBelt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ToolBelt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BeltEvents
{
    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event)
    {
        Entity entity = event.getTarget();
        if (!(entity instanceof Player))
            return;

        Player player = (Player) entity;
        BeltFinder.sendSync(player);
    }
}
