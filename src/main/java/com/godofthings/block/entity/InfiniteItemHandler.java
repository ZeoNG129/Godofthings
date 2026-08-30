package com.godofthings.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * 无限大小物品储存：永不拒绝插入（自动合并同类或新增堆叠）。
 * <p>
 * 优化（2026-08）：插入时<b>全局合并</b>同类堆叠（不再只合并目标槽，避免同类物品
 * 每次插入都新建一条导致无限增长）；并设 {@link #MAX_STACKS} 堆叠数上限，
 * 达到上限后把剩余物返回给调用方（由调用方掉落处理），保证挂机不爆内存。
 */
public class InfiniteItemHandler implements IItemHandlerModifiable
{
    /** 最大堆叠数上限，防止挂机时无限增长 */
    public static final int MAX_STACKS = 4096;

    private final List<ItemStack> stacks = new ArrayList<>();
    private Runnable onChange = () -> {};

    /** 储存已满时把剩余物掉落到方块位置附近，避免物品凭空消失。 */
    public static void dropRemainder(Level level, BlockPos pos, ItemStack stack)
    {
        if (level == null || level.isClientSide || stack.isEmpty())
        {
            return;
        }
        ItemEntity entity = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                stack.copy());
        entity.setDeltaMovement(0, 0.1, 0);
        level.addFreshEntity(entity);
    }

    public void setOnChange(Runnable onChange)
    {
        this.onChange = onChange;
    }

    public List<ItemStack> getStacks()
    {
        return stacks;
    }

    @Override
    public int getSlots()
    {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        return slot >= 0 && slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack)
    {
        if (slot >= 0 && slot < stacks.size())
        {
            if (stack.isEmpty())
            {
                stacks.remove(slot);
            }
            else
            {
                stacks.set(slot, stack.copy());
            }
            onChange.run();
        }
        else if (!stack.isEmpty())
        {
            stacks.add(stack.copy());
            onChange.run();
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        ItemStack toInsert = stack.copy();
        // 1) 全局合并同类堆叠（优先合并已有堆叠，避免同类物品每插入一次就新建一条）
        for (int i = 0; i < stacks.size() && !toInsert.isEmpty(); i++)
        {
            ItemStack target = stacks.get(i);
            if (ItemStack.isSameItemSameTags(target, toInsert))
            {
                int add = Math.min(target.getMaxStackSize() - target.getCount(), toInsert.getCount());
                if (add > 0)
                {
                    if (!simulate)
                    {
                        target.grow(add);
                        onChange.run();
                    }
                    toInsert.shrink(add);
                }
            }
        }
        // 2) 仍有剩余：新建堆叠（受堆叠数上限保护，超出部分返回给调用方掉落处理）
        if (!toInsert.isEmpty())
        {
            if (stacks.size() < MAX_STACKS)
            {
                if (!simulate)
                {
                    stacks.add(toInsert);
                    onChange.run();
                }
                return ItemStack.EMPTY;
            }
            return toInsert;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (slot < 0 || slot >= stacks.size())
        {
            return ItemStack.EMPTY;
        }
        ItemStack current = stacks.get(slot);
        int toExtract = Math.min(amount, current.getCount());
        ItemStack result = current.copy();
        result.setCount(toExtract);
        if (!simulate)
        {
            current.shrink(toExtract);
            if (current.isEmpty())
            {
                stacks.remove(slot);
            }
            onChange.run();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return true;
    }

    // ---- NBT ----

    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack stack : stacks)
        {
            if (!stack.isEmpty())
            {
                list.add(stack.save(new CompoundTag()));
            }
        }
        tag.put("Items", list);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag)
    {
        stacks.clear();
        if (tag.contains("Items", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++)
            {
                ItemStack stack = ItemStack.of(list.getCompound(i));
                if (!stack.isEmpty())
                {
                    stacks.add(stack);
                }
            }
        }
    }
}
