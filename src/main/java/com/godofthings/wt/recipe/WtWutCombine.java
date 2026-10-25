package com.godofthings.wt.recipe;

import com.mojang.datafixers.util.Unit;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.godofthings.wt.core.WtTerminalDefinition;

/**
 * 双终端合并配方（照抄 ae2wtlib 的 {@code Combine}，[本地改动] 定义类型换
 * {@link WtTerminalDefinition}，产物换本模组通用终端）。
 */
public class WtWutCombine extends WtWutCommon
{
    public static final WtWutCombineSerializer SERIALIZER = new WtWutCombineSerializer();
    private final Ingredient terminalA;
    private final Ingredient terminalB;
    private final WtTerminalDefinition terminalADefinition;
    private final WtTerminalDefinition terminalBDefinition;

    public WtWutCombine(Ingredient terminalA, Ingredient terminalB, WtTerminalDefinition terminalADefinition,
            WtTerminalDefinition terminalBDefinition)
    {
        this.terminalA = terminalA;
        this.terminalB = terminalB;
        this.terminalADefinition = terminalADefinition;
        this.terminalBDefinition = terminalBDefinition;
        outputStack.set(terminalADefinition.componentType().type(), Unit.INSTANCE);
        outputStack.set(terminalBDefinition.componentType().type(), Unit.INSTANCE);
    }

    public Ingredient getTerminalA()
    {
        return terminalA;
    }

    public Ingredient getTerminalB()
    {
        return terminalB;
    }

    public WtTerminalDefinition getTerminalADefinition()
    {
        return terminalADefinition;
    }

    public WtTerminalDefinition getTerminalBDefinition()
    {
        return terminalBDefinition;
    }

    @Override
    public boolean matches(CraftingInput inv, Level world)
    {
        return !WtWutInputHelper.getInputStack(inv, terminalA).isEmpty()
                && !WtWutInputHelper.getInputStack(inv, terminalB).isEmpty() && inv.ingredientCount() == 2;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider provider)
    {
        ItemStack wut = mergeTerminal(outputStack.copy(), WtWutInputHelper.getInputStack(inv, terminalA),
                terminalADefinition);
        wut = mergeTerminal(wut, WtWutInputHelper.getInputStack(inv, terminalB), terminalBDefinition);

        return wut;
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public NonNullList<Ingredient> getIngredients()
    {
        NonNullList<Ingredient> inputs = NonNullList.create();
        inputs.add(terminalA);
        inputs.add(terminalB);
        return inputs;
    }
}
