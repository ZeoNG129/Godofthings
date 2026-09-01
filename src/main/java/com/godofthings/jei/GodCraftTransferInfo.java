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
import net.minecraft.world.item.crafting.RecipeHolder;

// JEI 19.x（1.21.1）：IRecipeTransferInfo<C, R> 仍是两个泛型参数，但 R 为 RecipeHolder<CraftingRecipe>
//（RecipeTypes.CRAFTING 已变为 RecipeType<RecipeHolder<CraftingRecipe>>），方法入参随之带 Holder。
public class GodCraftTransferInfo implements IRecipeTransferInfo<GodCraftMenu, RecipeHolder<CraftingRecipe>> {
   public Class<? extends GodCraftMenu> getContainerClass() {
      return GodCraftMenu.class;
   }

   public Optional<MenuType<GodCraftMenu>> getMenuType() {
      return Optional.of((MenuType<GodCraftMenu>)Godofthings.GOD_CRAFT_MENU.get());
   }

   public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
      return RecipeTypes.CRAFTING;
   }

   public boolean canHandle(GodCraftMenu container, RecipeHolder<CraftingRecipe> recipe) {
      return true;
   }

   public List<Slot> getRecipeSlots(GodCraftMenu container, RecipeHolder<CraftingRecipe> recipe) {
      return container.slots.subList(0, 9);
   }

   public List<Slot> getInventorySlots(GodCraftMenu container, RecipeHolder<CraftingRecipe> recipe) {
      return container.slots.subList(10, container.slots.size());
   }

   public boolean requireCompleteSets(GodCraftMenu container, RecipeHolder<CraftingRecipe> recipe) {
      return false;
   }
}
