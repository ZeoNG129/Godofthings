package com.godofthings.wt.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.api.config.IncludeExclude;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;

import com.godofthings.wt.terminals.WtMagnetMenu;

/**
 * 磁卡过滤器屏幕（照抄 ae2wtlib 的 {@code MagnetScreen}，[本地改动] 图标与提示文本换
 * 本模组实现）。
 */
public class WtMagnetScreen extends AEBaseScreen<WtMagnetMenu>
{
    public WtMagnetScreen(WtMagnetMenu menu, Inventory playerInventory, Component title, ScreenStyle style)
    {
        super(menu, playerInventory, title, style);
        appeng.client.gui.implementations.AESubScreen.addBackButton(menu, "back", widgets);
        if (menu.getMagnetHost() == null)
            return;

        widgets.add("pickup_mode", new WtIconButton(button -> menu.togglePickupMode(), WtIcon.YES)
        {
            @Override
            protected WtIcon getIcon()
            {
                return icon(menu.getMagnetHost().getPickupMode());
            }

            @Override
            public Component getMessage()
            {
                return Component.translatable("gui.godofthings.pickup_filter",
                        modeName(menu.getMagnetHost().getPickupMode()));
            }
        });

        widgets.add("insert_mode", new WtIconButton(button -> menu.toggleInsertMode(), WtIcon.YES)
        {
            @Override
            protected WtIcon getIcon()
            {
                return icon(menu.getMagnetHost().getInsertMode());
            }

            @Override
            public Component getMessage()
            {
                return Component.translatable("gui.godofthings.insert_filter",
                        modeName(menu.getMagnetHost().getInsertMode()));
            }
        });

        widgets.add("copy_up", new WtIconButton(button -> menu.copyUp(), WtIcon.UP)
                .withTooltip(Component.translatable("gui.godofthings.copy_pickup")));

        widgets.add("copy_down",
                new WtIconButton(button -> menu.copyDown(), WtIcon.DOWN)
                        .withTooltip(Component.translatable("gui.godofthings.copy_insert")));

        widgets.add("switch",
                new WtIconButton(button -> menu.switchInsertPickup(), WtIcon.SWITCH)
                        .withTooltip(Component.translatable("gui.godofthings.switch")));
    }

    private WtIcon icon(IncludeExclude includeExclude)
    {
        return switch (includeExclude)
        {
            case WHITELIST -> WtIcon.YES;
            case BLACKLIST -> WtIcon.NO;
        };
    }

    private Component modeName(IncludeExclude includeExclude)
    {
        return switch (includeExclude)
        {
            case WHITELIST -> Component.translatable("gui.godofthings.allow");
            case BLACKLIST -> Component.translatable("gui.godofthings.deny");
        };
    }

    // Added to remove the VerticalButtonBar for this Screen
    @Override
    protected boolean shouldAddToolbar()
    {
        return false;
    }
}
