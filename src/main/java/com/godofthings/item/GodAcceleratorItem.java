package com.godofthings.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 神之加速：放入神之系列机器（神之资源 / 神之掉落 / 神之怪蛋 / 神之熔炉）的加速槽，
 * 提升机器的并行数量。每个神之加速提供 {@link #PARALLEL_PER_ITEM} 倍并行，
 * 最多放一组（64 个）= {@link #PARALLEL_PER_STACK} 倍。
 *
 * <p> previously 该倍率字面量 {@code 16} 被复制粘贴在 4 个方块实体的
 * {@code getParallelMultiplier()} 里（GodFurnace / GodResource / GodDrop / GodSpawnEgg），
 * 改一次倍率要动 4 个文件 —— 现在收敛到本常量，各机器统一引用 {@link #multiplierFor(int)}。</p>
 */
public class GodAcceleratorItem extends Item
{
    /** 每个神之加速提供的并行倍率（唯一权威值，机器侧一律引用本常量） */
    public static final int PARALLEL_PER_ITEM = 16;

    /** 一组（64 个）神之加速的并行倍率 */
    public static final int PARALLEL_PER_STACK = PARALLEL_PER_ITEM * 64;

    /** 由加速槽里的数量计算并行倍率（各机器 getParallelMultiplier 统一委托到这里）。 */
    public static int multiplierFor(int count)
    {
        return count <= 0 ? 1 : count * PARALLEL_PER_ITEM;
    }

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
