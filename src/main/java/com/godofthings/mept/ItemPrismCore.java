package com.godofthings.mept;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import net.minecraft.world.item.Item;

/**
 * 棱镜原体（照抄上游 ItemPrismCore）：带彩虹文字 tooltip 的合成材料。
 */
public class ItemPrismCore extends Item
{
    public ItemPrismCore(Properties props)
    {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag)
    {
        tooltipComponents.add(Component.translatable("tooltip.godofthings.mept.prism_core.prefix"));
        // 「光谱的钥匙」彩虹文字
        MutableComponent rainbow = Component.empty();
        String text = Component.translatable("tooltip.godofthings.mept.prism_core.rainbow").getString();
        ChatFormatting[] colors = {ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.GREEN,
                ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE};
        for (int i = 0; i < text.length(); i++)
        {
            rainbow.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(colors[i % colors.length]));
        }
        tooltipComponents.add(rainbow);
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
