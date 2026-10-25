package com.godofthings.wt.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import appeng.api.upgrades.IUpgradeableItem;

import com.godofthings.Godofthings;

/**
 * 磁力扫描的入口：每服务端 tick 从玩家身上找「装了本模组磁卡的终端」。
 *
 * <p>[本地改动] 上游由 ItemWCT.inventoryTick / ItemWUT.inventoryTick → MagnetHandler.handle
 * 驱动；本模组三台终端都聚合在通用终端里（无独立物品），所以挂在玩家 tick 上统一驱动，
 * 与上游效果一致（WeakHashMap 按 tick 去重在 MagnetHandler 内部）。</p>
 */
public final class WtMagnetTickHelper
{
    private WtMagnetTickHelper()
    {
    }

    public static ItemStack findMagnetTerminal(ServerPlayer player)
    {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof IUpgradeableItem))
            {
                continue;
            }
            var upgrades = ((IUpgradeableItem) stack.getItem()).getUpgrades(stack);
            if (upgrades.isInstalled(Godofthings.MAGNET_CARD.get()))
            {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
