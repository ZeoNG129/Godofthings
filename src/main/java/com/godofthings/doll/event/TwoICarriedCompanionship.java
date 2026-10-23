package com.godofthings.doll.event;

import com.godofthings.Godofthings;


import com.godofthings.doll.entity.TwoIDollMemory;
import com.godofthings.doll.entity.TwoICompanionshipData;
import com.godofthings.doll.compat.TwoICuriosCompat;
import com.godofthings.doll.item.TwoIDollItem;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.init.DollComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Equipped dolls, checked every five seconds. Only the five-minute reward changes a stack. */
@EventBusSubscriber(modid = Godofthings.MODID)
public final class TwoICarriedCompanionship {
    private TwoICarriedCompanionship() { }
    private static final EquipmentSlot[] CARRIED_SLOTS =
            {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD};

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
        long time = player.level().getGameTime();
        if (time % TwoICompanionshipData.CHECK_INTERVAL != 0) return;
        // A stable item ID selects server SavedData; ticking never rewrites item memory.
        for (EquipmentSlot slot : CARRIED_SLOTS) {
            sampleCarried(player, player.getItemBySlot(slot), time);
        }
        if (net.neoforged.fml.ModList.get().isLoaded("curios")) TwoICuriosCompat.sampleEquipped(player, time);
    }

    /** Shared with optional Curios slots; SavedData rejects a second sample of one ID in the same tick. */
    public static void sampleCarried(ServerPlayer player, ItemStack stack, long time) {
        if (!stack.is(DollRegistry.TWO_I_DOLL.get())) return;
        // Legacy stacks bind to their first holder. Carrying someone else's doll earns no bond.
        java.util.UUID owner = stack.get(DollComponents.TWO_I_OWNER.get());
        if (owner == null) stack.set(DollComponents.TWO_I_OWNER.get(), player.getUUID());
        else if (!owner.equals(player.getUUID())) return;
        java.util.UUID id = TwoIDollItem.ensureIdentity(stack);
        TwoIDollMemory memory = stack.getOrDefault(DollComponents.TWO_I_MEMORY.get(), TwoIDollMemory.empty());
        var data = TwoICompanionshipData.get(player.serverLevel().getServer());
        var sample = data.sample(id, memory, time);
        if (sample.awarded()) {
            stack.set(DollComponents.TWO_I_BOND.get(), Math.min(100, TwoIDollItem.getBond(stack) + 1));
            player.displayClientMessage(Component.translatable("msg.godofthings.two_i.companionship"), true);
        }
    }
}
