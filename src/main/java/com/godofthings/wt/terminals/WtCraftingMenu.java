package com.godofthings.wt.terminals;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;

import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.RestrictedInputSlot;

import com.godofthings.Godofthings;
import com.godofthings.wt.client.WtArmorSlot;
import com.godofthings.wt.core.WtMagnetHandler;
import com.godofthings.wt.core.WtMagnetMode;
import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtUniversalTerminalItem;


/**
 * 无线合成终端菜单（照抄 ae2wtlib 的 {@code WCTMenu}）。
 */
public class WtCraftingMenu extends CraftingTermMenu
{
    public static final ResourceLocation ID = Godofthings.godofthingsId("wt_wireless_crafting_terminal");
    public static final MenuType<WtCraftingMenu> TYPE = MenuTypeBuilder
            .create(WtCraftingMenu::new, WtCraftingMenuHost.class)
            .buildUnregistered(ID);

    public static final String MAGNET_MODE = "magnetMode";
    public static final String MAGNET_MENU = "magnetMenu";
    public static final String TRASH_MENU = "trash";

    private final WtCraftingMenuHost wctMenuHost;

    public WtCraftingMenu(int id, final Inventory ip, final WtCraftingMenuHost gui)
    {
        super(TYPE, id, ip, gui, true);
        wctMenuHost = gui;

        addSlot(new WtArmorSlot(getPlayerInventory(), WtArmorSlot.Armor.HEAD)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return (stack.getItem() instanceof BlockItem bItem && bItem.getBlock() instanceof Equipable)
                        || super.mayPlace(stack);
            }
        }, WtSlotSemantics.HELMET);
        addSlot(new WtArmorSlot(getPlayerInventory(), WtArmorSlot.Armor.CHEST), WtSlotSemantics.CHESTPLATE);
        addSlot(new WtArmorSlot(getPlayerInventory(), WtArmorSlot.Armor.LEGS), WtSlotSemantics.LEGGINGS);
        addSlot(new WtArmorSlot(getPlayerInventory(), WtArmorSlot.Armor.FEET), WtSlotSemantics.BOOTS);

        if (Integer.valueOf(Inventory.SLOT_OFFHAND).equals(wctMenuHost.getPlayerInventorySlot()))
            addSlot(new WtArmorSlot.DisabledOffhandSlot(getPlayerInventory()), WtSlotSemantics.OFFHAND);
        else
            addSlot(new WtArmorSlot(getPlayerInventory(), WtArmorSlot.Armor.OFFHAND), WtSlotSemantics.OFFHAND);
        addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                wctMenuHost.getSubInventory(WtMenuHost.INV_SINGULARITY), 0), WtSlotSemantics.SINGULARITY);

        registerClientAction(MAGNET_MODE, WtMagnetMode.class, this::setMagnetMode);
        registerClientAction(MAGNET_MENU, this::openMagnetMenu);
        registerClientAction(TRASH_MENU, this::openTrashMenu);
    }

    @Override
    public appeng.api.networking.IGridNode getGridNode()
    {
        return wctMenuHost.getActionableNode();
    }

    public WtMagnetMode getMagnetMode()
    {
        return WtMagnetHandler.getMagnetMode(wctMenuHost.getItemStack());
    }

    public void setMagnetMode(WtMagnetMode mode)
    {
        if (isClientSide())
        {
            sendClientAction(MAGNET_MODE, mode);
        }
        WtMagnetHandler.saveMagnetMode(wctMenuHost.getItemStack(), mode);
    }

    public void openMagnetMenu()
    {
        if (isClientSide())
        {
            sendClientAction(MAGNET_MENU);
            return;
        }
        appeng.menu.MenuOpener.open(WtMagnetMenu.TYPE, getPlayer(), getLocator());
    }

    public void openTrashMenu()
    {
        if (isClientSide())
        {
            sendClientAction(TRASH_MENU);
            return;
        }
        appeng.menu.MenuOpener.open(WtTrashMenu.TYPE, getPlayer(), getLocator());
    }

    @Override
    protected boolean canSlotsBeHidden(appeng.menu.SlotSemantic semantic)
    {
        return semantic == WtSlotSemantics.OFFHAND
                || semantic == WtSlotSemantics.HELMET
                || semantic == WtSlotSemantics.CHESTPLATE
                || semantic == WtSlotSemantics.LEGGINGS
                || semantic == WtSlotSemantics.BOOTS;
    }

    public boolean isWUT()
    {
        return wctMenuHost.getItemStack().getItem() instanceof WtUniversalTerminalItem;
    }

    public WtMenuHost getWtHost()
    {
        return wctMenuHost;
    }
}
