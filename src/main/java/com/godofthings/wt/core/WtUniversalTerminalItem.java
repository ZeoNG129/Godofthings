package com.godofthings.wt.core;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
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

    /** 通用终端已装终端数 ×2 = 升级槽容量（照抄 ItemWUT.countInstalledTerminals）。 */
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
        return terminals;
    }

    public IConfigManager getConfigManager(Supplier<ItemStack> target)
    {
        var item = WtTerminalDefinition.of(target.get()).item();
        if (item instanceof WtTerminalItem terminalItem)
        {
            return terminalItem.getConfigManager(target);
        }
        return super.getConfigManager(target);
    }
}
