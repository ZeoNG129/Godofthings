package com.godofthings.wondrous.event;
import com.godofthings.wondrous.init.WondrousItems;
import com.godofthings.Godofthings;

import com.godofthings.wondrous.content.entity.WondrousStaffAcceleration;
import com.godofthings.wondrous.content.item.WondrousStaffLootRefresh;
import com.godofthings.wondrous.config.WondrousConfig;
import com.godofthings.wondrous.init.ModItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Server-side staff features that operate on the normal block-drops pipeline. */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class WondrousStaffFeatureEvents {
    private WondrousStaffFeatureEvents() {
    }

    /** Minecart containers use entity interaction rather than Item#interactLivingEntity. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack staff = event.getItemStack();
        if (!staff.is(WondrousItems.WONDROUS_STAFF.get())
                || !player.isShiftKeyDown()
                || !WondrousConfig.enableStaffLootRefresh()
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
