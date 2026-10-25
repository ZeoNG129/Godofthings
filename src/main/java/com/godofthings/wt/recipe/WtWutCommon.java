package com.godofthings.wt.recipe;

import java.util.Iterator;

import com.mojang.datafixers.util.Unit;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.upgrades.IUpgradeInventory;

import com.godofthings.Godofthings;
import com.godofthings.wt.core.WtTerminalDefinition;
import com.godofthings.wt.core.WtTerminalItem;
import com.godofthings.wt.core.WtUniversalTerminalItem;

/**
 * 通用终端合成配方基类（照抄 ae2wtlib 的 {@code Common}，[本地改动] 产物换本模组的
 * 无耗能通用终端，物品类型判断换本模组的 {@link WtTerminalItem} / {@link WtUniversalTerminalItem}）。
 */
public abstract class WtWutCommon implements CraftingRecipe
{
    protected final ItemStack outputStack = new ItemStack(Godofthings.WIRELESS_UNIVERSAL_TERMINAL_FREE.get());

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
     *
     * @param wut      the wireless universal terminal a terminal should be added to
     * @param toMerge  the terminal to add
     * @param terminal the terminal to add
     * @return the upgraded wireless universal terminal
     */
    public static ItemStack mergeTerminal(ItemStack wut, ItemStack toMerge, WtTerminalDefinition terminal)
    {
        if (!(wut.getItem() instanceof WtUniversalTerminalItem itemWUT))
            return ItemStack.EMPTY;
        // [本地改动] 本模组只保留通用终端一台物品，合成食材是 AE2 的部件/无线终端。
        // 部件终端物品（PartItem）不是 IUpgradeableItem，升级与电量合并只对支持的食材做。
        if (toMerge.getItem() instanceof appeng.api.upgrades.IUpgradeableItem itemWT)
        {
            wut.set(terminal.componentType().type(), Unit.INSTANCE);

            // merge upgrades
            Iterator<ItemStack> iterator = itemWT.getUpgrades(toMerge).iterator();
            IUpgradeInventory wutUpgrades = itemWUT.getUpgrades(wut);
            while (iterator.hasNext())
            {
                wutUpgrades.addItems(iterator.next());
            }

            // update max power
            itemWUT.onUpgradesChanged(wut, itemWUT.getUpgrades(wut));

            // merge power（食材若是 AE2 原版终端，电流按 AEBasePoweredItem 读）
            if (toMerge.getItem() instanceof appeng.items.tools.powered.powersink.AEBasePoweredItem powered)
            {
                itemWUT.injectAEPower(wut, powered.getAECurrentPower(toMerge), Actionable.MODULATE);
            }
        }
        else
        {
            wut.set(terminal.componentType().type(), Unit.INSTANCE);
        }

        // merge nbt
        var componentsPatch = toMerge.getComponentsPatch();
        // remove the keys that are already handled
        componentsPatch = componentsPatch.forget(AEComponents.STORED_ENERGY::equals);
        componentsPatch = componentsPatch.forget(AEComponents.ENERGY_CAPACITY::equals);
        componentsPatch = componentsPatch.forget(AEComponents.UPGRADES::equals);
        wut.applyComponents(componentsPatch);

        return wut;
    }
}
