package com.godofthings.handler;

import appeng.api.config.Actionable;
import appeng.api.features.IGridLinkableHandler;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.PlayerSource;
import com.godofthings.item.GodFavorWandItem;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class GodFavorWandAe2Helper {
   private static final String TAG_ACCESS_POINT_POS = "accessPoint";
   public static final IGridLinkableHandler LINKABLE_HANDLER = new GodFavorWandAe2Helper.LinkableHandler();

   private GodFavorWandAe2Helper() {
   }

   @Nullable
   public static IGrid getLinkedGrid(ItemStack stack, Level level, @Nullable Player player) {
      if (level instanceof ServerLevel serverLevel) {
         GlobalPos linkedPos = GodFavorWandItem.getLinkedPosition(stack);
         if (linkedPos == null) {
            return null;
         } else {
            ServerLevel linkedLevel = serverLevel.getServer().getLevel(linkedPos.dimension());
            if (linkedLevel == null) {
               return null;
            } else {
               return linkedLevel.getBlockEntity(linkedPos.pos()) instanceof IWirelessAccessPoint accessPoint ? accessPoint.getGrid() : null;
            }
         }
      } else {
         return null;
      }
   }

   public static boolean storeItemInAENetwork(ItemStack stack, Player player, ItemStack toolStack) {
      if (player != null && !stack.isEmpty()) {
         ItemStack toolItem = toolStack;
         if (toolStack == null || toolStack.isEmpty()) {
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();
            if (mainHandItem.getItem() instanceof GodFavorWandItem) {
               toolItem = mainHandItem;
            } else {
               if (!(offHandItem.getItem() instanceof GodFavorWandItem)) {
                  return false;
               }

               toolItem = offHandItem;
            }
         }

         if (toolItem.getItem() instanceof GodFavorWandItem tool && tool.isAEStoragePriorityMode(toolItem)) {
            try {
               IGrid grid = getLinkedGrid(toolItem, player.level(), player);
               if (grid == null) {
                  return false;
               }

               MEStorage storage = grid.getStorageService().getInventory();
               if (storage == null) {
                  return false;
               }

               AEItemKey aeKey = AEItemKey.of(stack);
               if (aeKey == null) {
                  return false;
               }

               long inserted = storage.insert(aeKey, (long)stack.getCount(), Actionable.MODULATE, new PlayerSource(player, null));
               if (inserted == (long)stack.getCount()) {
                  return true;
               }

               if (inserted > 0L) {
                  stack.setCount((int)((long)stack.getCount() - inserted));
                  return stack.isEmpty();
               }

               return false;
            } catch (Exception var10) {
               return false;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static class LinkableHandler implements IGridLinkableHandler {
      public boolean canLink(ItemStack stack) {
         return stack.getItem() instanceof GodFavorWandItem;
      }

      public void link(ItemStack itemStack, GlobalPos pos) {
         GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(tag -> itemStack.getOrCreateTag().put("accessPoint", tag));
      }

      public void unlink(ItemStack itemStack) {
         itemStack.removeTagKey("accessPoint");
      }
   }
}
