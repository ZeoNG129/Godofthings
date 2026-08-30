package com.godofthings.jei;

import com.godofthings.Godofthings;
import com.godofthings.menu.GodCraftMenu;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;

public class GodCraftTransferInfo implements IRecipeTransferInfo<GodCraftMenu, CraftingRecipe> {
   public Class<? extends GodCraftMenu> getContainerClass() {
      return GodCraftMenu.class;
   }

   public Optional<MenuType<GodCraftMenu>> getMenuType() {
      return Optional.of((MenuType<GodCraftMenu>)Godofthings.GOD_CRAFT_MENU.get());
   }

   public RecipeType<CraftingRecipe> getRecipeType() {
      return RecipeTypes.CRAFTING;
   }

   public boolean canHandle(GodCraftMenu container, CraftingRecipe recipe) {
      return true;
   }

   public List<Slot> getRecipeSlots(GodCraftMenu container, CraftingRecipe recipe) {
      return container.slots.subList(0, 9);
   }

   public List<Slot> getInventorySlots(GodCraftMenu container, CraftingRecipe recipe) {
      return container.slots.subList(10, container.slots.size());
   }

   public boolean requireCompleteSets(GodCraftMenu container, CraftingRecipe recipe) {
      return false;
   }
}
