package com.godofthings.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 神之加速：放入神之系列机器（神之资源 / 神之掉落 / 神之熔炉）的加速槽，
 * 提升机器的并行数量。每个神之加速提供 16 倍并行，最多放一组（64 个）= 1024 倍。
 */
public class GodAcceleratorItem extends Item
{
    public GodAcceleratorItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull Item.TooltipContext context,
                                @NotNull List<Component> tooltipComponents,
                                @NotNull TooltipFlag tooltipFlag)
    {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.godofthings.god_accelerator"));
    }
}
