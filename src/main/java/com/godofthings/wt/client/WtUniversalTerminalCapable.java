package com.godofthings.wt.client;

import java.util.ArrayList;

import net.neoforged.neoforge.network.PacketDistributor;

import appeng.client.Hotkeys;
import appeng.client.gui.WidgetContainer;
import appeng.core.network.serverbound.HotkeyPacket;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;

import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtTerminalDefinition;
import com.godofthings.wt.network.WtNetwork;
import com.godofthings.wt.terminals.WtSlotSemantics;

/**
 * 通用终端屏幕能力（照抄 ae2wtlib 的 {@code IUniversalTerminalCapable}，[本地改动]
 * 循环/热键动作走本模组的网络包与注册表）。
 */
public interface WtUniversalTerminalCapable
{
    default void cycleTerminal()
    {
        storeState();
        PacketDistributor.sendToServer(new WtNetwork.CycleTerminal(isHandlingRightClick()));
    }

    WtMenuHost getHost();

    boolean isHandlingRightClick();

    void storeState();

    default boolean checkForTerminalKeys(int keyCode, int scanCode)
    {
        var closingHotkey = Hotkeys.getHotkeyMapping(getHost().getCloseHotkey());
        if (closingHotkey != null && closingHotkey.mapping().matches(keyCode, scanCode))
        {
            getHost().getPlayer().closeContainer();
            return true;
        }

        for (var terminal : WtTerminalDefinition.wirelessTerminalList())
        {
            var hotkey = Hotkeys.getHotkeyMapping(terminal.hotkeyName());
            if (hotkey == null)
                continue;
            if (hotkey.mapping().matches(keyCode, scanCode))
            {
                PacketDistributor.sendToServer(new HotkeyPacket(hotkey));
                return true;
            }
        }
        return false;
    }

    /**
     * creates the button that opens the terminal selector. you are responsible for adding this
     * to the leftToolbar when appropriate
     */
    default WtIconButton cycleTerminalButton()
    {
        return new WtTerminalSelectionButton(getHost(), this::storeState);
    }

    /**
     * creates and adds the upgrade panel
     *
     * @param widgets the WidgetContainer where the widget will be added
     * @param menu    the menu corresponding to this screen
     */
    default WtScrollingUpgradesPanel addUpgradePanel(WidgetContainer widgets, AEBaseMenu menu)
    {
        var upgrades = new ArrayList<>(menu.getSlots(WtSlotSemantics.SINGULARITY));
        upgrades.addAll(menu.getSlots(SlotSemantics.UPGRADE));
        var panel = new WtScrollingUpgradesPanel(upgrades, getHost(), widgets, () -> getHost().getUpgrades());
        widgets.add("scrollingUpgrades", panel);
        return panel;
    }
}
