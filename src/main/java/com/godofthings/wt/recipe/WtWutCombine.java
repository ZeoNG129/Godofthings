package com.godofthings.wt.recipe;

import com.mojang.datafixers.util.Unit;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.godofthings.wt.core.WtExtraTerminals;
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
        ItemStack a = WtWutInputHelper.getInputStack(inv, terminalA);
        ItemStack b = WtWutInputHelper.getInputStack(inv, terminalB);
        // [本地改动] v7.15.5：标准食材（AE2 终端）不齐时，接受「通用终端 + 任意一台 AE2
        // 无线终端（附属模组终端）」的组合——合并为额外终端（与 wtlib 开放生态对齐）。
        if (!a.isEmpty() && !b.isEmpty())
        {
            return inv.ingredientCount() == 2;
        }
        if (inv.ingredientCount() != 2)
        {
            return false;
        }
        boolean hasWut = false;
        boolean hasExtra = false;
        for (var stack : inv.items())
        {
            if (stack.getItem() instanceof com.godofthings.wt.core.WtUniversalTerminalItem)
            {
                hasWut = true;
            }
            else if (WtExtraTerminals.isMergeable(stack))
            {
                hasExtra = true;
            }
        }
        return hasWut && hasExtra;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider provider)
    {
        ItemStack a = WtWutInputHelper.getInputStack(inv, terminalA);
        ItemStack b = WtWutInputHelper.getInputStack(inv, terminalB);
        if (!a.isEmpty() && !b.isEmpty())
        {
            ItemStack wut = mergeTerminal(outputStack.copy(), a, terminalADefinition);
            wut = mergeTerminal(wut, b, terminalBDefinition);
            return wut;
        }
        // 附属终端合并分支
        for (var stack : inv.items())
        {
            if (WtExtraTerminals.isMergeable(stack))
            {
                // [本地改动] v7.15.6：以玩家投入的通用终端为基底——原版抄上游的
                // outputStack.copy() 在配方构造时被 set 过内建终端组件，附属合并
                // 会把这些组件连同食材一起带上（7.15.5「合成出三台终端」事故）。
                return mergeExtraTerminal(
                        WtWutInputHelper.getInputStack(inv, WtWutInputHelper.WUT).copy(), stack.copy());
            }
        }
        return ItemStack.EMPTY;
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
