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
 * 两台终端合成一台无耗能通用终端（照抄 ae2wtlib 的 {@code Combine}，[本地改动] 见基类）。
 */
public class WutfreeCombine extends WutfreeCommon
{
    public static final WutfreeCombineSerializer serializer = new WutfreeCombineSerializer();
    private final Ingredient terminalA;
    private final Ingredient terminalB;
    private final WTDefinition terminalADefinition;
    private final WTDefinition terminalBDefinition;

    public WutfreeCombine(Ingredient terminalA, Ingredient terminalB, WTDefinition terminalADefinition,
            WTDefinition terminalBDefinition)
    {
        this.terminalA = terminalA;
        this.terminalB = terminalB;
        this.terminalADefinition = terminalADefinition;
        this.terminalBDefinition = terminalBDefinition;
        outputStack.set(terminalADefinition.componentType(), Unit.INSTANCE);
        outputStack.set(terminalBDefinition.componentType(), Unit.INSTANCE);
    }

    public Ingredient getTerminalA()
    {
        return terminalA;
    }

    public Ingredient getTerminalB()
    {
        return terminalB;
    }

    public WTDefinition getTerminalADefinition()
    {
        return terminalADefinition;
    }

    public WTDefinition getTerminalBDefinition()
    {
        return terminalBDefinition;
    }

    @Override
    public boolean matches(CraftingInput inv, Level world)
    {
        return !getInputStack(inv, terminalA).isEmpty()
                && !getInputStack(inv, terminalB).isEmpty() && inv.ingredientCount() == 2;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider provider)
    {
        ItemStack wut = mergeTerminal(outputStack.copy(), getInputStack(inv, terminalA),
                terminalADefinition);
        wut = mergeTerminal(wut, getInputStack(inv, terminalB), terminalBDefinition);

        return wut;
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
        inputs.add(terminalA);
        inputs.add(terminalB);
        return inputs;
    }
}
