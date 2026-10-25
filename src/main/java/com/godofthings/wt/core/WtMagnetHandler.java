package com.godofthings.wt.core;

import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import com.google.common.collect.Maps;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import appeng.api.config.Actionable;
import appeng.api.config.IncludeExclude;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeableItem;
import appeng.me.helpers.PlayerSource;

import com.godofthings.Godofthings;
import com.godofthings.wt.core.WtComponents;
import com.godofthings.wt.core.WtMagnetMode;
import com.godofthings.wt.core.WtMagnetHost;
import com.godofthings.wt.core.WtCraftingTerminalHandler;
import com.godofthings.wt.network.WtNetwork;

/**
 * 磁力系统（照抄 ae2wtlib 的 {@code MagnetHandler}，[本地改动] 只认本模组的磁卡）。
 */
public final class WtMagnetHandler
{
    private static final WeakHashMap<ServerPlayer, Integer> players = new WeakHashMap<>();

    public static void handle(ServerPlayer player, ItemStack terminal)
    {
        if (players.containsKey(player) && players.get(player) == getTick(player))
        {
            return;
        }
        sendRestockAble(player, terminal);
        handleMagnet(player, terminal);
        players.put(player, getTick(player));
    }

    private static int getTick(ServerPlayer player)
    {
        if (player.getServer() == null)
        {
            return -1;
        }
        return player.getServer().getTickCount();
    }

    private static void sendRestockAble(ServerPlayer player, ItemStack terminal)
    {
        if (player.isCreative())
        {
            return;
        }
        if (!terminal.getOrDefault(WtComponents.RESTOCK, false))
        {
            return;
        }
        sendRestockAble(player);
    }

    private static void sendRestockAble(ServerPlayer player)
    {
        WtCraftingTerminalHandler handler = WtCraftingTerminalHandler.get(player);
        ItemStack hostItem = handler.getCraftingTerminal();
        if (!hostItem.getOrDefault(WtComponents.RESTOCK, false) || !handler.inRange())
        {
            return;
        }
        HashMap<Item, Long> items = new HashMap<>();

        if (handler.getTargetGrid() == null)
        {
            return;
        }
        KeyCounter storageList = handler.getTargetGrid().getStorageService().getCachedInventory();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty())
            {
                continue;
            }
            if (items.containsKey(stack.getItem()))
            {
                continue;
            }
            if (stack.is(WtTags.NO_RESTOCK))
            {
                continue;
            }
            AEItemKey key = AEItemKey.of(stack);
            if (key == null)
            {
                items.put(stack.getItem(), 0L);
            }
            else
            {
                items.put(stack.getItem(), storageList.get(key));
            }
        }

        HashMap<Holder<Item>, Long> map = Maps.newHashMapWithExpectedSize(items.size());
        // noinspection deprecation
        items.forEach((item, count) -> map.put(item.builtInRegistryHolder(), count));
        PacketDistributor.sendToPlayer(player, new WtNetwork.RestockAmount(map));
    }

    private static void handleMagnet(Player player, ItemStack terminal)
    {
        if (player.isShiftKeyDown())
        {
            return;
        }
        if (!getMagnetMode(terminal).magnet())
        {
            return;
        }
        handleMagnet(player);
    }

    private static void handleMagnet(Player player)
    {
        WtCraftingTerminalHandler ctHandler = WtCraftingTerminalHandler.get(player);
        if (!getMagnetMode(ctHandler.getCraftingTerminal()).magnet())
        {
            return;
        }
        WtMagnetHost magnetHost = ctHandler.getMagnetHost();
        if (magnetHost == null)
        {
            return;
        }

        // if the filter is empty, it will match anything, even in whitelist mode
        if (magnetHost.getPickupFilter().isEmpty() && magnetHost.getPickupMode() == IncludeExclude.WHITELIST)
        {
            return;
        }

        List<ItemEntity> entityItems = player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(com.godofthings.wt.WtConfig.magnetCardRange()),
                EntitySelector.ENTITY_STILL_ALIVE);

        for (ItemEntity entityItemNearby : entityItems)
        {
            var item = AEItemKey.of(entityItemNearby.getItem());
            if (item == null)
            {
                continue;
            }
            if (magnetHost.getPickupFilter().matchesFilter(item, magnetHost.getPickupMode())
                    && !entityItemNearby.getPersistentData().contains("PreventRemoteMovement"))
            {
                entityItemNearby.playerTouch(player);
            }
        }
    }

    public static void saveMagnetMode(ItemStack terminal, WtMagnetMode magnetSettings)
    {
        if (magnetSettings == WtMagnetMode.INVALID || magnetSettings == WtMagnetMode.NO_CARD)
        {
            return;
        }
        if (!(terminal.getItem() instanceof IUpgradeableItem upgradeableItem))
        {
            return;
        }
        if (!upgradeableItem.getUpgrades(terminal).isInstalled(Godofthings.MAGNET_CARD.get()))
        {
            return;
        }
        terminal.set(com.godofthings.Godofthings.WT_MAGNET_SETTINGS, magnetSettings);
    }

    public static WtMagnetMode getMagnetMode(ItemStack terminal)
    {
        if (!(terminal.getItem() instanceof IUpgradeableItem upgradeableItem))
        {
            return WtMagnetMode.INVALID;
        }
        if (!upgradeableItem.getUpgrades(terminal).isInstalled(Godofthings.MAGNET_CARD.get()))
        {
            return WtMagnetMode.NO_CARD;
        }
        return terminal.getOrDefault(com.godofthings.Godofthings.WT_MAGNET_SETTINGS, WtMagnetMode.OFF);
    }
}
