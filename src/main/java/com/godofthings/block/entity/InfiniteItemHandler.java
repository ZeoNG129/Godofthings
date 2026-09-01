package com.godofthings.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * 无限大小物品储存：永不拒绝插入（自动合并同类或新增堆叠）。
 * <p>
 * 优化（2026-08）：插入时<b>全局合并</b>同类堆叠（不再只合并目标槽，避免同类物品
 * 每次插入都新建一条导致无限增长）；并设 {@link #MAX_STACKS} 堆叠数上限，
 * 达到上限后把剩余物返回给调用方（由调用方掉落处理），保证挂机不爆内存。
 *
 * 1.21.1 移植说明：物品 NBT 序列化需要 HolderLookup.Provider
 * （ItemStack.save(provider) / ItemStack.parseOptional(provider, tag)）。
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
        // 1) 全局合并同类堆叠。单堆上限 99：ItemStack 的 count 序列化硬上限是 99
        //    （ItemStack.MAP_CODEC 用 ExtraCodecs.intRange(1, 99)，超 99 会在存档/掉落序列化时抛异常崩溃），
        //    因此每堆最多 99，超出部分拆成新堆，绝不允许单堆 > 99。
        for (int i = 0; i < stacks.size() && !toInsert.isEmpty(); i++)
        {
            ItemStack target = stacks.get(i);
            // 1.21.1：isSameItemSameTags 已删，改用 isSameItemSameComponents（数据组件等价判定）
            if (ItemStack.isSameItemSameComponents(target, toInsert))
            {
                int room = 99 - target.getCount();
                if (room > 0)
                {
                    int actual = Math.min(toInsert.getCount(), room);
                    if (!simulate)
                    {
                        target.grow(actual);
                        onChange.run();
                    }
                    toInsert.shrink(actual);
                }
            }
        }
        // 2) 仍有剩余：拆成 ≤99 的新堆叠（受堆叠数上限保护，超出部分返回给调用方掉落处理）
        while (!toInsert.isEmpty() && stacks.size() < MAX_STACKS)
        {
            int chunk = Math.min(toInsert.getCount(), 99);
            ItemStack piece = toInsert.copy();
            piece.setCount(chunk);
            if (!simulate)
            {
                stacks.add(piece);
                onChange.run();
            }
            toInsert.shrink(chunk);
        }
        return toInsert.isEmpty() ? ItemStack.EMPTY : toInsert;
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

    // ---- NBT（1.21.1：需要 HolderLookup.Provider，签名与 NeoForge ItemStackHandler 对齐） ----

    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack stack : stacks)
        {
            if (!stack.isEmpty())
            {
                list.add(stack.save(provider));
            }
        }
        tag.put("Items", list);
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag)
    {
        stacks.clear();
        if (tag.contains("Items", Tag.TAG_LIST))
        {
            ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++)
            {
                // 1.21.1：ItemStack.of(CompoundTag) 已删，改用 parseOptional（失败返回 EMPTY，同旧行为）
                ItemStack stack = ItemStack.parseOptional(provider, list.getCompound(i));
                if (!stack.isEmpty())
                {
                    stacks.add(stack);
                }
            }
        }
    }
}
