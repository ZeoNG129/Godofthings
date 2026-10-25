package com.godofthings.ae.wt.recipe;

import com.mojang.datafixers.util.Unit;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import de.mari_023.ae2wtlib.api.registration.WTDefinition;

/**
 * 单台终端装入无耗能通用终端（照抄 ae2wtlib 的 {@code Upgrade}，[本地改动] 见基类）。
 */
public class WutfreeUpgrade extends WutfreeCommon
{
    public static final WutfreeUpgradeSerializer serializer = new WutfreeUpgradeSerializer();
    private final Ingredient terminal;
    private final WTDefinition terminalDefinition;

    public WutfreeUpgrade(Ingredient terminal, WTDefinition terminalDefinition)
    {
        this.terminal = terminal;
        this.terminalDefinition = terminalDefinition;
        outputStack.set(terminalDefinition.componentType(), Unit.INSTANCE);
    }

    public Ingredient getTerminal()
    {
        return terminal;
    }

    public WTDefinition getTerminalDefinition()
    {
        return terminalDefinition;
    }

    @Override
    public boolean matches(CraftingInput inv, Level world)
    {
        ItemStack wut = getInputStack(inv, WUT_INGREDIENT);
        return !getInputStack(inv, terminal).isEmpty() && !wut.isEmpty() && inv.ingredientCount() == 2
                && wut.get(terminalDefinition.componentType()) == null;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider provider)
    {
        return mergeTerminal(getInputStack(inv, WUT_INGREDIENT).copy(),
                getInputStack(inv, terminal).copy(), terminalDefinition);
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return serializer;
    }

    @Override
    public NonNullList<Ingredient> getIngredients()
    {
        NonNullList<Ingredient> inputs = NonNullList.create();
        inputs.add(terminal);
        inputs.add(WUT_INGREDIENT);
        return inputs;
    }
}
