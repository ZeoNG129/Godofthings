package com.godofthings.godgrid;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Arrays;

/**
 * 自管 long 计数的单格大容量物品槽（对标 Mekanism 的「大容量槽 / 量子储物箱」观感），
 * <b>不依赖 Mekanism 或任何外部模组</b>。
 *
 * <p>为什么不直接用原版 {@code ItemStackHandler}：{@code ItemStack.CODEC} 的 count 是
 * {@code ExtraCodecs.intRange(1, 99)}，单格堆叠数一旦超过 99，存档 / 掉落 / 发包都会抛异常。
 * 所以这里把「物品身份」与「数量」拆开存：</p>
 * <ul>
 *   <li>{@link #keys}：每格的物品身份，{@code count} 恒为 1（保存时也只写 count=1）；</li>
 *   <li>{@link #amounts}：每格的真实数量，{@code long}，上限 {@link #capacity()}（默认 1,000,000）。</li>
 * </ul>
 *
 * <p><b>对外视图永远 ≤99</b>：{@link #getStackInSlot} 返回
 * {@code key.copyWithCount(min(amount, 99))}，保证别的模组管道 / AE / 原版容器同步
 * 永远拿不到非法堆。{@link #extractItem} / {@link #insertItem} 在 {@code int} 数量范围内工作，
 * 内部批量搬运走 {@link #insertLong} / {@link #extractLong} + {@link #amount}，不产生超限堆。
 * 超限堆（count&gt;99 的 ItemStack）只可能作为「内部临时对象」出现，绝不进存档、不发包。</p>
 *
 * <p>{@link LongSlot} 是 GUI 里的槽位包装：因为视图被钳到 99，vanilla 的「整格替换」语义
 * （{@code Slot#set}）会把超出视图的存量抹掉，所以这里按「视图差额」结算：
 * 取走视图就扣视图、放回视图就加回去。</p>
 */
public class LongSlotHandler implements IItemHandlerModifiable
{
    /** 对外视图的单格上限（= ItemStack 的 count 序列化硬上限） */
    public static final int VIEW_CAP = 99;

    /** 默认单格容量 */
    public static final long DEFAULT_CAPACITY = 1_000_000L;

    private final ItemStack[] keys;
    private final long[] amounts;
    private final long capacity;

    public LongSlotHandler(int slots)
    {
        this(slots, DEFAULT_CAPACITY);
    }

    public LongSlotHandler(int slots, long capacity)
    {
        this.keys = new ItemStack[Math.max(0, slots)];
        this.amounts = new long[Math.max(0, slots)];
        Arrays.fill(this.keys, ItemStack.EMPTY);
        this.capacity = Math.max(VIEW_CAP, capacity);
    }

    /** 单格容量（long） */
    public long capacity()
    {
        return capacity;
    }

    private boolean valid(int slot)
    {
        return slot >= 0 && slot < keys.length;
    }

    /** 该格的真实存量（GUI 显示与内部批量逻辑用；可能是 long 大数） */
    public long amount(int slot)
    {
        return valid(slot) ? amounts[slot] : 0L;
    }

    /** 该格的物品身份（count 恒 1 的副本；空格返回 {@link ItemStack#EMPTY}） */
    public ItemStack key(int slot)
    {
        return valid(slot) ? keys[slot].copy() : ItemStack.EMPTY;
    }

    public boolean isEmpty(int slot)
    {
        return !valid(slot) || keys[slot].isEmpty() || amounts[slot] <= 0L;
    }

    /** 该格还能再放多少（空格 = 整个容量） */
    public long room(int slot)
    {
        if (!valid(slot))
        {
            return 0L;
        }
        return isEmpty(slot) ? capacity : Math.max(0L, capacity - amounts[slot]);
    }

    // ---- IItemHandler / IItemHandlerModifiable ----

