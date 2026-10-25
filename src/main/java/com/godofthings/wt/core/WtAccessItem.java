package com.godofthings.wt.core;

import java.util.function.Supplier;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.Settings;
import appeng.api.config.ShowPatternProviders;
import appeng.api.util.IConfigManager;
import appeng.menu.locator.ItemMenuHostLocator;

import com.godofthings.wt.terminals.WtAccessMenu;

/**
 * 无线样板管理终端物品（照抄 ae2wtlib 的 {@code ItemWAT}）。
 */
public class WtAccessItem extends WtTerminalItem
{
    @Override
    public MenuType<?> getMenuType(ItemMenuHostLocator locator, Player player)
    {
        return WtAccessMenu.TYPE;
    }

    @Override
    public IConfigManager getConfigManager(Supplier<ItemStack> target)
    {
        return WtConfigManager.builder(target)
                .registerSetting(Settings.TERMINAL_SHOW_PATTERN_PROVIDERS, ShowPatternProviders.VISIBLE)
                .build();
    }
}
