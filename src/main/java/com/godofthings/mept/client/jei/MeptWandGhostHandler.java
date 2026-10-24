package com.godofthings.mept.client.jei;

import com.godofthings.mept.GhostSlot;
import com.godofthings.mept.WandMenu;
import com.godofthings.mept.client.MeptWandScreen;
import com.godofthings.mept.network.UpdateWandSlotPayload;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import appeng.api.stacks.GenericStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 从 JEI 往法杖幽灵槽拖物品的处理器（照抄上游 WandGhostHandler）。
 * 物品和流体都支持（流体经 GenericStack 包装）。
 */
public class MeptWandGhostHandler implements IGhostIngredientHandler<MeptWandScreen>
{
    @Override
    public <I> List<Target<I>> getTargetsTyped(MeptWandScreen gui, ITypedIngredient<I> ingredient, boolean doStart)
    {
        List<Target<I>> targets = new ArrayList<>();

        var menu = gui.getMenu();

        // 先直接取 ItemStack，取不到再试流体转 GenericStack
        ItemStack stack = ingredient.getItemStack().orElse(ItemStack.EMPTY);
        if (stack.isEmpty())
        {
            stack = convertToItemStack(ingredient.getType(), ingredient.getIngredient());
        }
        if (stack == null || stack.isEmpty())
        {
            return targets;
        }

        // 给可见幽灵槽建目标
        var ghostSlots = menu.getGhostSlots();
        for (int i = 0; i < ghostSlots.size(); i++)
        {
            GhostSlot slot = ghostSlots.get(i);
            // 跳过屏幕外的槽
            if (slot.x < 0 || slot.y < 0) continue;

            targets.add(new SlotTarget<>(gui, menu, i, stack.copyWithCount(1)));
        }

        return targets;
    }

    @Override
    public void onComplete()
    {
        // 无需善后
    }

    @Override
    public boolean shouldHighlightTargets()
    {
        return true;
    }

    /**
     * 某个幽灵槽的目标。
     */
    private static class SlotTarget<I> implements Target<I>
    {
        private final Rect2i area;
        private final WandMenu menu;
        private final int visualIndex;
        private final int actualIndex;
        private final ItemStack stackToPlace;

        public SlotTarget(MeptWandScreen gui, WandMenu menu, int visualIndex, ItemStack stackToPlace)
        {
            this.menu = menu;
            this.visualIndex = visualIndex;
            this.actualIndex = menu.getActualSlotIndex(visualIndex);
            this.stackToPlace = stackToPlace;

            GhostSlot slot = menu.getGhostSlots().get(visualIndex);
            this.area = new Rect2i(slot.x + gui.getGuiLeft(), slot.y + gui.getGuiTop(), 16, 16);
        }

        @Override
        public Rect2i getArea()
        {
            return area;
        }

        @Override
        public void accept(I ingredient)
        {
            try
            {
                // 更新本地 handler
                menu.getHandler().setStackInSlot(actualIndex, stackToPlace);

                // 发包让服务端持久化
                PacketDistributor.sendToServer(new UpdateWandSlotPayload(actualIndex, stackToPlace));
            }
            catch (Throwable ignored)
            {
                // 吞掉异常，别把 JEI 弄崩
            }
        }
    }

    /**
     * 把 JEI 材料转成 ItemStack；流体会包成 AE2 的 GenericStack 包装物品。
     */
    @SuppressWarnings("unchecked")
    private static <T> ItemStack convertToItemStack(IIngredientType<T> type, T ingredient)
    {
        if (type == NeoForgeTypes.FLUID_STACK)
        {
            FluidStack fluidStack = (FluidStack) ingredient;
            if (!fluidStack.isEmpty())
            {
                var genericStack = GenericStack.fromFluidStack(fluidStack);
                if (genericStack != null)
                {
                    return GenericStack.wrapInItemStack(genericStack);
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
