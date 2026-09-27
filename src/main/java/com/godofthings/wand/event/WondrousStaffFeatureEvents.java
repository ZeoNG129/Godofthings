package com.godofthings.wand.event;

import com.godofthings.wand.content.entity.WondrousStaffAcceleration;
import com.godofthings.wand.content.item.WondrousStaffLootRefresh;
import com.godofthings.wand.config.StretcherConfig;
import com.godofthings.wand.init.ModItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Server-side staff features that operate on the normal block-drops pipeline. */
@EventBusSubscriber(modid = com.godofthings.wand.UselessStretcherMod.MODID)
public final class WondrousStaffFeatureEvents {
    private WondrousStaffFeatureEvents() {
    }

    /** Minecart containers use entity interaction rather than Item#interactLivingEntity. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack staff = event.getItemStack();
        if (!staff.is(ModItems.WONDROUS_STAFF.get())
                || !player.isShiftKeyDown()
                || !StretcherConfig.enableStaffLootRefresh()
                || !WondrousStaffAcceleration.isLootRefreshEnabled(staff)
                || !WondrousStaffLootRefresh.isSupportedEntity(event.getTarget())) {
            return;
        }

        InteractionResult result = WondrousStaffLootRefresh.tryRefreshEntity(player, event.getTarget());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide()));
        }
    }
}
