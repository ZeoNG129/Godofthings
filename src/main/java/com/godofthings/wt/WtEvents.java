package com.godofthings.wt;

import java.util.HashMap;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import appeng.api.config.Actionable;
import appeng.api.config.IncludeExclude;
import appeng.api.stacks.AEItemKey;
import appeng.me.helpers.PlayerSource;
import appeng.menu.me.crafting.CraftAmountMenu;

import com.godofthings.wt.core.WtComponents;
import com.godofthings.wt.core.WtCraftingTerminalHandler;
import com.godofthings.wt.core.WtMagnetHandler;
import com.godofthings.wt.core.WtMagnetHost;
import com.godofthings.wt.core.WtTags;
import com.godofthings.wt.network.WtNetwork;

/**
 * 无线终端的事件钩子（照抄 ae2wtlib 的 {@code AE2wtlibEvents}：补货 / ME 直接入网 /
 * 中键选取方块）。
 */
public class WtEvents
{
    /**
     * Attempts to restock an item in a player's inventory from the ME system.
     */
    public static void restock(ServerPlayer player, ItemStack item, ItemStack now,
            java.util.function.Consumer<ItemStack> setStack)
    {
        int count = now.getCount();
        if (player.isCreative())
        {
            return;
        }
        if (item.isEmpty())
        {
            return;
        }
        if (item.is(WtTags.NO_RESTOCK))
        {
            return;
        }

        WtCraftingTerminalHandler cTHandler = WtCraftingTerminalHandler.get(player);
        if (!cTHandler.inRange())
        {
            return;
        }
        ItemStack hostItem = cTHandler.getCraftingTerminal();
        if (!(boolean) hostItem.getOrDefault(WtComponents.RESTOCK, false))
        {
            return;
        }
        if (cTHandler.getTargetGrid() == null)
        {
            return;
        }
        if (cTHandler.getTargetGrid().getStorageService() == null)
        {
            return;
        }

        int toAdd = item.getMaxStackSize() - count;
        if (toAdd == 0)
        {
            return;
        }
        if (!now.isEmpty() && !ItemStack.isSameItemSameComponents(item, now))
        {
            return;
        }

        long changed;
        if (toAdd > 0)
        {
            changed = cTHandler.getTargetGrid().getStorageService().getInventory().extract(
                    AEItemKey.of(item), toAdd, Actionable.MODULATE, new PlayerSource(player));
        }
        else
        {
            changed = -cTHandler.getTargetGrid().getStorageService().getInventory().insert(
                    AEItemKey.of(item), -toAdd, Actionable.MODULATE, new PlayerSource(player));
        }

        item.setCount(count + (int) changed);
        setStack.accept(item);

        int slot = player.getInventory().findSlotMatchingUnusedItem(item);
        if (slot == -1)
        {
            if (player.getInventory().offhand.contains(item))
            {
                slot = Inventory.INVENTORY_SIZE;
            }
        }
        PacketDistributor.sendToPlayer(player, new WtNetwork.UpdateRestock(slot, item));
    }

    /**
     * Attempts to insert an item stack from a player's inventory into an ME system linked to a terminal the player
     * carries.
     */
    public static void insertStackInME(ItemEntity entity, Player player)
    {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty())
        {
            return;
        }
        if (player.level().isClientSide())
        {
            return;
        }

        if (!canInsert(stack, player))
        {
            return;
        }

        WtCraftingTerminalHandler cTHandler = WtCraftingTerminalHandler.get(player);
        if (!cTHandler.inRange())
        {
            return;
        }

        if (cTHandler.getTargetGrid() == null)
        {
            return;
        }
        if (cTHandler.getTargetGrid().getStorageService() == null)
        {
            return;
        }

        long inserted = cTHandler.getTargetGrid().getStorageService().getInventory().insert(AEItemKey.of(stack),
                stack.getCount(), Actionable.MODULATE, new PlayerSource(player));
        int leftover = (int) (stack.getCount() - inserted);

