package com.godofthings.emi;

import com.godofthings.Godofthings;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/**
 * EMI 联动：让神之熔炉在 EMI 里作为熔炼工作站显示所有可烧配方。
 * 软前置——没装 EMI 时本类不会被加载，不影响其他功能。
 */
@EmiEntrypoint
public class GodEmiPlugin implements EmiPlugin
{
    public static final EmiRecipeCategory FURNACE_CATEGORY = new EmiRecipeCategory(
            ResourceLocation.tryBuild(Godofthings.MODID, "god_furnace"),
            EmiStack.of(Godofthings.GOD_FURNACE_ITEM.get()));

    @Override
    public void register(EmiRegistry registry)
    {
        registry.addCategory(FURNACE_CATEGORY);
        // 神之熔炉方块 + 物品都作为该分类的工作站
        registry.addWorkstation(FURNACE_CATEGORY, EmiStack.of(Godofthings.GOD_FURNACE_ITEM.get()));
        registry.addWorkstation(FURNACE_CATEGORY, EmiStack.of(Godofthings.GOD_FURNACE.get()));

        // 收集原版熔炉/高炉/烟熏炉配方（神之熔炉兼容三者），注册为可查看配方
        addCookingRecipes(registry, RecipeType.SMELTING);
        addCookingRecipes(registry, RecipeType.BLASTING);
        addCookingRecipes(registry, RecipeType.SMOKING);
    }

    // 1.21.1：RecipeManager.getAllRecipesFor 返回 List<RecipeHolder<T>>，改泛型方法以正确传递 Holder
    private <T extends AbstractCookingRecipe> void addCookingRecipes(EmiRegistry registry, RecipeType<T> type)
    {
        Minecraft mc = Minecraft.getInstance();
        RegistryAccess registryAccess = mc.level != null
                ? mc.level.registryAccess()
                : RegistryAccess.EMPTY;
        registry.getRecipeManager().getAllRecipesFor(type)
                .forEach(recipe -> registry.addRecipe(new GodFurnaceEmiRecipe(recipe, registryAccess)));
    }

    /** 包装一个熔炼配方为 EMI 配方显示 */
    public static class GodFurnaceEmiRecipe implements EmiRecipe
    {
        private final AbstractCookingRecipe recipe;
        private final ResourceLocation id;
        private final EmiIngredient input;
        private final EmiStack output;

        // 1.21.1：配方 id 移到 RecipeHolder 上（Recipe 不再有 getId），构造器改收 RecipeHolder
        public GodFurnaceEmiRecipe(RecipeHolder<? extends AbstractCookingRecipe> holder, RegistryAccess registryAccess)
        {
            this.recipe = holder.value();
            this.id = holder.id();
            Ingredient ingredient = recipe.getIngredients().isEmpty()
                    ? Ingredient.EMPTY
                    : recipe.getIngredients().get(0);
            this.input = EmiIngredient.of(ingredient);
            this.output = EmiStack.of(recipe.getResultItem(registryAccess));
        }

        @Override
        public EmiRecipeCategory getCategory()
        {
            return FURNACE_CATEGORY;
        }

        @Override
        public ResourceLocation getId()
        {
            return id;
        }

        @Override
        public List<EmiIngredient> getInputs()
        {
            return List.of(input);
        }

        @Override
        public List<EmiStack> getOutputs()
        {
            return List.of(output);
        }

        @Override
        public int getDisplayWidth()
        {
            return 76;
        }

        @Override
        public int getDisplayHeight()
        {
            return 34;
        }

        @Override
        public void addWidgets(WidgetHolder widgets)
        {
            // 输入槽（左）→ 输出槽（右）
            widgets.addSlot(input, 0, 8);
            widgets.addFillingArrow(26, 9, 24);
            widgets.addSlot(output, 58, 8).recipeContext(this);
        }
    }
}
