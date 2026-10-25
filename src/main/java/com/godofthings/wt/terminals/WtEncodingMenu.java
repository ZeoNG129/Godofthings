package com.godofthings.wt.terminals;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.RestrictedInputSlot;

import com.godofthings.Godofthings;
import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtUniversalTerminalItem;

/**
 * 无线样板终端菜单（照抄 ae2wtlib 的 {@code WETMenu}）。
 */
public class WtEncodingMenu extends PatternEncodingTermMenu
{
    public static final ResourceLocation ID = Godofthings.godofthingsId("wt_wireless_pattern_encoding_terminal");
    public static final MenuType<WtEncodingMenu> TYPE = MenuTypeBuilder
            .create(WtEncodingMenu::new, WtEncodingMenuHost.class)
            .buildUnregistered(ID);

    private final WtEncodingMenuHost wetMenuHost;

    public WtEncodingMenu(int id, final Inventory ip, final WtEncodingMenuHost gui)
    {
        super(TYPE, id, ip, gui, true);
        wetMenuHost = gui;
        addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                wetMenuHost.getSubInventory(WtMenuHost.INV_SINGULARITY), 0), WtSlotSemantics.SINGULARITY);
    }

    @Override
    public appeng.api.networking.IGridNode getGridNode()
    {
        return wetMenuHost.getActionableNode();
    }

    public boolean isWUT()
    {
        return wetMenuHost.getItemStack().getItem() instanceof WtUniversalTerminalItem;
    }

    public WtMenuHost getWtHost()
    {
        return wetMenuHost;
    }
}
