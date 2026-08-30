package com.godofthings.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnchantScrollItem extends Item {
   public EnchantScrollItem(Properties properties) {
      super(properties);
   }

   @NotNull
   public InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
      ItemStack scroll = player.getItemInHand(hand);
      InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      ItemStack target = player.getItemInHand(otherHand);
      if (target.isEmpty()) {
         if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("message.godofthings.scroll.no_target"), true);
         }

         return InteractionResultHolder.success(scroll);
      } else {
         Map<Enchantment, Integer> scrollEnchants = EnchantmentHelper.getEnchantments(scroll);
         boolean isFilled = !scrollEnchants.isEmpty();
         if (!isFilled) {
            Map<Enchantment, Integer> targetEnchants = EnchantmentHelper.getEnchantments(target);
            if (targetEnchants.isEmpty()) {
               if (!level.isClientSide) {
                  player.displayClientMessage(Component.translatable("message.godofthings.scroll.empty_target"), true);
               }

               return InteractionResultHolder.success(scroll);
            } else {
               if (!level.isClientSide) {
                  ItemStack newScroll = scroll.copy();
                  newScroll.setCount(1);
                  EnchantmentHelper.setEnchantments(new HashMap<>(targetEnchants), newScroll);
                  player.setItemInHand(hand, newScroll);
                  ItemStack cleared = target.copy();
                  EnchantmentHelper.setEnchantments(new HashMap(), cleared);
                  player.setItemInHand(otherHand, cleared);
                  level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                  player.displayClientMessage(Component.translatable("message.godofthings.scroll.captured"), true);
               }

               return InteractionResultHolder.success(player.getItemInHand(hand));
            }
         } else {
            Map<Enchantment, Integer> targetEnchants = new HashMap<>(EnchantmentHelper.getEnchantments(target));
            boolean anyApplied = false;

            for (Entry<Enchantment, Integer> entry : scrollEnchants.entrySet()) {
               Enchantment ench = entry.getKey();
               if (ench != null && ench.canEnchant(target)) {
                  targetEnchants.put(ench, Math.max(entry.getValue(), targetEnchants.getOrDefault(ench, 0)));
                  anyApplied = true;
               }
            }

            if (!anyApplied) {
               if (!level.isClientSide) {
                  player.displayClientMessage(Component.translatable("message.godofthings.scroll.no_apply"), true);
               }

               return InteractionResultHolder.success(scroll);
            } else {
               if (!level.isClientSide) {
                  ItemStack enchanted = target.copy();
                  EnchantmentHelper.setEnchantments(targetEnchants, enchanted);
                  player.setItemInHand(otherHand, enchanted);
                  scroll.shrink(1);
                  level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                  player.displayClientMessage(Component.translatable("message.godofthings.scroll.applied"), true);
               }

               return InteractionResultHolder.success(player.getItemInHand(hand));
            }
         }
      }
   }

   public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      tooltip.add(Component.translatable("item.godofthings.enchant_scroll.tip").withStyle(ChatFormatting.GRAY));
      Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
      if (!enchants.isEmpty()) {
         tooltip.add(Component.translatable("item.godofthings.enchant_scroll.filled").withStyle(ChatFormatting.GOLD));
      }
   }

   public boolean isFoil(@NotNull ItemStack stack) {
      return !EnchantmentHelper.getEnchantments(stack).isEmpty() || super.isFoil(stack);
   }
}
