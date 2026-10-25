package com.godofthings.wt.terminals;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;

import com.godofthings.Godofthings;

/**
 * 垃圾桶子菜单（照抄 ae2wtlib 的 {@code TrashMenu}）。
 */
public class WtTrashMenu extends AEBaseMenu implements ISubMenu
{
    public static final ResourceLocation ID = Godofthings.godofthingsId("wt_trash");
    public static final MenuType<WtTrashMenu> TYPE = MenuTypeBuilder
            .create(WtTrashMenu::new, WtCraftingMenuHost.class)
            .buildUnregistered(ID);

    private final WtCraftingMenuHost host;

    public WtTrashMenu(int id, Inventory playerInventory, WtCraftingMenuHost host)
    {
        super(TYPE, id, playerInventory, host);
        this.host = host;

        var trash = host.getSubInventory(WtCraftingMenuHost.INV_TRASH);
        if (trash == null)
        {
            return;
        }
        for (int i = 0; i < trash.size(); i++)
        {
            addSlot(new AppEngSlot(trash, i), WtSlotSemantics.TRASH);
        }
        createPlayerInventorySlots(playerInventory);
    }

    @Override
    public ISubMenuHost getHost()
    {
        return host;
    }
}
