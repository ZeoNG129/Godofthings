package com.godofthings.doll.item;

import com.godofthings.doll.entity.TwoIDollEntity;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.init.DollComponents;
import com.godofthings.doll.network.DollNetwork;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public final class TwoIDollItem extends BlockItem {
   public TwoIDollItem() {
      super((Block)DollRegistry.TWO_I_DOLL_DISPLAY.get(), new Properties().stacksTo(1));
   }

   public String getDescriptionId() {
      return "item.godofthings.two_i_doll";
   }

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return slotChanged
         || !ItemStack.isSameItem(oldStack, newStack)
         || !Objects.equals(
            oldStack.get((DataComponentType)DollComponents.TWO_I_DOLL_ID.get()), newStack.get((DataComponentType)DollComponents.TWO_I_DOLL_ID.get())
         );
   }

   public EquipmentSlot getEquipmentSlot(ItemStack stack) {
      return EquipmentSlot.HEAD;
   }

   public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity wearer) {
      return slot == EquipmentSlot.HEAD;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!claimOrCheckOwner(stack, player)) {
         return InteractionResultHolder.fail(stack);
      } else if (player.isShiftKeyDown()) {
         if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            deploy(serverPlayer, hand);
         }

         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
      } else {
         if (player instanceof ServerPlayer serverPlayer) {
            DollNetwork.sendItemDialogue(serverPlayer, hand, stack);
         }

         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
      }
   }

   public static boolean deploy(ServerPlayer player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.is((Item)DollRegistry.TWO_I_DOLL_ITEM.get()) && claimOrCheckOwner(stack, player)) {
         TwoIDollEntity doll = (TwoIDollEntity)DollRegistry.TWO_I_DOLL_ENTITY.get().create(player.serverLevel());
         if (doll == null) {
            return false;
         } else {
            Vec3 look = player.getLookAngle();
            doll.moveTo(player.getX() + look.x * 0.9, player.getY(), player.getZ() + look.z * 0.9, player.getYRot(), 0.0F);
            doll.setOwnerId(player.getUUID());
            doll.restoreFromItem(stack);
            if (!player.serverLevel().addFreshEntity(doll)) {
               return false;
            } else {
               if (!player.getAbilities().instabuild) {
                  stack.shrink(1);
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean claimOrCheckOwner(ItemStack stack, Player player) {
      if (!player.level().isClientSide) {
         ensureIdentity(stack);
      }

      UUID owner = (UUID)stack.get((DataComponentType)DollComponents.TWO_I_OWNER.get());
      if (owner == null) {
         if (!player.level().isClientSide) {
            stack.set((DataComponentType)DollComponents.TWO_I_OWNER.get(), player.getUUID());
         }

         return true;
      } else if (owner.equals(player.getUUID())) {
         return true;
      } else {
         if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("msg.godofthings.two_i.owner_only"), true);
         }

         return false;
      }
   }

   public static UUID ensureIdentity(ItemStack stack) {
      UUID id = (UUID)stack.get((DataComponentType)DollComponents.TWO_I_DOLL_ID.get());
      if (id == null) {
         id = UUID.randomUUID();
         stack.set((DataComponentType)DollComponents.TWO_I_DOLL_ID.get(), id);
      }

      return id;
   }

   public InteractionResult useOn(UseOnContext context) {
      return context.getPlayer() != null && !claimOrCheckOwner(context.getItemInHand(), context.getPlayer()) ? InteractionResult.FAIL : super.useOn(context);
   }

   public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
      super.inventoryTick(stack, level, holder, slot, selected);
      if (!level.isClientSide && holder instanceof Player player) {
         boolean newlyReceived = !stack.has((DataComponentType)DollComponents.TWO_I_DOLL_ID.get())
            || !stack.has((DataComponentType)DollComponents.TWO_I_OWNER.get());
         ensureIdentity(stack);
         if (!stack.has((DataComponentType)DollComponents.TWO_I_OWNER.get())) {
            stack.set((DataComponentType)DollComponents.TWO_I_OWNER.get(), player.getUUID());
         }

         if (newlyReceived && player instanceof ServerPlayer serverPlayer) {
            TwoIDollAcquisition.notifyAcquired(serverPlayer, stack);
         }
      }
   }

   public static int getBond(ItemStack stack) {
      if (stack.has((DataComponentType)DollComponents.TWO_I_BOND.get())) {
         return Math.max(-100, Math.min(100, (Integer)stack.getOrDefault((DataComponentType)DollComponents.TWO_I_BOND.get(), 0)));
      } else {
         int legacyScore = (Integer)stack.getOrDefault((DataComponentType)DollComponents.TWO_I_GIFT_SCORE.get(), 0);
         return Math.max(0, Math.min(100, legacyScore * 10));
      }
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (target instanceof TwoIDollEntity doll) {
         if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            doll.openDialogue(serverPlayer);
         }

         return InteractionResult.sidedSuccess(player.level().isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.godofthings.two_i_doll.flavor").withStyle(ChatFormatting.LIGHT_PURPLE));
      tooltip.add(Component.translatable("tooltip.godofthings.two_i_doll.use").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.godofthings.two_i_doll.headwear").withStyle(ChatFormatting.GRAY));
      if (ModList.get().isLoaded("curios")) {
         tooltip.add(Component.translatable("tooltip.godofthings.two_i_doll.curios").withStyle(ChatFormatting.GRAY));
      }

      tooltip.add(Component.translatable("tooltip.godofthings.two_i_doll.remote").withStyle(ChatFormatting.GRAY));
      super.appendHoverText(stack, context, tooltip, flag);
   }
}
