package com.godofthings.torchmaster.common.items;

import com.godofthings.torchmaster.TorchmasterConfig;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class TMItemBlock extends BlockItem {
   public TMItemBlock(Block blockIn, Properties builder) {
      super(blockIn, builder);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, world, tooltip, flag);
      if ((Boolean)TorchmasterConfig.GENERAL.beginnerTooltips.get()) {
         tooltip.add(Component.translatable(this.getDescriptionId(stack) + ".tooltip"));
      }
   }
}
