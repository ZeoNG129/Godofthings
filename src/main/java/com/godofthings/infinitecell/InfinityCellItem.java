package com.godofthings.infinitecell;

// 基于 ExtendedAE（作者 glodblock，LGPL-3.0）的 ItemInfinityCell 移植：
// https://github.com/GLondon/ExtendedAE
// ME 无限存储元件物品：固定绑定一种资源（AEKey），名字显示为「ME无限<资源>元件」，
// 悬浮提示 + 内容预览图。元件本身不存任何数据（见 InfinityCellInventory.persist 为空操作）。

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.items.AEBaseItem;
import appeng.items.storage.StorageCellTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class InfinityCellItem extends AEBaseItem
{
    /** 资源 supplier 延迟到首次使用（注册表冻结后）才解析，避免在注册阶段提前取 AE2 物品/流体 */
    private final Supplier<AEKey> type;
    private AEKey record;

    public InfinityCellItem(@NotNull Supplier<AEKey> type, Properties properties)
    {
        super(properties);
        this.type = type;
    }

    public AEKey getRecord()
    {
        if (this.record == null)
        {
            this.record = this.type.get();
        }
        return this.record;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack is)
    {
        return Component.translatable("item.godofthings.infinity_cell_name", this.getRecord().getDisplayName());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack is, Item.@NotNull TooltipContext ctx, @NotNull List<Component> lines, @NotNull TooltipFlag adv)
    {
        lines.add(Component.translatable("infinity.godofthings.tooltip").withStyle(ChatFormatting.GREEN));
    }

    @NotNull
    @Override
    public Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack)
    {
        var content = Collections.singletonList(new GenericStack(this.getRecord(), getAsIntMax(this.getRecord())));
        return Optional.of(new StorageCellTooltipComponent(List.of(), content, false, true));
    }

    /** 「无限」的数量口径（与上游一致）：21.4 亿 × 每单位数量（物品 1，流体按 mB 口径） */
    public static long getAsIntMax(AEKey key)
    {
        return (long) Integer.MAX_VALUE * key.getAmountPerUnit();
    }
}
