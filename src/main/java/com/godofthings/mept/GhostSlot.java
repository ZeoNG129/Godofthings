package com.godofthings.mept;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 幽灵槽（照抄上游 GhostSlot）：不真正存放物品，纯用于配置展示。
 */
public class GhostSlot extends Slot
{
    private static final Container DUMMY_CONTAINER = new SimpleContainer(1);

    private final int visualIndex;
    private final Consumer<ItemStack> onChange;
    private Supplier<ItemStack> displayStackSupplier = () -> ItemStack.EMPTY;

    public GhostSlot(int visualIndex, int x, int y, Consumer<ItemStack> onChange)
    {
        super(DUMMY_CONTAINER, 0, x, y);
        this.visualIndex = visualIndex;
        this.onChange = onChange;
    }

    public GhostSlot(int visualIndex, int x, int y)
    {
        this(visualIndex, x, y, (s) ->
        {
        });
    }

    public int getVisualIndex()
    {
        return visualIndex;
    }

    public Consumer<ItemStack> getOnChange()
    {
        return onChange;
    }

    public void setDisplayStackSupplier(Supplier<ItemStack> supplier)
    {
        this.displayStackSupplier = supplier;
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        return true;
    }

    @Override
    public boolean mayPickup(Player player)
    {
        return true;
    }

    @Override
    public int getMaxStackSize()
    {
        return 1;
    }

    @Override
    public ItemStack getItem()
    {
        return displayStackSupplier.get();
    }

    @Override
    public void set(ItemStack stack)
    {
        // 只有设置非空物品时（JEI/REI 拖入）才触发回调；
        // 空物品通常来自同步操作而非用户意图，清空由 WandMenu.clicked() 处理
        if (!stack.isEmpty() && onChange != null)
        {
            onChange.accept(stack);
        }
    }

    @Override
    public ItemStack remove(int amount)
    {
        // 不在此触发回调——清空由 WandMenu.clicked() 处理；
        // 该方法常被容器同步调用，不该被当成用户操作
        return ItemStack.EMPTY;
    }

    @Override
    public boolean hasItem()
    {
        return !displayStackSupplier.get().isEmpty();
    }
}
