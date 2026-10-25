package com.godofthings.wt.core;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.util.IConfigManager;
import appeng.menu.locator.ItemMenuHostLocator;

/**
 * 无耗能无线通用终端（照抄 ae2wtlib 的 {@code ItemWUT}，[本地改动] 注册表/组件键全走
 * 本模组的 {@link WtTerminalDefinition}，无耗能由 {@link WtPoweredItem} 提供）。
 */
public class WtUniversalTerminalItem extends WtTerminalItem
{
    private static final Style STYLE_GRAY = Style.EMPTY.withColor(ChatFormatting.GRAY);

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (WtTerminalDefinition.ofOrNull(player.getItemInHand(hand)) == null)
        {
            if (!level.isClientSide())
            {
                player.sendSystemMessage(Component.translatable("msg.godofthings.wut.empty"));
            }
            return new InteractionResultHolder<>(InteractionResult.sidedSuccess(level.isClientSide()),
                    player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public boolean open(Player player, ItemMenuHostLocator locator, boolean returningFromSubmenu)
    {
        return WtTerminalDefinition.open(player, locator, returningFromSubmenu);
    }

    @Override
    public MenuType<?> getMenuType(ItemMenuHostLocator locator, Player player)
    {
        return WtTerminalDefinition.of(locator.locateItem(player)).menuType();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
            TooltipFlag advancedTooltips)
    {
        var current = WtTerminalDefinition.ofOrNull(stack);
        if (current != null)
        {
            lines.add(Component.translatable("gui.godofthings.wut.current_terminal",
                    Component.translatable(current.translationKey())));
            lines.add(Component.empty());
        }
        lines.add(Component.translatable("item.godofthings.wireless_universal_terminal_free.desc"));
        for (var terminal : WtTerminalDefinition.wirelessTerminalList())
        {
            if (stack.get(terminal.componentType().type()) != null)
            {
                lines.add(terminal.formattedName());
            }
        }
        // [本地改动] v7.15.5：列出已合并的附属终端
        for (var extra : WtExtraTerminals.list(stack))
        {
            lines.add(WtExtraTerminals.displayName(extra).copy().withStyle(STYLE_GRAY));
        }
        lines.add(Component.translatable("tooltip.godofthings.wut.no_energy"));
        super.appendHoverText(stack, context, lines, advancedTooltips);
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack)
    {
        return UpgradeInventories.forItem(stack, countInstalledTerminals(stack) * 2, this::onUpgradesChanged);
    }

    public void onUpgradesChanged(ItemStack stack, IUpgradeInventory upgrades)
    {
        // [本地改动] 无耗能：上游在这里调整能量倍率，本模组不需要（电量恒满）。
    }

    /**
     * 通用终端已装终端数 ×2 = 升级槽容量（照抄 ItemWUT.countInstalledTerminals，
     * [本地改动] v7.15.6：附属终端也计入——合并进来的终端同样需要磁卡/能量卡槽位，
     * 否则合并后升级槽容量不够，磁卡「消失」）。
     */
    public int countInstalledTerminals(ItemStack stack)
    {
        int terminals = 0;
        for (var terminal : WtTerminalDefinition.wirelessTerminalList())
        {
            if (stack.get(terminal.componentType().type()) != null)
            {
                terminals++;
            }
        }
        return terminals + WtExtraTerminals.count(stack);
    }


    @Override
    public IConfigManager getConfigManager(Supplier<ItemStack> target)
    {
        // [本地改动] v7.15.3 单物品架构：三台终端定义的 item 都指向本物品，
        // 上游「按当前终端定义转发到对应物品的 getConfigManager」会变成自我递归
        // （7.15.3 首版 StackOverflow 崩溃），改为直接自建并注册全部终端需要的设置。
        return WtConfigManager.builder(target)
                .registerSetting(appeng.api.config.Settings.SORT_BY, appeng.api.config.SortOrder.NAME)
                .registerSetting(appeng.api.config.Settings.VIEW_MODE, appeng.api.config.ViewItems.ALL)
                .registerSetting(appeng.api.config.Settings.SORT_DIRECTION, appeng.api.config.SortDir.ASCENDING)
                .registerSetting(appeng.api.config.Settings.TERMINAL_SHOW_PATTERN_PROVIDERS,
                        appeng.api.config.ShowPatternProviders.VISIBLE)
                .build();
    }
}
