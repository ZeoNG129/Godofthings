package com.godofthings.menu;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodCraftBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class GodCraftTemplateMenu extends AbstractContainerMenu {
   private final GodCraftBlockEntity be;
   private final ContainerLevelAccess access;
   public static final int BUTTON_SAVE_BASE = 10;
   public static final int BUTTON_LOAD_BASE = 20;
   public static final int BUTTON_BACK = 30;

   public GodCraftTemplateMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
      this(containerId, playerInv, (GodCraftBlockEntity)playerInv.player.level().getBlockEntity(extraData.readBlockPos()));
   }

   public GodCraftTemplateMenu(int containerId, Inventory playerInv, final GodCraftBlockEntity be) {
      super((MenuType)Godofthings.GOD_CRAFT_TEMPLATE_MENU.get(), containerId);
      this.be = be;
      this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

      for (int t = 0; t < 8; t++) {
         final int idx = t;
         this.addDataSlot(new DataSlot() {
            public int get() {
               return be.hasTemplate(idx) ? 1 : 0;
            }

            public void set(int value) {
            }
         });
      }
   }

   public GodCraftBlockEntity getBlockEntity() {
      return this.be;
   }

   public boolean clickMenuButton(Player player, int buttonId) {
      if (this.be.getLevel() == null
         || !this.be.getLevel().dimension().equals(player.level().dimension())
         || player.distanceToSqr(
               (double)this.be.getBlockPos().getX() + 0.5, (double)this.be.getBlockPos().getY() + 0.5, (double)this.be.getBlockPos().getZ() + 0.5
            )
            > 64.0) {
         return false;
      } else if (buttonId >= 10 && buttonId < 18) {
         int slot = buttonId - 10;
         boolean ok = this.be.saveTemplate(slot);
         if (ok) {
            player.displayClientMessage(Component.translatable("gui.godofthings.god_craft.template_saved", new Object[]{slot + 1}), true);
         } else {
            player.displayClientMessage(Component.translatable("gui.godofthings.god_craft.template_empty"), true);
         }

         this.broadcastChanges();
         return true;
      } else if (buttonId >= 20 && buttonId < 28) {
         int slot = buttonId - 20;
         if (!this.be.hasTemplate(slot)) {
            player.displayClientMessage(Component.translatable("gui.godofthings.god_craft.template_none", new Object[]{slot + 1}), true);
         } else if (this.be.loadTemplateFromInventory(player, slot)) {
            player.displayClientMessage(Component.translatable("gui.godofthings.god_craft.template_loaded", new Object[]{slot + 1}), true);
         } else {
            player.displayClientMessage(Component.translatable("gui.godofthings.god_craft.template_missing", new Object[]{slot + 1}), true);
         }

         this.broadcastChanges();
         return true;
      } else if (buttonId == 30 && player instanceof ServerPlayer serverPlayer) {
         serverPlayer.openMenu(new MenuProvider() {
            public Component getDisplayName() {
               return Component.translatable("container.godofthings.god_craft");
            }

            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
               return new GodCraftMenu(containerId, inventory, GodCraftTemplateMenu.this.be);
            }
         }, buf -> buf.writeBlockPos(this.be.getBlockPos()));
         return true;
      } else {
         return false;
      }
   }

   public boolean stillValid(Player player) {
      return stillValid(this.access, player, (Block)Godofthings.GOD_CRAFT.get());
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }
}
