package com.godofthings.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 神之手册：右键打开游戏内手册（与「P 键」「/godofthings manual」等价）。
 *
 * <p>手册内容全部在<b>客户端</b>（条目从注册表生成、正文在语言文件里），所以这里客户端直接开界面，
 * 不走服务端往返；服务端分支什么都不做（{@link #use} 里客户端那一支才引用客户端类，
 * 服务端永远不会解析到它）。</p>
 */
public class GodManualItem extends Item
{
    public GodManualItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
        {
            com.godofthings.client.ManualOpener.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag)
    {
        tooltipComponents.add(Component.translatable("tooltip.godofthings.god_manual.1")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.godofthings.god_manual.2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
