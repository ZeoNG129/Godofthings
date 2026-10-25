package com.godofthings.wt.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.godofthings.Godofthings;
import com.godofthings.wt.core.WtComponents;
import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtMagnetMode;
import com.godofthings.wt.network.WtNetwork;
import com.godofthings.wt.terminals.WtCraftingMenu;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.TabButton;
import appeng.menu.SlotSemantics;

/**
 * 无线终端设置屏幕（照抄 ae2wtlib 的 {@code WirelessTerminalSettingsScreen}，[本地改动]
 * 组件键换本模组的 {@link WtComponents}，磁卡模式换 {@link WtMagnetMode}）。
 */
public class WtTerminalSettingsScreen extends AESubScreen<WtCraftingMenu, WtCraftingScreen>
{
    private final AECheckbox pickBlock = widgets.addCheckbox("pickBlock",
            Component.translatable("gui.godofthings.pick_block.text"), this::changeVisibility);
    private final AECheckbox craftIfMissing = widgets.addCheckbox("craftIfMissing",
            Component.translatable("gui.godofthings.craft_if_missing.text"), this::save);
    private final AECheckbox restock = widgets.addCheckbox("restock",
            Component.translatable("gui.godofthings.restock.text"), this::save);
    private final AECheckbox magnet = widgets.addCheckbox("magnet",
            Component.translatable("gui.godofthings.magnet.text"), this::save);
    private final AECheckbox pickupToME = widgets.addCheckbox("pickupToME",
            Component.translatable("gui.godofthings.pickup_to_me.text"), this::save);

    public WtTerminalSettingsScreen(WtCraftingScreen parent)
    {
        super(parent, "/screens/wutfree/wireless_terminal_settings.json");
        widgets.add("back",
                new TabButton(Icon.BACK, menu.getHost().getMainMenuIcon().getHoverName(), btn -> returnToParent()));

        pickBlock.setSelected(stack().getOrDefault(WtComponents.PICK_BLOCK, false));
        craftIfMissing.setSelected(stack().getOrDefault(WtComponents.CRAFT_IF_MISSING, false));
        craftIfMissing.active = pickBlock.isSelected();
        restock.setSelected(stack().getOrDefault(WtComponents.RESTOCK, false));
        magnet.setSelected(stack().getOrDefault(Godofthings.WT_MAGNET_SETTINGS, WtMagnetMode.OFF).magnet());
        pickupToME.setSelected(
                stack().getOrDefault(Godofthings.WT_MAGNET_SETTINGS, WtMagnetMode.OFF).pickupToME());
        if (com.godofthings.wt.core.WtMagnetHandler.getMagnetMode(stack()) == WtMagnetMode.NO_CARD)
        {
            magnet.active = false;
            pickupToME.active = false;
        }
    }

    @Override
    protected void init()
    {
        super.init();
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }

    private ItemStack stack()
    {
        return ((WtMenuHost) getMenu().getHost()).getItemStack();
    }

    private void changeVisibility()
    {
        craftIfMissing.active = pickBlock.isSelected();
        save();
    }

    private void save()
    {
        var locator = ((WtMenuHost) getMenu().getHost()).getLocator();
        if (locator == null)
            return;
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new WtNetwork.TerminalSettings(locator,
                pickBlock.isSelected(), restock.isSelected(), magnet.isSelected(), pickupToME.isSelected(),
                craftIfMissing.isSelected()));
    }
}
