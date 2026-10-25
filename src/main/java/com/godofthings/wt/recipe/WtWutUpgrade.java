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
        if (wut.isEmpty() || inv.ingredientCount() != 2)
        {
            return false;
        }
        // [本地改动] v7.15.5：标准食材（AE2 终端）不匹配时，退而接受任意 AE2 无线终端
        // （附属模组终端）作为「额外终端」合并——与原版 wtlib 的开放生态对齐。
        ItemStack standard = WtWutInputHelper.getInputStack(inv, terminal);
        if (!standard.isEmpty())
        {
            return wut.get(terminalDefinition.componentType().type()) == null;
        }
        for (var stack : inv.items())
        {
            if (WtExtraTerminals.isMergeable(stack))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider provider)
    {
        ItemStack standard = WtWutInputHelper.getInputStack(inv, terminal);
        if (!standard.isEmpty())
        {
            return mergeTerminal(WtWutInputHelper.getInputStack(inv, WtWutInputHelper.WUT).copy(),
                    standard.copy(), terminalDefinition);
        }
        // 附属终端合并分支
        for (var stack : inv.items())
        {
            if (WtExtraTerminals.isMergeable(stack))
            {
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
        inputs.add(terminal);
        inputs.add(WtWutInputHelper.WUT);
        return inputs;
    }
}
