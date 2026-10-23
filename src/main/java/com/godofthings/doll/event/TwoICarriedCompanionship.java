package com.godofthings.doll.event;

import com.godofthings.doll.compat.TwoICuriosCompat;
import com.godofthings.doll.entity.TwoICompanionshipData;
import com.godofthings.doll.entity.TwoIDollMemory;
import com.godofthings.doll.item.TwoIDollItem;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.init.DollComponents;
import java.util.UUID;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(
   modid = "godofthings"
)
public final class TwoICarriedCompanionship {
   private static final EquipmentSlot[] CARRIED_SLOTS = new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD};

   private TwoICarriedCompanionship() {
   }

   @SubscribeEvent
   public static void tick(Post event) {
      if (event.getEntity() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()) {
         long time = player.level().getGameTime();
         if (time % 100L != 0L) {
            return;
         }

         for (EquipmentSlot slot : CARRIED_SLOTS) {
            sampleCarried(player, player.getItemBySlot(slot), time);
         }

         if (ModList.get().isLoaded("curios")) {
            TwoICuriosCompat.sampleEquipped(player, time);
         }

         return;
      }
   }

   public static void sampleCarried(ServerPlayer player, ItemStack stack, long time) {
      if (stack.is((Item)DollRegistry.TWO_I_DOLL_ITEM.get())) {
         UUID owner = (UUID)stack.get((DataComponentType)DollComponents.TWO_I_OWNER.get());
         if (owner == null) {
            stack.set((DataComponentType)DollComponents.TWO_I_OWNER.get(), player.getUUID());
         } else if (!owner.equals(player.getUUID())) {
            return;
         }

         UUID id = TwoIDollItem.ensureIdentity(stack);
         TwoIDollMemory memory = (TwoIDollMemory)stack.getOrDefault((DataComponentType)DollComponents.TWO_I_MEMORY.get(), TwoIDollMemory.empty());
         TwoICompanionshipData data = TwoICompanionshipData.get(player.serverLevel().getServer());
         TwoICompanionshipData.Sample sample = data.sample(id, memory, time);
         if (sample.awarded()) {
            stack.set((DataComponentType)DollComponents.TWO_I_BOND.get(), Math.min(100, TwoIDollItem.getBond(stack) + 1));
            player.displayClientMessage(Component.translatable("msg.godofthings.two_i.companionship"), true);
         }
      }
   }
}