    @Override
    public int getSlots()
    {
        return keys.length;
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack)
    {
        return valid(slot) && !stack.isEmpty();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate)
    {
        if (stack.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        long inserted = insertLong(slot, stack, stack.getCount(), simulate);
        if (inserted <= 0L)
        {
            return stack;
        }
        if (inserted >= stack.getCount())
        {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount((int) (stack.getCount() - inserted));
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate)
    {
        if (amount <= 0 || isEmpty(slot))
        {
            return ItemStack.EMPTY;
        }
        // 先留一份身份，extractLong 抽空后 keys[slot] 会被清掉
        ItemStack view = keys[slot].copy();
        long got = extractLong(slot, amount, simulate);
        if (got <= 0L)
        {
            return ItemStack.EMPTY;
        }
        return view.copyWithCount((int) Math.min(got, Integer.MAX_VALUE));
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack)
    {
        if (!valid(slot))
        {
            return;
        }
        if (stack.isEmpty())
        {
            clear(slot);
            return;
        }
        keys[slot] = stack.copyWithCount(1);
        amounts[slot] = Math.min(capacity, Math.max(0, stack.getCount()));
        onContentsChanged(slot);
    }

    /**
     * 对外视图数量：≤99（ItemStack 序列化上限），且不超过物品自身的最大堆叠。
     *
     * <p>后半条不是装饰：vanilla 的拖拽 / 合堆运算用
     * {@code min(视图数量 + 增量, 本格上限)} 反推结果，视图一旦大于「本格上限」就会算出负数差额。
     * 让视图恒 ≤ 物品最大堆叠，这些运算就与普通槽位完全同构。</p>
     */
    public int viewCount(int slot)
    {
        if (isEmpty(slot))
        {
            return 0;
        }
        return (int) Math.min(Math.min(amounts[slot], VIEW_CAP), keys[slot].getMaxStackSize());
    }

    @Override
    public ItemStack getStackInSlot(int slot)
    {
        int view = viewCount(slot);
        if (view <= 0)
        {
            return ItemStack.EMPTY;
        }
        // 对外视图永远 ≤99
        return keys[slot].copyWithCount(view);
    }

    // ---- long 级操作（内部批量搬运用，不构造超限堆） ----

    /** 插入至多 {@code amount} 个，返回实际插入量；同类累加，异类拒绝 */
    public long insertLong(int slot, ItemStack stack, long amount, boolean simulate)
    {
        if (!valid(slot) || stack.isEmpty() || amount <= 0L || !isItemValid(slot, stack))
        {
            return 0L;
        }
        if (!isEmpty(slot) && !ItemStack.isSameItemSameComponents(keys[slot], stack))
        {
            return 0L;
        }
        long inserted = Math.min(room(slot), amount);
        if (inserted <= 0L)
        {
            return 0L;
        }
        if (!simulate)
        {
            if (isEmpty(slot))
            {
                keys[slot] = stack.copyWithCount(1);
                amounts[slot] = inserted;
            }
            else
            {
                amounts[slot] = Math.min(capacity, amounts[slot] + inserted);
            }
            onContentsChanged(slot);
        }
        return inserted;
    }

    /** 抽取至多 {@code amount} 个，返回实际抽取量 */
    public long extractLong(int slot, long amount, boolean simulate)
    {
        if (!valid(slot) || amount <= 0L || isEmpty(slot))
        {
            return 0L;
        }
        long got = Math.min(amount, amounts[slot]);
        if (got <= 0L)
        {
            return 0L;
        }
        if (!simulate)
        {
            amounts[slot] -= got;
            if (amounts[slot] <= 0L)
            {
                clear(slot);
            }
            else
            {
                onContentsChanged(slot);
            }
        }
        return got;
    }

    /** 在现有量上加减一个增量（按容量 / 0 夹取），返回最终数量 */
    public long addAmount(int slot, long delta)
    {
        if (!valid(slot) || delta == 0L || isEmpty(slot))
        {
            return amount(slot);
        }
        long next = Math.max(0L, Math.min(capacity, amounts[slot] + delta));
        amounts[slot] = next;
        if (next <= 0L)
        {
            clear(slot);
        }
        else
        {
            onContentsChanged(slot);
        }
        return next;
    }

    public void clear(int slot)
    {
        if (!valid(slot))
        {
            return;
        }
        // 注意：必须以「是否还有物品身份」判断，不能看 isEmpty()（数量已归 0 时它已经返回 true，
        // 会让最后一次抽空不触发 onContentsChanged，方块实体就不会 markDirty）
        boolean hadItem = !keys[slot].isEmpty();
        keys[slot] = ItemStack.EMPTY;
        amounts[slot] = 0L;
        if (hadItem)
        {
            onContentsChanged(slot);
        }
    }

    /** 直接写入身份 + 数量（排序回写用；不做同类合并） */
    public void setEntry(int slot, ItemStack key, long amount)
    {
        if (!valid(slot))
        {
            return;
        }
        if (key.isEmpty() || amount <= 0L)
        {
            clear(slot);
            return;
        }
        keys[slot] = key.copyWithCount(1);
        amounts[slot] = Math.min(capacity, amount);
        onContentsChanged(slot);
    }

    /** 内容变化回调（方块实体里用来 setChanged()） */
    protected void onContentsChanged(int slot)
    {
    }

    // ---- NBT ----
    // 每个非空槽一条 {Slot:int, Item:{id,components}, Amount:long}，
    // 物品本体按 count=1 存（完全绕开 99 的序列化上限），真实数量单独存 long。

    /** 存成 {@code {Slots:[...]}} 复合标签 */
    public CompoundTag save(HolderLookup.Provider registries)
    {
        CompoundTag tag = new CompoundTag();
        tag.put("Slots", saveList(registries));
        return tag;
    }

    /** 从 {@code {Slots:[...]}} 复合标签读取 */
    public void load(CompoundTag tag, HolderLookup.Provider registries)
    {
        loadList(tag.getList("Slots", Tag.TAG_COMPOUND), registries);
    }

    /** 槽位列表本体（方块实体把这一层直接挂到自己的键上） */
    public ListTag saveList(HolderLookup.Provider registries)
    {
        ListTag list = new ListTag();
        for (int i = 0; i < keys.length; i++)
        {
            if (isEmpty(i))
            {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            entry.put("Item", keys[i].copyWithCount(1).save(registries));
            entry.putLong("Amount", amounts[i]);
            list.add(entry);
        }
        return list;
    }

    /** 读取槽位列表（先清空全部槽位） */
    public void loadList(ListTag list, HolderLookup.Provider registries)
    {
        Arrays.fill(keys, ItemStack.EMPTY);
        Arrays.fill(amounts, 0L);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (!valid(slot))
            {
                continue;
            }
            ItemStack stack = entry.contains("Item", Tag.TAG_COMPOUND)
                    ? ItemStack.parseOptional(registries, entry.getCompound("Item"))
                    : ItemStack.EMPTY;
            long amount = entry.getLong("Amount");
            if (stack.isEmpty() || amount <= 0L)
            {
                continue;
            }
            setEntry(slot, stack, amount);
        }
    }

    /**
     * GUI 槽位包装。
     *
     * <p>对外视图 ≤99，所以 vanilla 那套「整格替换」的点击逻辑会抹掉超出视图的存量。
     * 这里把三处会写回槽位的地方改成差额结算：</p>
     * <ul>
     *   <li>{@link #safeInsert}：点击放入 = 累加（返回手持剩余，语义与 vanilla 一致）；</li>
     *   <li>{@link #set}：存量 &gt; 99 时按「视图差额」结算，清空 = 只扣掉被拿走的视图那 99 个；</li>
     *   <li>{@link #mayPlace}：存量 &gt; 99 的格子不接受换物品（必须先取空）。</li>
     * </ul>
     */
    public static class LongSlot extends SlotItemHandler
    {
        private final LongSlotHandler longHandler;
        private final int slotIndex;

        public LongSlot(LongSlotHandler handler, int index, int x, int y)
        {
            super(handler, index, x, y);
            this.longHandler = handler;
            this.slotIndex = index;
        }

        public LongSlotHandler longHandler()
        {
            return longHandler;
        }

        public int slotIndex()
        {
            return slotIndex;
        }

        // 界面上的「上限」只能是视图上限，否则 vanilla 的 64 / 99 运算会算出负数差额
        @Override
        public int getMaxStackSize()
        {
            return VIEW_CAP;
        }

        @Override
        public int getMaxStackSize(ItemStack stack)
        {
            return Math.min(VIEW_CAP, stack.getMaxStackSize());
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            if (stack.isEmpty() || !longHandler.isItemValid(slotIndex, stack))
            {
                return false;
            }
            // 视图已完整反映存量（没被钳制）→ 正常替换，vanilla 语义成立
            if (longHandler.amount(slotIndex) <= longHandler.viewCount(slotIndex))
            {
                return true;
            }
            // 存量大于视图（被钳制过）：只允许同物品累加，避免「整格替换」丢量
            return ItemStack.isSameItemSameComponents(longHandler.key(slotIndex), stack);
        }

        @Override
        public ItemStack safeInsert(ItemStack stack, int amount)
        {
            if (stack.isEmpty() || amount <= 0 || !mayPlace(stack))
            {
                return stack;
            }
            long inserted = longHandler.insertLong(slotIndex, stack, Math.min(amount, stack.getCount()), false);
            if (inserted <= 0L)
            {
                return stack;
            }
            ItemStack rest = stack.copy();
            rest.shrink((int) inserted);
            return rest;
        }

        @Override
        public void set(ItemStack stack)
        {
            long current = longHandler.amount(slotIndex);
            int view = longHandler.viewCount(slotIndex);
            if (!longHandler.isEmpty(slotIndex) && current > view)
            {
                if (stack.isEmpty())
                {
                    // 调用方只拿走了「视图」那一份（数字键交换），扣视图而不是清空整格
                    longHandler.extractLong(slotIndex, view, false);
                    return;
                }
                if (ItemStack.isSameItemSameComponents(longHandler.key(slotIndex), stack))
                {
                    // 同物品：按「相对视图的差额」结算（放回 +delta / 换走 -delta），绝不抹掉超出视图的存量
                    longHandler.addAmount(slotIndex, (long) stack.getCount() - view);
                    return;
                }
            }
            longHandler.setStackInSlot(slotIndex, stack);
        }
    }
}
