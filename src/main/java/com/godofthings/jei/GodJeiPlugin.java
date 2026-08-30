package com.godofthings.jei;

import com.godofthings.Godofthings;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 联动：
 * 1. 把神之熔炉挂到原版熔炼分类的催化剂上，玩家在 JEI 里点神之熔炉即可查看所有熔炉配方。
 * 2. 神之合成注册 JEI 配方传输，JEI 合成界面的"+ "按钮可一键填充 3×3 合成格。
 * 软前置——没装 JEI 时本类不会被加载，不影响其他功能。
 */
@JeiPlugin
public class GodJeiPlugin implements IModPlugin
{
    @Override
    public ResourceLocation getPluginUid()
    {
        return ResourceLocation.tryBuild(Godofthings.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration)
    {
        // 复用原版分类，无需自定义 category
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration)
    {
        // 神之合成：JEI "+" 一键填充配方材料到 3×3 合成格
        registration.addRecipeTransferHandler(new GodCraftTransferInfo());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
    {
        // 神之熔炉方块 + 物品作为熔炼/高炉/烟熏炉三类配方的催化剂
        registration.addRecipeCatalyst(Godofthings.GOD_FURNACE.get(), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(Godofthings.GOD_FURNACE.get(), RecipeTypes.BLASTING);
        registration.addRecipeCatalyst(Godofthings.GOD_FURNACE.get(), RecipeTypes.SMOKING);
        registration.addRecipeCatalyst(Godofthings.GOD_FURNACE_ITEM.get(), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(Godofthings.GOD_FURNACE_ITEM.get(), RecipeTypes.BLASTING);
        registration.addRecipeCatalyst(Godofthings.GOD_FURNACE_ITEM.get(), RecipeTypes.SMOKING);
    }
}
