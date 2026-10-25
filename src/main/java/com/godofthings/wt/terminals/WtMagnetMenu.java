package com.godofthings.wt.terminals;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.AppEngSlot;
import appeng.util.ConfigMenuInventory;

import com.godofthings.Godofthings;
import com.godofthings.wt.core.WtCraftingTerminalHandler;
import com.godofthings.wt.core.WtMagnetHost;
import com.godofthings.wt.terminals.WtCraftingMenuHost;

/**
 * 磁卡设置子菜单（照抄 ae2wtlib 的 {@code MagnetMenu}）。
 */
public class WtMagnetMenu extends AEBaseMenu implements ISubMenu
{
    public static final ResourceLocation ID = Godofthings.godofthingsId("wt_magnet");
    public static final MenuType<WtMagnetMenu> TYPE = MenuTypeBuilder
            .create(WtMagnetMenu::new, WtCraftingMenuHost.class)
            .buildUnregistered(ID);

    private final WtCraftingMenuHost host;
    private final WtMagnetHost magnetHost;
    private final static String TOGGLE_PICKUP_MODE = "togglepickupmode";
    private final static String TOGGLE_INSERT_MODE = "toggleinsertmode";
    private final static String COPY_UP = "copy_up";
    private final static String COPY_DOWN = "copy_down";
    private final static String SWITCH_INSERT_PICKUP = "switch";

    public WtMagnetMenu(int id, Inventory playerInventory, WtCraftingMenuHost host)
    {
        super(TYPE, id, playerInventory, host);
        this.host = host;

        magnetHost = WtCraftingTerminalHandler.get(playerInventory.player).getMagnetHost();

        if (magnetHost == null)
        {
            return;
        }

        addConfigSlots(magnetHost.pickupConfig, WtSlotSemantics.PICKUP_CONFIG);
        addConfigSlots(magnetHost.insertConfig, WtSlotSemantics.INSERT_CONFIG);
        createPlayerInventorySlots(playerInventory);
        registerClientAction(TOGGLE_PICKUP_MODE, this::togglePickupMode);
        registerClientAction(TOGGLE_INSERT_MODE, this::toggleInsertMode);
        registerClientAction(COPY_UP, this::copyUp);
        registerClientAction(COPY_DOWN, this::copyDown);
        registerClientAction(SWITCH_INSERT_PICKUP, this::switchInsertPickup);
    }

    private void addConfigSlots(appeng.util.ConfigInventory config, appeng.menu.SlotSemantic slotSemantic)
    {
        ConfigMenuInventory inv = config.createMenuWrapper();

        for (int y = 0; y < 3; y++)
        {
            for (int x = 0; x < 9; x++)
            {
                addSlot(new FakeSlotWrapper(inv, y * 9 + x), slotSemantic);
            }
        }
    }

    @Override
    public ISubMenuHost getHost()
    {
        return host;
    }

    public WtMagnetHost getMagnetHost()
    {
        return magnetHost;
    }

    public void togglePickupMode()
    {
        if (isClientSide())
        {
            sendClientAction(TOGGLE_PICKUP_MODE);
        }
        if (magnetHost == null)
        {
            return;
        }
        magnetHost.togglePickupMode();
    }

    public void toggleInsertMode()
    {
        if (isClientSide())
        {
            sendClientAction(TOGGLE_INSERT_MODE);
        }
        if (magnetHost == null)
        {
            return;
        }
        magnetHost.toggleInsertMode();
    }

    public void copyUp()
    {
        if (isClientSide())
        {
            sendClientAction(COPY_UP);
        }
        if (magnetHost == null)
        {
            return;
        }
        magnetHost.copyUp();
    }

    public void copyDown()
    {
        if (isClientSide())
        {
            sendClientAction(COPY_DOWN);
        }
        if (magnetHost == null)
        {
            return;
        }
        magnetHost.copyDown();
    }

    public void switchInsertPickup()
    {
        if (isClientSide())
        {
            sendClientAction(SWITCH_INSERT_PICKUP);
        }
        if (magnetHost == null)
        {
            return;
        }
        magnetHost.switchInsertPickup();
    }

    /** appeng.menu.slot.FakeSlot 的本地引用（避免与 AE2 版本差异）。 */
    private static class FakeSlotWrapper extends appeng.menu.slot.FakeSlot
    {
        public FakeSlotWrapper(ConfigMenuInventory inv, int slotIndex)
        {
            super(inv, slotIndex);
        }
    }
}
