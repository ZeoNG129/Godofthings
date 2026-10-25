package com.godofthings.wt.terminals;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.api.storage.ITerminalHost;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.menu.SlotSemantics;
import appeng.menu.ToolboxMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.PatternAccessTermMenu;
import appeng.menu.slot.RestrictedInputSlot;

import com.godofthings.Godofthings;
import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtUniversalTerminalItem;

/**
 * 无线样板管理终端菜单（照抄 ae2wtlib 的 {@code WATMenu}）。
 */
public class WtAccessMenu extends PatternAccessTermMenu
{
    public static final ResourceLocation ID = Godofthings.godofthingsId("wt_wireless_pattern_access_terminal");
    public static final MenuType<WtAccessMenu> TYPE = MenuTypeBuilder
            .create(WtAccessMenu::new, WtAccessMenuHost.class)
            .buildUnregistered(ID);

    private final WtAccessMenuHost watMenuHost;
    private final ToolboxMenu toolboxMenu;

    public WtAccessMenu(int id, final Inventory ip, final WtAccessMenuHost anchor)
    {
        super(TYPE, id, ip, anchor, true);
        watMenuHost = anchor;
        toolboxMenu = new ToolboxMenu(this);

        IUpgradeInventory upgrades = watMenuHost.getUpgrades();
        for (int i = 0; i < upgrades.size(); i++)
        {
            var slot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.UPGRADES, upgrades, i);
            slot.setNotDraggable();
            addSlot(slot, SlotSemantics.UPGRADE);
        }
        addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                watMenuHost.getSubInventory(WtMenuHost.INV_SINGULARITY), 0), WtSlotSemantics.SINGULARITY);
    }

    @Override
    public void broadcastChanges()
    {
        toolboxMenu.tick();
        super.broadcastChanges();
    }

    public appeng.api.storage.ISubMenuHost getHost()
    {
        return watMenuHost;
    }

    public boolean isWUT()
    {
        return watMenuHost.getItemStack().getItem() instanceof WtUniversalTerminalItem;
    }

    public ITerminalHost getWtHost()
    {
        return watMenuHost;
    }

    public ToolboxMenu getToolbox()
    {
        return toolboxMenu;
    }
}
