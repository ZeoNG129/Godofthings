package com.godofthings.backpack;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * 神之背包的 120 格容器：读写直接落到背包物品的数据组件上。
 *
 * <p>物品堆叠本身是引用语义（{@link #getItem(int)} 返回的是组件列表里的那个 ItemStack 对象），
 * 所以「就地改数量」（例如 {@code stack.grow(n)}）也会生效；{@link #setChanged()} 负责把当前内容
 * 整体写回物品组件（菜单里每个改动都会调它，物品掉地上 / 放进箱子都不丢）。</p>
 *
 * <p>背包物品用 {@link Supplier} 惰性取：界面开着的时候玩家可能把背包挪到别的格子，
 * 每次访问都重新解析一遍就不会写到旧对象上。</p>
 */
public class GodBackpackContainer implements Container
{
    /** 背包物品的来源（每次访问都重新取，避免持有已经不在物品栏里的旧堆叠） */
    private final Supplier<ItemStack> backpack;

    /** 内容缓存：组件里没有就按空背包算 */
    private GodBackpackContents cached;
    /** 缓存是从哪个堆叠读出来的（换背包了就重新读一次） */
    private ItemStack cachedFrom;

    public GodBackpackContainer(ItemStack backpack)
    {
        this(() -> backpack);
    }

    public GodBackpackContainer(Supplier<ItemStack> backpack)
    {
        this.backpack = backpack;
    }

    private ItemStack stack()
    {
        ItemStack stack = backpack.get();
        return stack == null ? ItemStack.EMPTY : stack;
    }

    /** 当前内容（惰性从物品组件读；背包被换掉时重新读） */
    private GodBackpackContents contents()
    {
        ItemStack stack = stack();
        if (cached == null || (!stack.isEmpty() && stack != cachedFrom))
        {
            cached = stack.isEmpty() ? GodBackpackContents.empty() : GodBackpackItem.contents(stack);
            cachedFrom = stack;
        }
        return cached;
    }

    private static boolean inRange(int slot)
    {
        return slot >= 0 && slot < GodBackpackItem.SIZE;
    }

    @Override
    public int getContainerSize()
    {
        return GodBackpackItem.SIZE;
    }

    @Override
    public boolean isEmpty()
    {
        return contents().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return inRange(slot) ? contents().get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount)
    {
        if (!inRange(slot) || amount <= 0)
        {
            return ItemStack.EMPTY;
        }
        ItemStack current = contents().get(slot);
        if (current.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        ItemStack taken = current.split(amount);
        // split 之后原堆叠数量变 0 但仍带着物品 id，这里统一归一化成 ItemStack.EMPTY
        if (current.isEmpty())
        {
            setItem(slot, ItemStack.EMPTY);
        }
        else
        {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        if (!inRange(slot))
        {
            return ItemStack.EMPTY;
        }
        ItemStack current = contents().get(slot);
        if (current.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        cached = contents().with(slot, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack)
    {
        if (!inRange(slot))
        {
            return;
        }
        cached = contents().with(slot, stack);
        setChanged();
    }

    /** 把当前内容整体写回物品组件（组件的持久化 / 网络同步见 GodBackpackItem） */
    @Override
    public void setChanged()
    {
        ItemStack backpackStack = stack();
        if (!backpackStack.isEmpty() && cached != null)
        {
            GodBackpackItem.setContents(backpackStack, cached);
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        // 真正的「界面还能不能用」由菜单判断（背包不在物品栏里就关界面）
        return true;
    }

    @Override
    public void clearContent()
    {
        cached = GodBackpackContents.empty();
        setChanged();
    }
}
