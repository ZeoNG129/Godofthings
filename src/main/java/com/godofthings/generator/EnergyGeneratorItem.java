package com.godofthings.generator;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 能量发电机物品，tooltip 展示当前电量、发电量、成长进度与无线充电状态。
 */
public class EnergyGeneratorItem extends BlockItem
{
    public EnergyGeneratorItem(Block block)
    {
        super(block, new Properties().stacksTo(1).fireResistant());
    }

    /** 物品名称使用能源主题色（红色） */
    @Override
    public Component getName(ItemStack stack)
    {
        return super.getName(stack).copy().withStyle(ChatFormatting.RED);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn)
    {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        double output = EnergyGenConfig.MIN;
        long energy = 0;
        long tickCount = 0;
        long nextIncrease = EnergyGenConfig.STEP;
        boolean wirelessOn = false;
        long second = EnergyGenConfig.SECOND;
        long step = EnergyGenConfig.STEP;
        if (stack.hasTag())
        {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag != null)
            {
                if (tag.contains("output", Tag.TAG_LONG))
                {
                    output = tag.getLong("output");
                }
                if (tag.contains("energy", Tag.TAG_LONG))
                {
                    energy = tag.getLong("energy");
                }
                if (tag.contains("tickCount", Tag.TAG_LONG))
                {
                    tickCount = tag.getLong("tickCount");
                }
                if (tag.contains("nextIncrease", Tag.TAG_LONG))
                {
                    nextIncrease = tag.getLong("nextIncrease");
                }
                else if (tag.contains("beaconIncrease", Tag.TAG_LONG))
                {
                    nextIncrease = tag.getLong("beaconIncrease");
                }
                if (tag.contains("wirelessOn", Tag.TAG_BYTE))
                {
                    wirelessOn = tag.getBoolean("wirelessOn");
                }
            }
        }
        double percent = (int) (tickCount / 20.00D / second * 10000) / 100.00D;
        tooltip.add(Component.translatable("item.godofthings.energy_generator.tooltip.energy", energy).withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.godofthings.energy_generator.tooltip.output", output).withStyle(ChatFormatting.RED));
        if (output >= EnergyGenConfig.MAX)
        {
            tooltip.add(Component.translatable("item.godofthings.energy_generator.tooltip.next_max").withStyle(ChatFormatting.GOLD));
        }
        else
        {
            tooltip.add(Component.translatable("item.godofthings.energy_generator.tooltip.next", nextIncrease).withStyle(ChatFormatting.RED));
        }
        if (output < EnergyGenConfig.MAX)
        {
            tooltip.add(Component.translatable("item.godofthings.energy_generator.tooltip.growth", percent).withStyle(ChatFormatting.GREEN));
        }
        else
        {
            tooltip.add(Component.translatable("item.godofthings.energy_generator.tooltip.growth_max").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.godofthings.energy_generator.tooltip.step", second, step).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(wirelessOn ? "item.godofthings.energy_generator.tooltip.wireless_on" : "item.godofthings.energy_generator.tooltip.wireless_off")
                .withStyle(wirelessOn ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("item.godofthings.energy_generator.tooltip.group_faster").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.godofthings.energy_generator.tooltip.tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
