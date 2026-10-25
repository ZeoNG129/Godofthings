package com.godofthings.wt.core;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.util.IConfigManager;
import appeng.core.AEConfig;
import appeng.helpers.WirelessTerminalMenuHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.menu.locator.MenuLocators;

/**
 * 无线终端基类（照抄 ae2wtlib 的 {@code ItemWT}，[本地改动] 打开放行条件只认本模组的
 * 通用终端；能量五连继承自 {@link WtPoweredItem}）。
 */
public abstract class WtTerminalItem extends WtPoweredItem
{
    public WtTerminalItem()
    {
        super(AEConfig.instance().getWirelessTerminalBattery(), new Item.Properties().stacksTo(1));
    }

    public boolean open(Player player, ItemMenuHostLocator locator, boolean returningFromSubmenu)
    {
        return MenuOpener.open(getMenuType(locator, player), player, locator, returningFromSubmenu);
    }

    public boolean tryOpen(Player player, ItemMenuHostLocator locator, boolean returningFromSubmenu)
    {
        if (checkPreconditions(locator.locateItem(player)))
        {
            return open(player, locator, returningFromSubmenu);
        }
        return false;
    }

    public abstract MenuType<?> getMenuType(ItemMenuHostLocator locator, Player player);

    protected boolean checkPreconditions(ItemStack item)
    {
        // 只保留通用终端一台物品：任何 WtTerminalItem 的 use 都放行通用终端（独立终端物品
        // 已删，merging 判断也在配方包里对 AE2 食材放宽）
        return !item.isEmpty()
                && (item.getItem() == this || item.getItem() instanceof WtUniversalTerminalItem);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack is = player.getItemInHand(hand);
        if (checkPreconditions(is))
        {
            if (!level.isClientSide())
            {
                open(player, MenuLocators.forHand(player, hand), false);
            }
            return new InteractionResultHolder<>(InteractionResult.sidedSuccess(level.isClientSide()), is);
        }
        return new InteractionResultHolder<>(InteractionResult.FAIL, is);
    }

    @Override
    public WirelessTerminalMenuHost<?> getMenuHost(Player player, ItemMenuHostLocator locator,
            @Nullable BlockHitResult hitResult)
    {
        return WtTerminalDefinition.of(locator.locateItem(player)).wTMenuHostFactory().create(this, player,
                locator, (p, subMenu) -> tryOpen(player, locator, true));
    }

    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot)
    {
        return true;
    }

    /**
     * 终端的配置管理器（照抄 ItemWT.getConfigManager，走 {@link WtConfigManager} 以保留未知设置值）。
     */
    public IConfigManager getConfigManager(Supplier<ItemStack> target)
    {
        return WtConfigManager.builder(target)
                .registerSetting(appeng.api.config.Settings.SORT_BY, SortOrder.NAME)
                .registerSetting(appeng.api.config.Settings.VIEW_MODE, ViewItems.ALL)
                .registerSetting(appeng.api.config.Settings.SORT_DIRECTION, SortDir.ASCENDING)
                .build();
    }
}
