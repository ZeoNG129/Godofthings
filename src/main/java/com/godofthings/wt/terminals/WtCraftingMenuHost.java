package com.godofthings.wt.terminals;

import java.util.function.BiConsumer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.ids.AEComponents;
import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.api.inventories.InternalInventory;
import appeng.items.contents.StackDependentSupplier;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.parts.reporting.CraftingTerminalPart;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.SupplierInternalInventory;

import com.godofthings.wt.core.WtComponents;
import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtTerminalItem;

/**
 * 无线合成终端宿主（照抄 ae2wtlib 的 {@code WCTMenuHost}）。
 */
public class WtCraftingMenuHost extends WtMenuHost implements IViewCellStorage
{
    private final SupplierInternalInventory<InternalInventory> craftingGrid;
    private final AppEngInternalInventory trash = new AppEngInternalInventory(27);
    public static final net.minecraft.resources.ResourceLocation INV_TRASH =
            WtComponents.godofthingsId("wct_trash");

    public WtCraftingMenuHost(WtTerminalItem item, Player player, ItemMenuHostLocator locator,
            BiConsumer<Player, ISubMenu> returnToMainMenu)
    {
        super(item, player, locator, returnToMainMenu);
        this.craftingGrid = new SupplierInternalInventory<>(
                new StackDependentSupplier<>(
                        this::getItemStack,
                        stack -> createInv(player, stack, AEComponents.CRAFTING_INV, 9)));
    }

    @Override
    public InternalInventory getSubInventory(net.minecraft.resources.ResourceLocation id)
    {
        if (id.equals(CraftingTerminalPart.INV_CRAFTING))
        {
            return craftingGrid;
        }
        if (id.equals(INV_TRASH))
        {
            return trash;
        }
        return super.getSubInventory(id);
    }
}
