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
 * 单终端升级配方（照抄 ae2wtlib 的 {@code Upgrade}，[本地改动] 定义类型换
 * {@link WtTerminalDefinition}，产物换本模组通用终端）。
 */
public class WtWutUpgrade extends WtWutCommon
{
    public static final WtWutUpgradeSerializer SERIALIZER = new WtWutUpgradeSerializer();
    private final Ingredient terminal;
    private final WtTerminalDefinition terminalDefinition;

    public WtWutUpgrade(Ingredient terminal, WtTerminalDefinition terminalDefinition)
    {
        this.terminal = terminal;
        this.terminalDefinition = terminalDefinition;
        outputStack.set(terminalDefinition.componentType().type(), Unit.INSTANCE);
    }

    public Ingredient getTerminal()
    {
        return terminal;
    }

    public WtTerminalDefinition getTerminalDefinition()
    {
        return terminalDefinition;
    }

    @Override
    public boolean matches(CraftingInput inv, Level world)
    {
        ItemStack wut = WtWutInputHelper.getInputStack(inv, WtWutInputHelper.WUT);
        return !WtWutInputHelper.getInputStack(inv, terminal).isEmpty() && !wut.isEmpty()
                && inv.ingredientCount() == 2
                && wut.get(terminalDefinition.componentType().type()) == null;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider provider)
    {
        return mergeTerminal(WtWutInputHelper.getInputStack(inv, WtWutInputHelper.WUT).copy(),
                WtWutInputHelper.getInputStack(inv, terminal).copy(), terminalDefinition);
    }

    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    public NonNullList<Ingredient> getIngredients()
    {
        NonNullList<Ingredient> inputs = NonNullList.create();
        inputs.add(terminal);
        inputs.add(WtWutInputHelper.WUT);
        return inputs;
    }
}
