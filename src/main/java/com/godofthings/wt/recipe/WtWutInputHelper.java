package com.godofthings.wt.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;

import com.godofthings.Godofthings;

/**
 * 配方输入助手（照抄 ae2wtlib 的 {@code InputHelper}，[本地改动] WUT 食材换本模组的
 * 无耗能通用终端）。
 */
public final class WtWutInputHelper
{
    private WtWutInputHelper()
    {
    }

    public static ItemStack getInputStack(CraftingInput input, Ingredient ingredient)
    {
        for (var stack : input.items())
        {
            if (ingredient.test(stack))
                return stack;
        }
        return ItemStack.EMPTY;
    }

    public static final Ingredient WUT = Ingredient
            .of(Godofthings.WIRELESS_UNIVERSAL_TERMINAL_FREE.get());
}
