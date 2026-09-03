package com.godofthings.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 把 NeoForge {@link IItemHandler} 包装成 AE2 的 {@link MEStorage}，
 * 让神之系列机器的内部存储可被 AE 存储总线直接接入（占一个频道）。
 * <p>
 * 只处理物品键（AEItemKey），流体键返回 0。
 */
public class ItemHandlerMEStorage implements MEStorage
{
    private final IItemHandler handler;
    private final Component description;

    public ItemHandlerMEStorage(IItemHandler handler, Component description)
    {
        this.handler = handler;
        this.description = description;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        if (!(what instanceof AEItemKey itemKey) || amount <= 0)
        {
            return 0;
        }
        boolean simulate = mode.isSimulate();
        long remaining = amount;
        long inserted = 0;
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++)
        {
            ItemStack toInsert = itemKey.toStack((int) Math.min(remaining, Integer.MAX_VALUE));
            ItemStack leftover = handler.insertItem(slot, toInsert, simulate);
            int accepted = toInsert.getCount() - leftover.getCount();
            if (accepted <= 0)
            {
                continue;
            }
            inserted += accepted;
            remaining -= accepted;
        }
        return inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        if (!(what instanceof AEItemKey itemKey) || amount <= 0)
        {
            return 0;
        }
        boolean simulate = mode.isSimulate();
        long remaining = amount;
        long extracted = 0;
        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++)
        {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || !stack.is(itemKey.getItem()))
            {
                continue;
            }
            int chunk = (int) Math.min(remaining, (long) stack.getCount());
            ItemStack taken = handler.extractItem(slot, chunk, simulate);
            if (taken.isEmpty())
            {
                continue;
            }
            extracted += taken.getCount();
            remaining -= taken.getCount();
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out)
    {
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty())
            {
                out.add(AEItemKey.of(stack), stack.getCount());
            }
        }
    }

    @Override
    public Component getDescription()
    {
        return description;
    }
}