        player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), (int) inserted);
        player.onItemPickup(entity);

        stack.setCount(leftover);
    }

    private static boolean canInsert(ItemStack stack, Player player)
    {
        WtCraftingTerminalHandler cTHandler = WtCraftingTerminalHandler.get(player);
        ItemStack terminal = cTHandler.getCraftingTerminal();

        cTHandler.checkTerminal();
        if (cTHandler.isRestockEnabled() && isRestocking(stack, player))
        {
            return true;
        }

        if (player.isShiftKeyDown())
        {
            return false;
        }

        if (!(WtMagnetHandler.getMagnetMode(terminal).pickupToME()))
        {
            return false;
        }

        WtMagnetHost magnetHost = cTHandler.getMagnetHost();
        if (magnetHost == null)
        {
            return false;
        }

        // if the filter is empty, it will match anything, even in whitelist mode
        if (magnetHost.getInsertFilter().isEmpty() && magnetHost.getInsertMode() == IncludeExclude.WHITELIST)
        {
            return false;
        }
        if (!magnetHost.getInsertFilter().matchesFilter(AEItemKey.of(stack), magnetHost.getInsertMode()))
        {
            return false;
        }

        return true;
    }

    private static boolean isRestocking(ItemStack stack, Player player)
    {
        if (stack.is(WtTags.NO_RESTOCK))
        {
            return false;
        }
        if (stack.getMaxStackSize() == 1)
        {
            return false;
        }
        for (int i = 0; i < 9; i++)
        {
            if (ItemStack.isSameItemSameComponents(stack, player.getInventory().getItem(i)))
            {
                return true;
            }
        }
        return false;
    }

    public static void pickBlock(ItemStack stack)
    {
        PacketDistributor.sendToServer(new WtNetwork.PickBlock(stack));
    }

    public static void pickBlock(ServerPlayer player, ItemStack stack)
    {
        var cTHandler = WtCraftingTerminalHandler.get(player);
        ItemStack terminal = cTHandler.getCraftingTerminal();
        if (!terminal.getOrDefault(WtComponents.PICK_BLOCK, false))
        {
            return;
        }

        if (cTHandler.getTargetGrid() == null)
        {
            return;
        }
        if (cTHandler.getTargetGrid().getStorageService() == null)
        {
            return;
        }
        var networkInventory = cTHandler.getTargetGrid().getStorageService().getInventory();
        var playerSource = new PlayerSource(player);

        var inventory = player.getInventory();
        int targetSlot = inventory.getSuitableHotbarSlot();
        var toReplace = inventory.getItem(targetSlot);

        var insert = networkInventory.insert(AEItemKey.of(toReplace), toReplace.getCount(), Actionable.SIMULATE,
                playerSource);
        if (insert < toReplace.getCount())
        {
            return;
        }
        int targetAmount = stack.getMaxStackSize();
        var what = AEItemKey.of(stack);
        var extracted = networkInventory.extract(what, targetAmount, Actionable.SIMULATE, playerSource);
        if (extracted == 0)
        {
            if (!terminal.getOrDefault(WtComponents.CRAFT_IF_MISSING, false)
                    || cTHandler.getTargetGrid().getCraftingService().getCraftingFor(what).isEmpty())
            {
                return;
            }
            CraftAmountMenu.open(player, cTHandler.getLocator(), what, 1);
            return;
        }

        insert = networkInventory.insert(AEItemKey.of(toReplace), toReplace.getCount(), Actionable.MODULATE,
                playerSource);
        if (insert < toReplace.getCount())
        {
            toReplace.setCount(toReplace.getCount() - (int) insert);
            inventory.setItem(targetSlot, toReplace);
            return;
        }

        extracted = networkInventory.extract(AEItemKey.of(stack), targetAmount, Actionable.MODULATE, playerSource);
        if (extracted == 0)
        {
            inventory.setItem(targetSlot, ItemStack.EMPTY);
            return;
        }
        stack.setCount((int) extracted);
        inventory.setItem(targetSlot, stack);
        inventory.selected = targetSlot;
        player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
    }
}
