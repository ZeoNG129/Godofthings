package com.godofthings.ae.wt.recipe;

import java.util.Iterator;

import com.mojang.datafixers.util.Unit;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.upgrades.IUpgradeInventory;

import de.mari_023.ae2wtlib.api.registration.WTDefinition;
import de.mari_023.ae2wtlib.api.terminal.ItemWUT;

/**
 * 无耗能通用终端的合成基类（照抄 ae2wtlib 的 {@code Common}，[本地改动] 仅产物物品换成
 * 本模组的无耗能 WUT）。
 *
 * <p>上游 {@code Common.outputStack} 在字段初始化时硬编码 {@code new ItemStack(上游 WUT)}；
 * combine（两台终端合一）与 upgrade（单台终端装入 WUT）两类配方共用它的
 * {@link #mergeTerminal} 合并逻辑。本类逐行照抄该逻辑，只把产物换成
 * {@code godofthings:wireless_universal_terminal_free}——合并时检查
 * {@code wut.getItem() instanceof ItemWUT}，本地 WUT 继承自上游 {@link ItemWUT}，
 * 因此升级合并、组件平移、能量转移全部原生工作；上游能量组件
 * （STORED_ENERGY / ENERGY_CAPACITY）随之写入本物品但无耗能实现不消费它们。</p>
 */
public abstract class WutfreeCommon implements CraftingRecipe
{
    // [本地改动] 产物物品：上游是 AE2wtlibItems.UNIVERSAL_TERMINAL
    protected final ItemStack outputStack = new ItemStack(
            com.godofthings.Godofthings.WIRELESS_UNIVERSAL_TERMINAL_FREE.get());

    // [本地改动] upgrade 类配方的 WUT 输入判定：上游 InputHelper.WUT 硬编码 ae2wtlib 的
    // UNIVERSAL_TERMINAL，这里指向本地无耗能 WUT（合成输入完全对称）。
    public static final Ingredient WUT_INGREDIENT =
            Ingredient.of(com.godofthings.Godofthings.WIRELESS_UNIVERSAL_TERMINAL_FREE.get());

    @Override
    public boolean canCraftInDimensions(int width, int height)
    {
        return width > 1 || height > 1;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider)
    {
        return outputStack;
    }

    @Override
    public CraftingBookCategory category()
    {
        return CraftingBookCategory.EQUIPMENT;
    }

    /**
     * merge a terminal into a wireless universal terminal
     * （照抄上游 Common.mergeTerminal，仅 wut/toMerge 的实例判定放宽到本地 WUT）
     */
    public static ItemStack mergeTerminal(ItemStack wut, ItemStack toMerge, WTDefinition terminal)
    {
        if (!(wut.getItem() instanceof ItemWUT itemWUT))
        {
            return ItemStack.EMPTY;
        }
        if (!(toMerge.getItem() instanceof de.mari_023.ae2wtlib.api.terminal.ItemWT itemWT))
        {
            return ItemStack.EMPTY;
        }

        wut.set(terminal.componentType(), Unit.INSTANCE);

        // merge upgrades, this also updates max energy
        Iterator<ItemStack> iterator = itemWT.getUpgrades(toMerge).iterator();
        IUpgradeInventory wutUpgrades = itemWUT.getUpgrades(wut);
        while (iterator.hasNext())
        {
            wutUpgrades.addItems(iterator.next());
        }

        // update max power
        itemWUT.onUpgradesChanged(wut, itemWUT.getUpgrades(wut));

        // merge power
        itemWUT.injectAEPower(wut, itemWT.getAECurrentPower(toMerge), Actionable.MODULATE);

        // merge nbt
        var componentsPatch = toMerge.getComponentsPatch();
        // remove the keys that are already handled
        componentsPatch = componentsPatch.forget(AEComponents.STORED_ENERGY::equals);
        componentsPatch = componentsPatch.forget(AEComponents.ENERGY_CAPACITY::equals);
        componentsPatch = componentsPatch.forget(AEComponents.UPGRADES::equals);
        wut.applyComponents(componentsPatch);

        return wut;
    }

    /** 找到合成格里匹配指定成分的物品（照抄上游 InputHelper.getInputStack）。 */
    public static ItemStack getInputStack(net.minecraft.world.item.crafting.CraftingInput input, Ingredient ingredient)
    {
        for (var stack : input.items())
        {
            if (ingredient.test(stack))
            {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
