package com.godofthings.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * 无限大小物品储存：永不拒绝插入（同类合并为单堆，数量无上限）。
 * <p>
 * 2026-09 修复：旧版为规避 count&gt;99 序列化崩溃把每堆限制在 99，
 * 导致同类物品堆数随总量无界增长（每 99 个一堆），达到 {@link #MAX_STACKS}
 * 堆数上限后剩余物被 {@link #dropRemainder} 掉落到机器上方——
 * 即「内部存储到一定数量后在机器上方生成掉落物」的根因。
 * 现改为「每个物品类型恒一个堆叠、count 无上限」：
 * 序列化用自定义格式（物品本体按 count=1 保存，真实数量单独存 int），
 * 彻底绕开 ItemStack.save 的 99 硬上限，机器储存再大也不会外吐掉落物。
 * {@link #MAX_STACKS} 语义随之变为「不同物品类型数上限」，防内存滥用。
 * <p>
 * 注意：{@link #getStackInSlot} / {@link #extractItem} 返回的堆叠 count
 * 可能 &gt;99，只允许在本 handler 内流转或经邻居 insertItem 按容量截断；
 * 禁止直接 ItemStack.save 或生成 ItemEntity（须先拆成 ≤99 的堆）。
 * <p>
 * 1.21.1 移植说明：物品 NBT 序列化需要 HolderLookup.Provider
 * （ItemStack.save(provider) / ItemStack.parseOptional(provider, tag)）。
 */
public class InfiniteItemHandler implements IItemHandlerModifiable
{
    /** 最大不同物品类型数上限，防止挂机时无限增长 */
    public static final int MAX_STACKS = 4096;

    private final List<ItemStack> stacks = new ArrayList<>();
    private Runnable onChange = () -> {};

    /** 储存类型数达上限时把剩余物掉落到方块位置附近，避免物品凭空消失。
     *  单个 ItemEntity 的 ItemStack count 序列化硬上限是 99，大数量自动拆堆。 */
    public static void dropRemainder(Level level, BlockPos pos, ItemStack stack)
    {
        if (level == null || level.isClientSide || stack.isEmpty())
        {
            return;
        }
        // 大数量拆成 ≤99 的堆逐个生成，防止超限 count 在存档时抛异常崩溃
        ItemStack rest = stack.copy();
        while (!rest.isEmpty())
        {
            int chunk = Math.min(rest.getCount(), 99);
            ItemEntity entity = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    rest.copyWithCount(chunk));
            entity.setDeltaMovement(0, 0.1, 0);
            level.addFreshEntity(entity);
            rest.shrink(chunk);
        }
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
        // 1) 同类合并为单堆：数量无上限（仅防 int 溢出钳到 MAX_VALUE）。
        //    这样同一种物品无论总量多大都只占一个堆叠，储存永不因数量增长而外吐。
        for (int i = 0; i < stacks.size() && !toInsert.isEmpty(); i++)
        {
            ItemStack target = stacks.get(i);
            // 1.21.1：isSameItemSameTags 已删，改用 isSameItemSameComponents（数据组件等价判定）
            if (ItemStack.isSameItemSameComponents(target, toInsert))
            {
                int room = Integer.MAX_VALUE - target.getCount();
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
        // 2) 无同类堆：新开一堆整堆放入（类型数达上限时剩余返回给调用方处理）
        if (!toInsert.isEmpty() && stacks.size() < MAX_STACKS)
        {
            if (!simulate)
            {
                stacks.add(toInsert.copy());
                onChange.run();
            }
            return ItemStack.EMPTY;
        }
        return toInsert;
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

    // ---- NBT（1.21.1：需要 HolderLookup.Provider） ----
    // 自定义格式：每条 {"Item": <count=1 的完整物品序列化>, "Count": <真实数量 int>}。
    // 不直接 ItemStack.save 大数量堆（count>99 会抛 IllegalStateException），
    // 数量单独存放；读取时兼容旧格式（直接物品 Compound、count≤99）。

    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack stack : stacks)
        {
            if (!stack.isEmpty())
            {
                CompoundTag entry = new CompoundTag();
                ItemStack unit = stack.copy();
                unit.setCount(1);
                entry.put("Item", unit.save(provider));
                entry.putInt("Count", stack.getCount());
                list.add(entry);
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
                CompoundTag entry = list.getCompound(i);
                ItemStack stack;
                if (entry.contains("Item", Tag.TAG_COMPOUND))
                {
                    // 新格式：物品本体 count=1 + 独立 Count 字段（数量可 >99）
                    stack = ItemStack.parseOptional(provider, entry.getCompound("Item"));
                    if (!stack.isEmpty())
                    {
                        stack.setCount(entry.getInt("Count"));
                    }
                }
                else
                {
                    // 旧格式兼容：直接的物品序列化（count ≤99）
                    stack = ItemStack.parseOptional(provider, entry);
                }
                if (!stack.isEmpty() && stack.getCount() > 0)
                {
                    stacks.add(stack);
                }
            }
        }
        // 旧存档可能同类多堆（旧 99 上限造成），归一化为每类一堆
        normalize();
    }

    /** 把同类多堆合并为单堆（数量累加，防溢出钳制）。 */
    private void normalize()
    {
        if (stacks.size() <= 1)
        {
            return;
        }
        List<ItemStack> merged = new ArrayList<>();
        outer:
        for (ItemStack stack : stacks)
        {
            if (stack.isEmpty())
            {
                continue;
            }
            for (ItemStack target : merged)
            {
                if (ItemStack.isSameItemSameComponents(target, stack))
                {
                    target.grow(Math.min(stack.getCount(), Integer.MAX_VALUE - target.getCount()));
                    continue outer;
                }
            }
            merged.add(stack);
        }
        stacks.clear();
        stacks.addAll(merged);
    }
}
