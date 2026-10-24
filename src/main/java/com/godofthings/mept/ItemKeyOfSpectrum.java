package com.godofthings.mept;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import net.minecraft.world.item.Item;

/**
 * 光谱的钥匙（照抄上游 ItemKeyOfSpectrum）：让线缆工具可染任意颜色的升级卡。
 * <p>
 * 上游继承 AE2 的 UpgradeCardItem；因为只有 CableToolMenu 的专属槽位会校验物品
 * （KeyOfSpectrumSlot#mayPlace），这里改为普通 Item 并同样给出「可用于」提示。
 */
public class ItemKeyOfSpectrum extends Item
{
    public ItemKeyOfSpectrum(Properties props)
    {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag)
    {
        tooltipComponents.add(Component.translatable("tooltip.godofthings.mept.key_of_spectrum"));

        tooltipComponents.add(Component.translatable("tooltip.godofthings.mept.supported_by")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.literal("  ").append(
                Component.translatable("item.godofthings.mept.me_cable_placement_tool"))
                .withStyle(ChatFormatting.GRAY));
    }
}
