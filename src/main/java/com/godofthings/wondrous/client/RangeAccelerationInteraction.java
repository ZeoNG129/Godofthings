package com.godofthings.wondrous.client;
import com.godofthings.wondrous.init.WondrousItems;
import com.godofthings.Godofthings;

import com.godofthings.wondrous.UselessStretcherMod;
import com.godofthings.wondrous.content.entity.WondrousStaffAcceleration;
import com.godofthings.wondrous.content.range.RangeAccelerationSettings;
import com.godofthings.wondrous.init.ModItems;
import com.godofthings.wondrous.network.RangeNetwork;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Sends range placement and label-gun-style filter-marking gestures from the client. */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class RangeAccelerationInteraction {
    private RangeAccelerationInteraction() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) return;

        ItemStack staff = event.getItemStack();
        if (!staff.is(WondrousItems.WONDROUS_STAFF.get())) return;
        if (com.godofthings.wondrous.content.apotheosis.ApotheosisStaffSettings.selectionMode(staff)
                && com.godofthings.wondrous.content.apotheosis.ApotheosisStaffSettings.isTable(
                        event.getLevel().getBlockState(event.getPos()))) return;

        if (RangeAccelerationSettings.filterMarkingMode(staff)) {
            if (WondrousStaffAcceleration.isValidTarget(event.getLevel(), event.getPos())) {
                RangeNetwork.toggleFilter(event.getHand(), event.getPos(), Screen.hasControlDown());
            }
        } else if (RangeAccelerationSettings.placementMode(staff)
                && WondrousStaffAcceleration.isEnabled(staff)
                && event.getEntity().isShiftKeyDown()) {
            RangeNetwork.place(event.getHand(), event.getPos(), event.getFace());
        } else {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
